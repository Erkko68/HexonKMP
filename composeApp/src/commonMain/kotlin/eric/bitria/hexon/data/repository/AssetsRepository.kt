package eric.bitria.hexon.data.repository

import eric.bitria.hexon.data.remote.AssetsClient
import eric.bitria.hexon.dtos.assets.IconResponse

interface AssetsRepository {
    suspend fun getIcon(iconId: String) : ApiResult<IconResponse>
    fun getIconCached(iconId: String): IconResponse?
}

class AssetsRepositoryImpl(
    private val assetsClient: AssetsClient
) : AssetsRepository {

    private val iconCache = HashMap<String, IconResponse>()

    override suspend fun getIcon(iconId: String): ApiResult<IconResponse> {
        val cachedIcon = iconCache[iconId]
        if (cachedIcon != null) {
            return ApiResult.Success(cachedIcon)
        }

        val result = safeApiCall {
            assetsClient.getIcon(iconId)
        }

        if (result is ApiResult.Success) {
            iconCache[iconId] = result.data
        }

        return result
    }

    override fun getIconCached(iconId: String): IconResponse? {
        return iconCache[iconId]
    }
}