package eric.bitria.hexonkmp.client

import eric.bitria.hexonkmp.core.dto.JoinGameResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class GameClient(private val http: HttpClient, private val baseUrl: String) {
    suspend fun joinGame(): JoinGameResponse = http.post("$baseUrl/game").body()
}
