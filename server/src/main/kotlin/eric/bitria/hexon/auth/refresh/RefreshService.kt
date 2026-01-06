package eric.bitria.hexon.auth.refresh

import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResponse

interface RefreshService {
    suspend fun refresh(request: RefreshRequest): RefreshResponse
}
