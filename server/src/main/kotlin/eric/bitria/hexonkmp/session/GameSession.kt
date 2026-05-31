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

class GameSession(val gameId: String, private val maxPlayers: Int = 2) {
    private val mutex = Mutex()
    private val reservations = mutableSetOf<String>()
    private val connections = mutableMapOf<String, DefaultWebSocketSession>()

    fun hasAvailableSlot(): Boolean = reservations.size < maxPlayers

    fun reserveSlot(playerId: String) {
        reservations.add(playerId)
    }

    suspend fun connect(playerId: String, ws: DefaultWebSocketSession): Boolean = mutex.withLock {
        if (playerId !in reservations) return@withLock false
        connections[playerId] = ws
        if (connections.size == maxPlayers) {
            broadcast(GameStarted)
        } else {
            broadcast(WaitingForPlayers(connected = connections.size, needed = maxPlayers))
        }
        true
    }

    suspend fun disconnect(playerId: String) = mutex.withLock {
        connections.remove(playerId)
        if (connections.isNotEmpty()) broadcast(PlayerDisconnected)
    }

    private suspend fun broadcast(event: ServerEvent) {
        val text = AppJson.encodeToString(event)
        connections.values.forEach { it.send(Frame.Text(text)) }
    }
}
