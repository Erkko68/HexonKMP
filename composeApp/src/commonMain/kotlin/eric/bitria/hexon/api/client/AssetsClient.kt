package eric.bitria.hexon.api.client

import eric.bitria.hexon.dtos.assets.IconResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

interface AssetsClient{
    suspend fun getIcon(iconId: String) : IconResponse
}

class KtorAssetsClient(
    private val client: HttpClient
) : AssetsClient {
    override suspend fun getIcon(iconId: String): IconResponse {
        return client.get("/assets/icons/$iconId.json").body()
    }
}