package eric.bitria.hexon.ui.screens

import kotlinx.serialization.Serializable

@Serializable
sealed class Screens {
    @Serializable
    object AuthGraph : Screens()

    @Serializable
    object MainGraph : Screens()

    @Serializable
    object ForgotPasswordGraph : Screens()

    @Serializable
    object Login : Screens()

    @Serializable
    data class Verify(val email: String) : Screens()

    @Serializable
    object ForgotPassword : Screens()

    @Serializable
    data class ResetPassword(val email: String? = null) : Screens()

    @Serializable
    object ChangePassword : Screens()

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
    data class FriendProfile(val userId: String) : Screens()

    @Serializable
    sealed class GameSubScreens {
        @Serializable object MainMenu : GameSubScreens()
        @Serializable object Matchmaking : GameSubScreens()
        @Serializable object Lobby : GameSubScreens()
        @Serializable object Gameplay : GameSubScreens()
    }
}
