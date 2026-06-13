package eric.bitria.hexonkmp.session

import eric.bitria.hexonkmp.core.game.engine.GameEngine
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.Redactable
import eric.bitria.hexonkmp.core.protocol.ActionRejected
import eric.bitria.hexonkmp.core.protocol.GameCodec
import eric.bitria.hexonkmp.core.protocol.GameStarted
import eric.bitria.hexonkmp.core.protocol.GameUpdate
import eric.bitria.hexonkmp.core.protocol.LobbyMember
import eric.bitria.hexonkmp.core.protocol.LobbyRoster
import eric.bitria.hexonkmp.core.protocol.PartyRules
import eric.bitria.hexonkmp.core.protocol.PlayerJoined
import eric.bitria.hexonkmp.core.protocol.PlayerLeft
import eric.bitria.hexonkmp.core.protocol.ServerEvent
import eric.bitria.hexonkmp.core.protocol.TurnTimer
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

// Owns connections AND the authoritative game state, but contains NO game rules:
// it delegates every decision to a pure GameEngine and only handles transport
// (who's connected, what to broadcast). Generic over a game's state [S], action
// [A], and event [E] — it hosts ANY turn-based game, not just Catan. The state
// and events must be Redactable so the session can ship per-recipient projections
// that hide other players' secrets. (De)serialization is delegated to a GameCodec.
//
// Matchmaking is configured per game (the ints below): the room holds up to
// [maxPlayers], and once [minPlayers] are connected it waits up to
// [autoStartDelaySeconds] for more before starting automatically (or starts
// instantly the moment the room fills).
class GameSession<S : Redactable<S>, A, E : Redactable<E>>(
    val gameId: String,
    // Builds the engine for this game once it starts, given the host's victory-point
    // override (null = the game mode's default). Deferred to start time so a private
    // host can change the win target in the lobby before the engine is created. The
    // transport stays game-agnostic — only the factory (in AppModule) names Catan.
    private val engineFor: (victoryPoints: Int?) -> GameEngine<S, A, E>,
    private val codec: GameCodec<S, A, E>,
    private val minPlayers: Int,
    private val maxPlayers: Int,
    autoStartDelaySeconds: Int,
    // The game mode's default rules a private host starts from (and that auto lobbies
    // always use): the win target and the per-turn timer (null = no timer).
    private val defaultVictoryPoints: Int,
    private val defaultTurnTimerSeconds: Int?,
    // Lobby policy. false (default) = auto/matchmaking: arm a countdown at the
    // minimum and start automatically. true = manual/private: a host presses Start;
    // no countdown, and a full room does NOT auto-start.
    private val manualStart: Boolean = false,
    // Scope for the lobby auto-start countdown; the timer outlives any single
    // connect() call, so it can't live on the request coroutine.
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    // Called after the last player leaves so the repository can clean up.
    private val onEmpty: suspend (gameId: String) -> Unit = {},
) {
    private val autoStartDelayMs = autoStartDelaySeconds.toLong() * 1000L

    // The engine, created at start time via [engineFor] (so victory-point overrides
    // apply). Null until the game starts; only ever read once [state] is non-null.
    private var engine: GameEngine<S, A, E>? = null

    // The rules the game starts with: the mode defaults for auto/matchmaking, or the
    // host's chosen rules supplied at start (startByHost). Drives the start-time
    // engine (victory points) and the turn timer.
    private var activeRules: PartyRules = PartyRules(defaultVictoryPoints, defaultTurnTimerSeconds)

    private val mutex = Mutex()
    // LinkedHashSet: preserves join order, which becomes the turn order.
    private val reservations = LinkedHashSet<String>()
    private val connections = mutableMapOf<String, DefaultWebSocketSession>()
    // Display names by playerId, learned on connect; populates the lobby roster.
    private val names = mutableMapOf<String, String>()
    // The host of a manual lobby (the creator, or whoever's promoted if they leave).
    // Null for auto/matchmaking lobbies.
    private var hostId: String? = null
    // Non-null once the game starts; it then runs for the rest of the session even
    // if players later drop below minPlayers.
    private var state: S? = null
    // The pending auto-start countdown, if the lobby has reached minPlayers but
    // isn't full yet. Cancelled if the room fills or drops below minPlayers.
    private var autoStartJob: Job? = null
    // Wall-clock instant the countdown will fire, so we can report the remaining
    // seconds to clients on lobby changes (they tick it down locally). Null when no
    // countdown is running.
    private var autoStartDeadlineMs: Long? = null

    // The running game clock (in-game). [turnTimerKey] is the engine's identifier for
    // the current timed situation (a turn, a discard round, …), so the clock only
    // re-arms when the situation actually changes — not on every intermediate
    // build/trade. Cancelled at game over / for manual (no-timer) games.
    private var turnTimerJob: Job? = null
    private var turnTimerKey: Any? = null
    private var turnDeadlineMs: Long? = null

    fun hasAvailableSlot(): Boolean = reservations.size < maxPlayers

    fun reserveSlot(playerId: String) {
        reservations.add(playerId)
        // The first reserver of a manual lobby is its host.
        if (manualStart && hostId == null) hostId = playerId
    }

    suspend fun connect(playerId: String, name: String, ws: DefaultWebSocketSession): Boolean {
        // Decide everything under the lock; perform the sends outside it.
        val plan = mutex.withLock {
            if (playerId !in reservations) return false
            connections[playerId] = ws
            names[playerId] = name
            val current = state
            val outcome = when {
                current != null -> {
                    // Reconnect into a running game: mark the player present again.
                    state = engine!!.playerJoined(current, PlayerId(playerId)).state
                    Outcome.RECONNECTED
                }
                // A full AUTO room starts immediately; a manual room waits for its host.
                !manualStart && connections.size >= maxPlayers -> {
                    cancelAutoStart()
                    state = startState()
                    armTurnTimerLocked()
                    Outcome.STARTED
                }
                else -> {
                    // Still in the lobby. Auto lobbies arm the countdown once they reach
                    // the minimum (no-op if already running); manual lobbies never do.
                    if (!manualStart && connections.size >= minPlayers) armAutoStart()
                    Outcome.LOBBY
                }
            }
            // Snapshot the connections so per-recipient redaction happens outside the
            // lock; GameStarted must be redacted per player (hidden information).
            ConnectPlan(
                selfId = playerId,
                recipients = connections.toMap(),
                state = state,
                outcome = outcome,
                roster = if (outcome == Outcome.LOBBY) roster() else null,
                // The current turn clock, so a just-started or reconnecting client can
                // render the countdown immediately.
                turnTimer = if (outcome == Outcome.LOBBY) null else TurnTimer(turnRemainingSeconds()),
            )
        }

        val gameState = plan.state
        when (plan.outcome) {
            // Lobby phase: everyone sees the updated roster (names + host + any running
            // countdown — they tick it down locally).
            Outcome.LOBBY -> broadcast(plan.roster!!, plan.recipients.values.toList())
            // Room just filled: each connected player gets their own redacted snapshot,
            // then the shared turn clock.
            Outcome.STARTED -> {
                sendStartedTo(plan.recipients, gameState!!)
                plan.turnTimer?.let { broadcast(it, plan.recipients.values.toList()) }
            }
            // Reconnect: only the returning player gets a snapshot (+ the turn clock);
            // others see a join.
            Outcome.RECONNECTED -> {
                plan.recipients[plan.selfId]?.let {
                    send(it, GameStarted(gameState!!.redactedFor(PlayerId(plan.selfId)), names.toMap()))
                    plan.turnTimer?.let { t -> send(it, t) }
                }
                plan.recipients.filterKeys { it != plan.selfId }.values
                    .forEach { send(it, PlayerJoined(plan.selfId)) }
            }
        }
        return true
    }

    // Sends each recipient a GameStarted snapshot redacted from their viewpoint, so
    // nobody sees another player's hidden information. The public roster (names) rides
    // along as transport metadata so opponents can be shown by name in-game.
    private suspend fun sendStartedTo(recipients: Map<String, DefaultWebSocketSession>, gameState: S) {
        val playerNames = names.toMap()
        recipients.forEach { (pid, ws) ->
            send(ws, GameStarted(gameState.redactedFor(PlayerId(pid)), playerNames))
        }
    }

    suspend fun disconnect(playerId: String) {
        val plan = mutex.withLock {
            connections.remove(playerId)
            reservations.remove(playerId)
            // Keep names: a player may reconnect, and the in-game roster should still
            // show by name anyone who's part of the game even if briefly disconnected.
            // (The lobby roster lists current connections, so departed lobby members
            // still drop out of it.)
            // Tell the engine the player left so the turn can move on if it was
            // theirs; capture any resulting game events to broadcast.
            val current = state
            // A started game with no live connections is abandoned — tear it down even
            // if stale reservations linger (e.g. a player who reserved a seat but never
            // opened its socket). In the lobby we also wait for reservations to clear so
            // a freshly reserved seat isn't torn down before its socket connects.
            val isEmpty = connections.isEmpty() && (current != null || reservations.isEmpty())
            val gameEvents = if (current != null) {
                val result = engine!!.playerLeft(current, PlayerId(playerId))
                state = result.state
                result.events
            } else {
                // Still in the lobby. Auto lobbies stop the countdown if they dropped
                // below the minimum; a manual lobby promotes a new host if its host left.
                if (!manualStart && connections.size < minPlayers) cancelAutoStart()
                if (manualStart && playerId == hostId) hostId = connections.keys.firstOrNull()
                emptyList()
            }
            // While still in the lobby, refresh the remaining players' roster (host
            // and countdown may have just changed because of this departure).
            val lobbyUpdate = if (current == null && connections.isNotEmpty()) roster() else null
            // A departure mid-game may hand the turn to the next player (or end the
            // game) — re-arm the clock to match.
            val turnTimer = if (current != null) armTurnTimerLocked() else null
            DisconnectPlan(connections.toMap(), gameEvents, isEmpty, lobbyUpdate, turnTimer)
        }
        // I/O outside the lock — avoids holding the mutex during sends.
        if (plan.recipients.isNotEmpty()) {
            broadcast(PlayerLeft(playerId), plan.recipients.values.toList())
            plan.lobbyUpdate?.let { broadcast(it, plan.recipients.values.toList()) }
            plan.events.forEach { broadcastRedacted(it, plan.recipients) }
            plan.turnTimer?.let { broadcast(it, plan.recipients.values.toList()) }
        }
        if (plan.isEmpty) {
            cancelAutoStart()
            scope.cancel()
            onEmpty(gameId)
        }
    }

    // Host-only start of a manual lobby. Validated under the lock: must be a manual
    // lobby that hasn't started, the caller must be the host, and the minimum must be
    // met. Returns false (and does nothing) otherwise so the route can reject it.
    // Host-only start of a manual lobby, carrying the host's chosen [rules] (null =
    // mode defaults). Validated under the lock: must be a manual lobby that hasn't
    // started, by its host, with the minimum met. Returns false (and does nothing)
    // otherwise so the route can reject it.
    suspend fun startByHost(playerId: String, rules: PartyRules?): Boolean {
        val started = mutex.withLock {
            if (!manualStart || state != null) return false
            if (playerId != hostId || connections.size < minPlayers) return false
            if (rules != null) activeRules = rules
            val fresh = startState()
            state = fresh
            val timer = armTurnTimerLocked()
            Triple(fresh, connections.toMap(), timer)
        }
        val (gameState, recipients, timer) = started
        sendStartedTo(recipients, gameState)
        timer?.let { broadcast(it, recipients.values.toList()) }
        return true
    }

    // A snapshot of the current lobby for broadcast. Read under the lock.
    private fun roster(): LobbyRoster = LobbyRoster(
        members = connections.keys.map { LobbyMember(it, names[it] ?: it) },
        hostId = hostId,
        minPlayers = minPlayers,
        maxPlayers = maxPlayers,
        countdownSeconds = countdownSeconds(),
    )

    // The game's initial state, seeded with the currently-connected players in join
    // order (reservations that never connected are left out). Builds the engine here
    // so the host's victory-point override is baked in at start.
    private fun startState(): S {
        val eng = engineFor(activeRules.victoryPoints)
        engine = eng
        return eng.initialState(reservations.filter { it in connections }.map { PlayerId(it) })
    }

    // Arms the lobby countdown if one isn't already running. Called under the lock;
    // launch() doesn't suspend, so it's safe to hold the mutex.
    private fun armAutoStart() {
        if (autoStartJob != null) return
        autoStartDeadlineMs = System.currentTimeMillis() + autoStartDelayMs
        autoStartJob = scope.launch {
            delay(autoStartDelayMs.milliseconds)
            autoStart()
        }
    }

    private fun cancelAutoStart() {
        autoStartJob?.cancel()
        autoStartJob = null
        autoStartDeadlineMs = null
    }

    // Seconds left on the running auto-start countdown (rounded up), or null if no
    // countdown is active. Read under the lock.
    private fun countdownSeconds(): Int? =
        autoStartDeadlineMs?.let { (((it - System.currentTimeMillis()) + 999) / 1000).toInt().coerceAtLeast(0) }

    // Fires when the countdown elapses: starts the game with whoever is connected,
    // provided the minimum still holds and the room didn't already start/fill.
    private suspend fun autoStart() {
        val started = mutex.withLock {
            autoStartJob = null
            autoStartDeadlineMs = null
            if (state != null || connections.size < minPlayers) return
            val fresh = startState()
            state = fresh
            val timer = armTurnTimerLocked()
            Triple(fresh, connections.toMap(), timer)
        }
        val (gameState, recipients, timer) = started
        sendStartedTo(recipients, gameState)
        timer?.let { broadcast(it, recipients.values.toList()) }
    }

    // --- Game clock (in-game) ---

    // (Re)arms the clock to match the engine's current [timerKey] (a turn, a discard
    // round, …). Returns a TurnTimer event to broadcast when the running clock
    // changed (a new key, or it stopped), else null (same situation still running,
    // nothing to announce). Must be called under the lock; launch() doesn't suspend,
    // so holding it is safe.
    private fun armTurnTimerLocked(): TurnTimer? {
        val current = state ?: return null
        val key = engine!!.timerKey(current)
        val seconds = activeRules.turnTimerSeconds
        // No timer configured (manual mode) or nothing to time (game over): stop any
        // running clock and tell clients only if one was actually running.
        if (key == null || seconds == null) {
            val wasRunning = turnTimerKey != null
            cancelTurnTimer()
            return if (wasRunning) TurnTimer(null) else null
        }
        // The same situation is still going: leave the clock untouched (it runs for
        // the whole turn / whole discard round, not per intermediate action).
        if (key == turnTimerKey && turnTimerJob != null) return null
        // A new situation: reset the clock.
        turnTimerJob?.cancel()
        turnTimerKey = key
        turnDeadlineMs = System.currentTimeMillis() + seconds * 1000L
        turnTimerJob = scope.launch {
            delay((seconds * 1000L).milliseconds)
            fireTimeout(key)
        }
        return TurnTimer(seconds)
    }

    private fun cancelTurnTimer() {
        turnTimerJob?.cancel()
        turnTimerJob = null
        turnTimerKey = null
        turnDeadlineMs = null
    }

    // Seconds left on the running clock (rounded up), or null if none is active.
    private fun turnRemainingSeconds(): Int? =
        turnDeadlineMs?.let { (((it - System.currentTimeMillis()) + 999) / 1000).toInt().coerceAtLeast(0) }

    // Fires when the clock for [forKey] elapses: asks the engine what to auto-apply
    // (end the turn, resolve a robber move, discard for everyone who still owes, …)
    // and runs each action like a normal one, then re-arms for whatever's next. Stale
    // fires (the situation already moved on) are ignored.
    private suspend fun fireTimeout(forKey: Any) {
        val plan = mutex.withLock {
            var current = state ?: return
            if (turnTimerKey != forKey) return
            turnTimerJob = null
            turnDeadlineMs = null
            turnTimerKey = null
            val events = mutableListOf<E>()
            for ((actor, action) in engine!!.onTimeout(current)) {
                val result = engine!!.reduce(current, actor, action)
                if (result.rejection == null) {
                    current = result.state
                    events += result.events
                }
            }
            if (events.isEmpty()) return  // nothing forced; leave the clock stopped
            state = current
            TimeoutPlan(connections.toMap(), events, armTurnTimerLocked())
        }
        plan.events.forEach { broadcastRedacted(it, plan.recipients) }
        plan.turnTimer?.let { broadcast(it, plan.recipients.values.toList()) }
    }

    // Runs one player action through the engine and broadcasts the outcome. The
    // action arrives as raw text and is decoded by the game's codec here, so the
    // route layer stays fully game-agnostic. Malformed actions are ignored.
    suspend fun handleAction(playerId: String, actionText: String) {
        val action = runCatching { codec.decodeAction(actionText) }.getOrNull() ?: return
        val plan = mutex.withLock {
            val current = state ?: return  // action before the game started: ignore
            val result = engine!!.reduce(current, PlayerId(playerId), action)
            val rejection = result.rejection
            if (rejection != null) {
                ActionPlan.Rejected(connections[playerId], rejection)
            } else {
                state = result.state
                // An applied action may have ended the turn — re-arm the clock for
                // whoever's up next (no-op while the same player keeps acting).
                ActionPlan.Applied(connections.toMap(), result.events, armTurnTimerLocked())
            }
        }
        when (plan) {
            is ActionPlan.Rejected ->
                plan.actor?.let { send(it, ActionRejected(plan.reason)) }
            is ActionPlan.Applied -> {
                plan.events.forEach { broadcastRedacted(it, plan.recipients) }
                plan.turnTimer?.let { broadcast(it, plan.recipients.values.toList()) }
            }
        }
    }

    // Broadcasts a domain event per recipient, each through redactedFor so nobody
    // sees another player's hidden detail (e.g. a robber steal's resource).
    private suspend fun broadcastRedacted(event: E, recipients: Map<String, DefaultWebSocketSession>) {
        recipients.forEach { (pid, ws) -> send(ws, GameUpdate(event.redactedFor(PlayerId(pid)))) }
    }

    // What connecting resulted in, decided under the lock and acted on outside it.
    private enum class Outcome { LOBBY, STARTED, RECONNECTED }

    private inner class ConnectPlan(
        val selfId: String,
        // playerId -> socket snapshot, so GameStarted can be redacted per recipient.
        val recipients: Map<String, DefaultWebSocketSession>,
        val state: S?,
        val outcome: Outcome,
        // The roster to broadcast for a LOBBY outcome; null otherwise.
        val roster: LobbyRoster?,
        // The current turn clock to send a started/reconnecting client; null in lobby.
        val turnTimer: TurnTimer?,
    )

    private inner class DisconnectPlan(
        val recipients: Map<String, DefaultWebSocketSession>,
        val events: List<E>,
        val isEmpty: Boolean,
        // A refreshed roster to broadcast if we're still in the lobby after this
        // departure; null once the game has started.
        val lobbyUpdate: LobbyRoster?,
        // A re-armed turn clock to broadcast if the departure moved the turn; else null.
        val turnTimer: TurnTimer?,
    )

    // The outcome of a turn-timer firing: events to broadcast plus the re-armed clock.
    private inner class TimeoutPlan(
        val recipients: Map<String, DefaultWebSocketSession>,
        val events: List<E>,
        val turnTimer: TurnTimer?,
    )

    private sealed interface ActionPlan<out Ev> {
        data class Rejected(val actor: DefaultWebSocketSession?, val reason: String) : ActionPlan<Nothing>
        data class Applied<Ev>(
            val recipients: Map<String, DefaultWebSocketSession>,
            val events: List<Ev>,
            // A re-armed turn clock if this action changed whose turn it is; else null.
            val turnTimer: TurnTimer?,
        ) : ActionPlan<Ev>
    }

    private suspend fun send(target: DefaultWebSocketSession, event: ServerEvent<S, E>) =
        target.send(Frame.Text(codec.encodeServerEvent(event)))

    private suspend fun broadcast(event: ServerEvent<S, E>, targets: List<DefaultWebSocketSession>) {
        val text = codec.encodeServerEvent(event)
        targets.forEach { it.send(Frame.Text(text)) }
    }
}
