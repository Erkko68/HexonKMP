package eric.bitria.hexon.users.password

import at.favre.lib.crypto.bcrypt.BCrypt
import eric.bitria.hexon.auth.repository.AuthRepository
import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResponse
import eric.bitria.hexon.dtos.account.ChangePasswordResult
import eric.bitria.hexon.utils.Validators

class PasswordServiceImpl(
    private val repository: AuthRepository
) : PasswordService {

    override suspend fun changePassword(userId: String, request: ChangePasswordRequest): ChangePasswordResponse {
        if (!Validators.isValidPassword(request.newPassword)) {
            return ChangePasswordResponse(
                ChangePasswordResult.INVALID_PASSWORD,
                "New password does not meet security requirements."
            )
        }

        // 1. Fetch User (to get current password hash)
        val user = repository.findUserById(userId)
            ?: return ChangePasswordResponse(ChangePasswordResult.USER_NOT_FOUND, "User not found")

        // 2. Verify OLD Password
        val oldPasswordMatches = BCrypt.verifyer().verify(
            request.oldPassword.toCharArray(),
            user.password
        ).verified

        if (!oldPasswordMatches) {
            return ChangePasswordResponse(
                ChangePasswordResult.WRONG_PASSWORD,
                "The old password you entered is incorrect."
            )
        }


        // 3. Hash NEW Password
        val newHash = BCrypt.withDefaults().hashToString(12, request.newPassword.toCharArray())

        // 4. Update DB
        repository.updatePassword(userId, newHash)

        // Invalidate old refresh tokens
        repository.updateRefreshToken(userId, null)

        return ChangePasswordResponse(
            ChangePasswordResult.SUCCESS,
            "Password changed successfully."
        )
    }
}