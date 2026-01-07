package eric.bitria.hexon

import eric.bitria.hexon.di.sharedModule
import eric.bitria.hexon.viewmodel.GameSceneViewModel
import eric.bitria.hexon.viewmodel.GameUIViewModel
import eric.bitria.hexon.viewmodel.MainMenuViewModel
import eric.bitria.hexon.viewmodel.SettingsViewModel
import eric.bitria.hexon.viewmodel.auth.ForgotPasswordViewModel
import eric.bitria.hexon.viewmodel.auth.LoginViewModel
import eric.bitria.hexon.viewmodel.auth.ResetPasswordViewModel
import eric.bitria.hexon.viewmodel.auth.VerifyViewModel
import eric.bitria.hexon.viewmodel.social.FriendProfileViewModel
import eric.bitria.hexon.viewmodel.social.FriendsViewModel
import eric.bitria.hexon.viewmodel.social.ProfileViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val viewModelsModule = module {
    viewModelOf(::GameUIViewModel)
    viewModelOf(::GameSceneViewModel)
    viewModelOf(::MainMenuViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::VerifyViewModel)
    viewModelOf(::FriendsViewModel)
    viewModelOf(::FriendProfileViewModel)
    viewModelOf(::ForgotPasswordViewModel)
    viewModelOf(::ResetPasswordViewModel)
}

fun initKoin(config: KoinAppDeclaration? = null){
    startKoin {
        config?.invoke(this)
        modules(sharedModule, viewModelsModule)
    }
}
