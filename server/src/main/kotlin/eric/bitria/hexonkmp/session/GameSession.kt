package eric.bitria.hexonkmp.session

import eric.bitria.hexonkmp.core.AppJson
import eric.bitria.hexonkmp.core.ws.GameStarted
import eric.bitria.hexonkmp.core.ws.PlayerDisconnected
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

    fun hasAvailableSlot(): Boolean = reservations.size < maxPlayers

    fun reserveSlot(playerId: String) {
        reservations.add(playerId)
    }

    // Returns the event to broadcast, computed inside the lock, sent outside.
    suspend fun connect(playerId: String, ws: DefaultWebSocketSession): Boolean {
        val event = mutex.withLock {
            if (playerId !in reservations) return false
            connections[playerId] = ws
            if (connections.size == maxPlayers) GameStarted
            else WaitingForPlayers(connected = connections.size, needed = maxPlayers)
        }
        broadcast(event)
        return true
    }

    suspend fun disconnect(playerId: String) {
        val (shouldBroadcast, isEmpty) = mutex.withLock {
            connections.remove(playerId)
            reservations.remove(playerId)
            Pair(connections.isNotEmpty(), connections.isEmpty() && reservations.isEmpty())
        }
        // I/O outside the lock — avoids holding the mutex during sends.
        if (shouldBroadcast) broadcast(PlayerDisconnected)
        if (isEmpty) onEmpty(gameId)
    }

    private suspend fun broadcast(event: ServerEvent) {
        val text = AppJson.encodeToString(event)
        connections.values.forEach { it.send(Frame.Text(text)) }
    }
}
