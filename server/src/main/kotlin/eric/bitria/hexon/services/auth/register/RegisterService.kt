package eric.bitria.hexon.services.auth.register


import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.dtos.auth.RegisterResponse

interface RegisterService {

    /**
     * Handles the full registration flow:
     * 1. Validates inputs.
     * 2. Checks if Email or Username are taken (via AuthRepository).
     * 3. Hashes the password.
     * 4. Creates the User in the DB.
     * 5. Triggers the Verification Email (via EmailVerificationService).
     */
    suspend fun register(request: RegisterRequest): RegisterResponse
}