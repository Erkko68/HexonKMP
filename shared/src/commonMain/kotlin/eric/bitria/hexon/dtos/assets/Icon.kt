package eric.bitria.hexon.dtos.assets

import kotlinx.serialization.Serializable

@Serializable
data class IconResponse(
    val svg: String,
    val color: String
)