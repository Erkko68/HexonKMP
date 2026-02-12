package eric.bitria.hexon.data.repository

import co.touchlab.kermit.Logger
import io.ktor.client.plugins.ResponseException
import kotlinx.io.IOException

private const val TAG = "ApiResult"

sealed interface ApiResult<out T> {
    object Idle : ApiResult<Nothing>
    object Loading : ApiResult<Nothing>
    data class Success<out T>(val data: T) : ApiResult<T>
    data class Error(val message: String?) : ApiResult<Nothing>
    object NetworkError : ApiResult<Nothing>
}

suspend fun <T> safeApiCall(call: suspend () -> T): ApiResult<T> {
    return try {
        Logger.d(TAG) { "safeApiCall: executing API call..." }
        val result = call()
        Logger.d(TAG) { "safeApiCall: API call successful" }
        ApiResult.Success(result)
    } catch (e: ResponseException) {
        Logger.e(TAG, e) { "safeApiCall: ResponseException - ${e.response.status.description}" }
        ApiResult.Error(e.response.status.description)
    } catch (e: IOException) {
        Logger.e(TAG, e) { "safeApiCall: IOException (NetworkError) - ${e.message}" }
        ApiResult.NetworkError
    } catch (e: Exception) {
        Logger.e(TAG, e) { "safeApiCall: Exception - ${e::class.simpleName}: ${e.message}" }
        ApiResult.Error(e.message ?: "Unknown error")
    }
}
