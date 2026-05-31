package eric.bitria.hexonkmp.session

import eric.bitria.hexonkmp.core.AppJson
import eric.bitria.hexonkmp.core.ws.GameStarted
import eric.bitria.hexonkmp.core.ws.PlayerJoined
import eric.bitria.hexonkmp.core.ws.PlayerLeft
import eric.bitria.hexonkmp.core.ws.ServerEvent
import eric.bitria.hexonkmp.core.ws.WaitingForPlayers
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GameSession(
    val gameId: String,
    private val maxPlayers: Int = 2,
    // Called after the last player leaves so the repository can clean up.
    private val onEmpty: suspend (gameId: String) -> Unit = {},
) {
    private val mutex = Mutex()
    private val reservations = mutableSetOf<String>()
    private val connections = mutableMapOf<String, DefaultWebSocketSession>()
    // Once the room first fills we're in the in-game phase for the rest of the
    // session's life, even if players later drop below maxPlayers.
    private var started = false

    fun hasAvailableSlot(): Boolean = reservations.size < maxPlayers

    fun reserveSlot(playerId: String) {
        reservations.add(playerId)
    }

    suspend fun connect(playerId: String, ws: DefaultWebSocketSession): Boolean {
        // Compute everything under the lock; perform the sends outside it.
        val plan = mutex.withLock {
            if (playerId !in reservations) return false
            val others = connections.values.toList()
            connections[playerId] = ws
            val justStarted = !started && connections.size == maxPlayers
            if (justStarted) started = true
            ConnectPlan(self = ws, others = others, started = started, justStarted = justStarted)
        }

        if (!plan.started) {
            // Lobby phase: everyone sees the updated count.
            broadcast(WaitingForPlayers(plan.others.size + 1, maxPlayers), plan.others + plan.self)
        } else {
            // In-game phase. The connecting player enters the game; the rest, if
            // any, just see a join notice. (justStarted: it was the lobby that
            // filled; otherwise it's a reconnect into a running game.)
            send(plan.self, GameStarted)
            if (plan.others.isNotEmpty()) {
                val event = if (plan.justStarted) GameStarted else PlayerJoined(playerId)
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

    private class ConnectPlan(
        val self: DefaultWebSocketSession,
        val others: List<DefaultWebSocketSession>,
        val started: Boolean,
        val justStarted: Boolean,
    )

    private suspend fun send(target: DefaultWebSocketSession, event: ServerEvent) =
        target.send(Frame.Text(AppJson.encodeToString(event)))

    private suspend fun broadcast(event: ServerEvent, targets: List<DefaultWebSocketSession>) {
        val text = AppJson.encodeToString(event)
        targets.forEach { it.send(Frame.Text(text)) }
    }
}
