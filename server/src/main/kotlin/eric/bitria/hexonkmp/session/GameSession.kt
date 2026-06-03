package eric.bitria.hexonkmp.session

import eric.bitria.hexonkmp.core.game.action.GameAction
import eric.bitria.hexonkmp.core.game.config.ClassicCatan
import eric.bitria.hexonkmp.core.game.config.ScenarioConfig
import eric.bitria.hexonkmp.core.game.engine.CatanGameEngine
import eric.bitria.hexonkmp.core.game.engine.GameEngine
import eric.bitria.hexonkmp.core.game.event.GameEvent
import eric.bitria.hexonkmp.core.game.model.GameState
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.protocol.ActionRejected
import eric.bitria.hexonkmp.core.protocol.GameStarted
import eric.bitria.hexonkmp.core.protocol.GameUpdate
import eric.bitria.hexonkmp.core.protocol.PlayerJoined
import eric.bitria.hexonkmp.core.protocol.PlayerLeft
import eric.bitria.hexonkmp.core.protocol.ServerEvent
import eric.bitria.hexonkmp.core.protocol.WaitingForPlayers
import eric.bitria.hexonkmp.core.protocol.Wire
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
// it delegates every decision to the pure GameEngine and only handles transport
// (who's connected, what to broadcast). All game logic lives in the engine.
//
// Matchmaking is driven entirely by the scenario's RuleConfig: the room holds up
// to `maxPlayers`, and once `minPlayers` are connected it waits up to
// `autoStartDelaySeconds` for more before starting automatically (or starts
// instantly the moment the room fills).
class GameSession(
    val gameId: String,
    private val config: ScenarioConfig = ClassicCatan,
    private val engine: GameEngine = CatanGameEngine(config),
    // Scope for the lobby auto-start countdown; the timer outlives any single
    // connect() call, so it can't live on the request coroutine.
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    // Called after the last player leaves so the repository can clean up.
    private val onEmpty: suspend (gameId: String) -> Unit = {},
) {
    private val minPlayers = config.rules.minPlayers
    private val maxPlayers = config.rules.maxPlayers
    private val autoStartDelayMs = config.rules.autoStartDelaySeconds.toLong() * 1000L

    private val mutex = Mutex()
    // LinkedHashSet: preserves join order, which becomes the turn order.
    private val reservations = LinkedHashSet<String>()
    private val connections = mutableMapOf<String, DefaultWebSocketSession>()
    // Non-null once the game starts; it then runs for the rest of the session even
    // if players later drop below minPlayers.
    private var state: GameState? = null
    // The pending auto-start countdown, if the lobby has reached minPlayers but
    // isn't full yet. Cancelled if the room fills or drops below minPlayers.
    private var autoStartJob: Job? = null

    fun hasAvailableSlot(): Boolean = reservations.size < maxPlayers

    fun reserveSlot(playerId: String) {
        reservations.add(playerId)
    }

    suspend fun connect(playerId: String, ws: DefaultWebSocketSession): Boolean {
        // Decide everything under the lock; perform the sends outside it.
        val plan = mutex.withLock {
            if (playerId !in reservations) return false
            val others = connections.values.toList()
            connections[playerId] = ws
            val current = state
            if (current != null) {
                // Reconnect into a running game: mark the player present again.
                state = engine.playerJoined(current, PlayerId(playerId)).state
                ConnectPlan(self = ws, others = others, state = state, justStarted = false)
            } else if (connections.size >= maxPlayers) {
                // Room is full — start immediately and drop any pending countdown.
                cancelAutoStart()
                state = startState()
                ConnectPlan(self = ws, others = others, state = state, justStarted = true)
            } else {
                // Still in the lobby. Arm the auto-start countdown once we have the
                // minimum (no-op if it's already running).
                if (connections.size >= minPlayers) armAutoStart()
                ConnectPlan(self = ws, others = others, state = null, justStarted = false)
            }
        }

        val gameState = plan.state
        if (gameState == null) {
            // Lobby phase: everyone sees the updated count.
            broadcast(WaitingForPlayers(plan.others.size + 1, maxPlayers), plan.others + plan.self)
        } else {
            // In-game. The connecting player enters the game (gets a snapshot);
            // existing players see either the start (room just filled) or a join.
            send(plan.self, GameStarted(gameState))
            if (plan.others.isNotEmpty()) {
                val event = if (plan.justStarted) GameStarted(gameState) else PlayerJoined(playerId)
                broadcast(event, plan.others)
            }
        }
        return true
    }

    suspend fun disconnect(playerId: String) {
        val plan = mutex.withLock {
            connections.remove(playerId)
            reservations.remove(playerId)
            val isEmpty = connections.isEmpty() && reservations.isEmpty()
            // Tell the engine the player left so the turn can move on if it was
            // theirs; capture any resulting game events to broadcast.
            val current = state
            val gameEvents = if (current != null) {
                val result = engine.playerLeft(current, PlayerId(playerId))
                state = result.state
                result.events
            } else {
                // Still in the lobby: if we dropped below the minimum, stop the
                // pending auto-start so we don't launch an undersized game.
                if (connections.size < minPlayers) cancelAutoStart()
                emptyList()
            }
            DisconnectPlan(connections.values.toList(), gameEvents, isEmpty)
        }
        // I/O outside the lock — avoids holding the mutex during sends.
        if (plan.targets.isNotEmpty()) {
            broadcast(PlayerLeft(playerId), plan.targets)
            plan.events.forEach { broadcast(GameUpdate(it), plan.targets) }
        }
        if (plan.isEmpty) {
            cancelAutoStart()
            scope.cancel()
            onEmpty(gameId)
        }
    }

    // The game's initial state, seeded with the currently-connected players in
    // join order (reservations that never connected are left out).
    private fun startState(): GameState =
        engine.initialState(reservations.filter { it in connections }.map { PlayerId(it) })

    // Arms the lobby countdown if one isn't already running. Called under the lock;
    // launch() doesn't suspend, so it's safe to hold the mutex.
    private fun armAutoStart() {
        if (autoStartJob != null) return
        autoStartJob = scope.launch {
            delay(autoStartDelayMs.milliseconds)
            autoStart()
        }
    }

    private fun cancelAutoStart() {
        autoStartJob?.cancel()
        autoStartJob = null
    }

    // Fires when the countdown elapses: starts the game with whoever is connected,
    // provided the minimum still holds and the room didn't already start/fill.
    private suspend fun autoStart() {
        val started = mutex.withLock {
            autoStartJob = null
            if (state != null || connections.size < minPlayers) return
            val fresh = startState()
            state = fresh
            fresh to connections.values.toList()
        }
        val (gameState, targets) = started
        broadcast(GameStarted(gameState), targets)
    }

    // Runs one player action through the engine and broadcasts the outcome.
    suspend fun handleAction(playerId: String, action: GameAction) {
        val plan = mutex.withLock {
            val current = state ?: return  // action before the game started: ignore
            val result = engine.reduce(current, PlayerId(playerId), action)
            val rejection = result.rejection
            if (rejection != null) {
                ActionPlan.Rejected(connections[playerId], rejection)
            } else {
                state = result.state
                ActionPlan.Applied(connections.values.toList(), result.events)
            }
        }
        when (plan) {
            is ActionPlan.Rejected ->
                plan.actor?.let { send(it, ActionRejected(plan.reason)) }
            is ActionPlan.Applied ->
                plan.events.forEach { broadcast(GameUpdate(it), plan.targets) }
        }
    }

    private class ConnectPlan(
        val self: DefaultWebSocketSession,
        val others: List<DefaultWebSocketSession>,
        val state: GameState?,
        val justStarted: Boolean,
    )

    private class DisconnectPlan(
        val targets: List<DefaultWebSocketSession>,
        val events: List<GameEvent>,
        val isEmpty: Boolean,
    )

    private sealed interface ActionPlan {
        data class Rejected(val actor: DefaultWebSocketSession?, val reason: String) : ActionPlan
        data class Applied(
            val targets: List<DefaultWebSocketSession>,
            val events: List<GameEvent>,
        ) : ActionPlan
    }

    private suspend fun send(target: DefaultWebSocketSession, event: ServerEvent) =
        target.send(Frame.Text(Wire.encode(event)))

    private suspend fun broadcast(event: ServerEvent, targets: List<DefaultWebSocketSession>) {
        val text = Wire.encode(event)
        targets.forEach { it.send(Frame.Text(text)) }
    }
}
