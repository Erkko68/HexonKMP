package eric.bitria.hexon.auth.refresh

import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResponse

interface RefreshService {
    /**
     * Rotates the tokens.
     * 1. Validates the incoming Refresh Token (Signature & Expiry).
     * 2. Extracts User ID.
     * 3. Compares the token against the Hash stored in the DB (Revocation/Reuse Check).
     * 4. Issues a NEW Access Token and a NEW Refresh Token (Rotation).
     * 5. Updates the DB with the new hash.
     */
    suspend fun refresh(request: RefreshRequest): RefreshResponse
}