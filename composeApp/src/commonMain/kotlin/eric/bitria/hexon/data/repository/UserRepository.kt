package eric.bitria.hexon.data.repository

import eric.bitria.hexon.data.local.TokenStorage
import eric.bitria.hexon.data.remote.UserClient
import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResult
import eric.bitria.hexon.dtos.account.ConfirmDeleteAccountRequest
import eric.bitria.hexon.dtos.account.DeleteAccountResult
import eric.bitria.hexon.dtos.account.ForgotPasswordRequest
import eric.bitria.hexon.dtos.account.ForgotPasswordResult
import eric.bitria.hexon.dtos.account.ResetPasswordRequest
import eric.bitria.hexon.dtos.account.ResetPasswordResult
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeRequest
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeResult
import eric.bitria.hexon.dtos.auth.VerifyEmailRequest
import eric.bitria.hexon.dtos.auth.VerifyEmailResult
import eric.bitria.hexon.dtos.profile.PublicUserProfileResponse
import eric.bitria.hexon.dtos.profile.UserProfileResponse

interface UserRepository {
    suspend fun getProfile(): ApiResult<UserProfileResponse>
    suspend fun verifyEmail(code: String, email: String): ApiResult<VerifyEmailResult>
    suspend fun resendVerificationCode(email: String): ApiResult<ResendVerificationCodeResult>
    suspend fun changePassword(old: String, new: String): ApiResult<ChangePasswordResult>
    suspend fun forgotPassword(email: String): ApiResult<ForgotPasswordResult>
    suspend fun resetPassword(email: String, code: String, new: String): ApiResult<ResetPasswordResult>
    suspend fun deleteAccount(password: String, code: String): ApiResult<DeleteAccountResult>
    suspend fun requestDeleteAccount(): ApiResult<String>
    suspend fun getPublicProfile(userId: String): ApiResult<PublicUserProfileResponse?>
}

class UserRepositoryImpl(
    private val userClient: UserClient,
    private val tokenStorage: TokenStorage
) : UserRepository {

    override suspend fun getProfile(): ApiResult<UserProfileResponse> {
        return safeApiCall {
            userClient.getMe()
        }
    }

    override suspend fun verifyEmail(code: String, email: String): ApiResult<VerifyEmailResult> {
        return safeApiCall {
            val response = userClient.verifyEmail(VerifyEmailRequest(email, code))
            if (response.result == VerifyEmailResult.SUCCESS) {
                response.accessToken?.let { tokenStorage.saveAccess(it) }
                response.refreshToken?.let { tokenStorage.saveRefresh(it) }
            }
            response.result
        }
    }

    override suspend fun resendVerificationCode(email: String): ApiResult<ResendVerificationCodeResult> {
        return safeApiCall {
            userClient.resendVerificationCode(ResendVerificationCodeRequest(email)).result
        }
    }

    override suspend fun changePassword(old: String, new: String): ApiResult<ChangePasswordResult> {
        return safeApiCall {
            val response = userClient.changePassword(ChangePasswordRequest(old, new))
            if (response.result == ChangePasswordResult.SUCCESS) {
                tokenStorage.clear()
            }
            response.result
        }
    }

    override suspend fun forgotPassword(email: String): ApiResult<ForgotPasswordResult> {
        return safeApiCall {
            userClient.forgotPassword(ForgotPasswordRequest(email)).result
        }
    }

    override suspend fun resetPassword(email: String, code: String, new: String): ApiResult<ResetPasswordResult> {
        return safeApiCall {
            val response = userClient.resetPassword(ResetPasswordRequest(email, code, new))
            if (response.result == ResetPasswordResult.SUCCESS) {
                tokenStorage.clear()
            }
            response.result
        }
    }

    override suspend fun deleteAccount(password: String, code: String): ApiResult<DeleteAccountResult> {
        return safeApiCall {
            val response = userClient.confirmDeleteAccount(ConfirmDeleteAccountRequest(password, code))
            if (response.result == DeleteAccountResult.SUCCESS) {
                tokenStorage.clear()
            }
            response.result
        }
    }

    override suspend fun requestDeleteAccount(): ApiResult<String> {
        return safeApiCall {
            userClient.initiateDeleteAccount().message
        }
    }

    override suspend fun getPublicProfile(userId: String): ApiResult<PublicUserProfileResponse?> {
        return safeApiCall {
            userClient.getPublicProfile(userId)
        }
    }
}
