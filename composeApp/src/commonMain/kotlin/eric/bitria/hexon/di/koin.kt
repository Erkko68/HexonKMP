package eric.bitria.hexon.di

import eric.bitria.hexon.AppViewModel
import eric.bitria.hexon.viewmodel.account.*
import eric.bitria.hexon.viewmodel.auth.LoginViewModel
import eric.bitria.hexon.viewmodel.auth.VerifyViewModel
import eric.bitria.hexon.viewmodel.game.GameSceneViewModel
import eric.bitria.hexon.viewmodel.game.GameViewModel
import eric.bitria.hexon.viewmodel.game.LobbyViewModel
import eric.bitria.hexon.viewmodel.game.MatchmakingViewModel
import eric.bitria.hexon.viewmodel.social.FriendProfileViewModel
import eric.bitria.hexon.viewmodel.social.FriendsViewModel
import eric.bitria.hexon.viewmodel.social.ProfileViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val viewModelsModule = module {
    viewModelOf(::GameSceneViewModel)
    viewModelOf(::LobbyViewModel)
    viewModelOf(::MatchmakingViewModel)
    viewModel { (sceneVM: GameSceneViewModel) ->
        GameViewModel(get(), sceneVM)
    }
    viewModelOf(::ProfileViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::VerifyViewModel)
    viewModelOf(::FriendsViewModel)
    viewModelOf(::FriendProfileViewModel)
    viewModelOf(::ResetPasswordViewModel)
    viewModelOf(::ForgotPasswordViewModel)
    viewModelOf(::ChangePasswordViewModel)
    viewModelOf(::DeleteAccountViewModel)
    viewModelOf(::AppViewModel)
}

fun initKoin(
    config: KoinAppDeclaration? = null
){
    startKoin {
        config?.invoke(this)
        modules(
            platformModule(),
            repositoryModule,
            storageModule,
            networkModule,
            viewModelsModule
        )
    }
}
