package eric.bitria.hexonkmp.session

import eric.bitria.hexonkmp.core.AppJson
import eric.bitria.hexonkmp.core.ws.PlayerConnected
import eric.bitria.hexonkmp.core.ws.PlayerDisconnected
import eric.bitria.hexonkmp.core.ws.ServerEvent
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

    fun hasAvailableSlot(): Boolean = reservations.size < maxPlayers

    fun reserveSlot(playerId: String) {
        reservations.add(playerId)
    }

    // Returns the event to broadcast plus a snapshot of recipients, both taken
    // inside the lock; the actual sends happen outside it.
    suspend fun connect(playerId: String, ws: DefaultWebSocketSession): Boolean {
        val (event, targets) = mutex.withLock {
            if (playerId !in reservations) return false
            connections[playerId] = ws
            PlayerConnected(playerId, connected = connections.size, needed = maxPlayers) to
                connections.values.toList()
        }
        broadcast(event, targets)
        return true
    }

    suspend fun disconnect(playerId: String) {
        val (targets, isEmpty) = mutex.withLock {
            connections.remove(playerId)
            reservations.remove(playerId)
            connections.values.toList() to (connections.isEmpty() && reservations.isEmpty())
        }
        // I/O outside the lock — avoids holding the mutex during sends.
        // targets.size is the post-removal connected count.
        if (targets.isNotEmpty()) {
            broadcast(PlayerDisconnected(playerId, connected = targets.size, needed = maxPlayers), targets)
        }
        if (isEmpty) onEmpty(gameId)
    }

    private suspend fun broadcast(event: ServerEvent, targets: List<DefaultWebSocketSession>) {
        val text = AppJson.encodeToString(event)
        targets.forEach { it.send(Frame.Text(text)) }
    }
}
