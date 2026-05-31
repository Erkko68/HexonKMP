package eric.bitria.hexonkmp.session

import eric.bitria.hexonkmp.core.game.action.GameAction
import eric.bitria.hexonkmp.core.game.engine.CatanGameEngine
import eric.bitria.hexonkmp.core.game.engine.GameEngine
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Owns connections AND the authoritative game state, but contains NO game rules:
// it delegates every decision to the pure GameEngine and only handles transport
// (who's connected, what to broadcast). All game logic lives in the engine.
class GameSession(
    val gameId: String,
    private val maxPlayers: Int = 2,
    private val engine: GameEngine = CatanGameEngine(),
    // Called after the last player leaves so the repository can clean up.
    private val onEmpty: suspend (gameId: String) -> Unit = {},
) {
    private val mutex = Mutex()
    // LinkedHashSet: preserves join order, which becomes the turn order.
    private val reservations = LinkedHashSet<String>()
    private val connections = mutableMapOf<String, DefaultWebSocketSession>()
    // Non-null once the room first fills; the game runs for the rest of the
    // session even if players later drop below maxPlayers.
    private var state: GameState? = null

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
            val justStarted = state == null && connections.size == maxPlayers
            if (justStarted) {
                state = engine.initialState(reservations.map { PlayerId(it) })
            }
            ConnectPlan(self = ws, others = others, state = state, justStarted = justStarted)
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
        val (targets, isEmpty) = mutex.withLock {
            connections.remove(playerId)
            reservations.remove(playerId)
            connections.values.toList() to (connections.isEmpty() && reservations.isEmpty())
        }
        // I/O outside the lock — avoids holding the mutex during sends.
        if (targets.isNotEmpty()) broadcast(PlayerLeft(playerId), targets)
        if (isEmpty) onEmpty(gameId)
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

    private sealed interface ActionPlan {
        data class Rejected(val actor: DefaultWebSocketSession?, val reason: String) : ActionPlan
        data class Applied(
            val targets: List<DefaultWebSocketSession>,
            val events: List<eric.bitria.hexonkmp.core.game.event.GameEvent>,
        ) : ActionPlan
    }

    private suspend fun send(target: DefaultWebSocketSession, event: ServerEvent) =
        target.send(Frame.Text(Wire.encode(event)))

    private suspend fun broadcast(event: ServerEvent, targets: List<DefaultWebSocketSession>) {
        val text = Wire.encode(event)
        targets.forEach { it.send(Frame.Text(text)) }
    }
}
