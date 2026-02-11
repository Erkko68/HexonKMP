package eric.bitria.hexon.data.remote

import eric.bitria.hexon.dtos.matchmaking.JoinGameRequest
import eric.bitria.hexon.dtos.matchmaking.JoinGameResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

interface MatchmakingClient {
    suspend fun joinGame(request: JoinGameRequest): JoinGameResponse
}

class KtorMatchmakingClient(
    private val client: HttpClient
) : MatchmakingClient {

    override suspend fun joinGame(request: JoinGameRequest): JoinGameResponse {
        return client.post("/game") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
