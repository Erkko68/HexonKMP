package eric.bitria.hexon.auth.register

import eric.bitria.hexon.dtos.auth.*

interface RegisterService {

    /**
     * Registers a new user with the provided request.
     */
    suspend fun register(request: RegisterRequest): RegisterResponse
}
