package eric.bitria.hexon.ui.repository

import io.ktor.client.plugins.ResponseException
import kotlinx.io.IOException

sealed interface ApiResult<out T> {
    object Idle : ApiResult<Nothing>
    object Loading : ApiResult<Nothing>
    data class Success<out T>(val data: T) : ApiResult<T>
    data class Error(val message: String?) : ApiResult<Nothing>
    object NetworkError : ApiResult<Nothing>
}

suspend fun <T> safeApiCall(call: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(call())
    } catch (e: ResponseException) {
        ApiResult.Error(e.response.status.description)
    } catch (e: IOException) {
        ApiResult.NetworkError
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Unknown error")
    }
}
