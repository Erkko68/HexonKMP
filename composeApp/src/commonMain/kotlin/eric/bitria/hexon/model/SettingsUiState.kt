package eric.bitria.hexon.model

data class SettingsUiState(
    val masterVolume: Float = 1.0f,
    val musicVolume: Float = 0.8f,
    val mirrorUI: Boolean = false,
    val muteAll: Boolean = false,
    val disableFriendRequests: Boolean = false,
    val disableGameInvites: Boolean = false
)

