package eric.bitria.hexon.ui.repository

import eric.bitria.hexon.api.client.SessionManager
import eric.bitria.hexon.api.client.UserClient
import eric.bitria.hexon.dtos.account.*
import eric.bitria.hexon.dtos.auth.*
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
    private val sessionManager: SessionManager
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
                sessionManager.saveTokens(response.accessToken!!, response.refreshToken!!)
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
            userClient.changePassword(ChangePasswordRequest(old, new)).result
        }
    }

    override suspend fun forgotPassword(email: String): ApiResult<ForgotPasswordResult> {
        return safeApiCall {
            userClient.forgotPassword(ForgotPasswordRequest(email)).result
        }
    }

    override suspend fun resetPassword(email: String, code: String, new: String): ApiResult<ResetPasswordResult> {
        return safeApiCall {
            userClient.resetPassword(ResetPasswordRequest(email, code, new)).result
        }
    }

    override suspend fun deleteAccount(password: String, code: String): ApiResult<DeleteAccountResult> {
        return safeApiCall {
            userClient.confirmDeleteAccount(ConfirmDeleteAccountRequest(password, code)).result
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
