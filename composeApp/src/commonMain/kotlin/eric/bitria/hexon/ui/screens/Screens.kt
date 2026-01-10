package eric.bitria.hexon.ui.screens

import kotlinx.serialization.Serializable

@Serializable
sealed class Screens {
    @Serializable
    object Login : Screens()

    @Serializable
    data class Verify(val email: String) : Screens()

    @Serializable
    object SendPasswordResetCode : Screens()

    @Serializable
    data class ForgotPassword(val email: String) : Screens()

    @Serializable
    object ResetPassword : Screens()

    @Serializable
    object MainMenu : Screens()

    @Serializable
    object Game : Screens()

    @Serializable
    object Profile : Screens()

    @Serializable
    object Friends : Screens()

    @Serializable
    object Settings : Screens()

    @Serializable
    object DeleteAccount : Screens()

    @Serializable
    data class FriendProfile(val username: String) : Screens()
}
