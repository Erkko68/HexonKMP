package eric.bitria.hexon

import eric.bitria.hexon.client.di.networkModule
import eric.bitria.hexon.client.di.platformModule
import eric.bitria.hexon.client.di.platformStorageModule
import eric.bitria.hexon.client.di.repositoryModule
import eric.bitria.hexon.client.di.storageModule
import eric.bitria.hexon.viewmodel.GameSceneViewModel
import eric.bitria.hexon.viewmodel.GameUIViewModel
import eric.bitria.hexon.viewmodel.MainMenuViewModel
import eric.bitria.hexon.viewmodel.SettingsViewModel
import eric.bitria.hexon.viewmodel.account.ChangePasswordViewModel
import eric.bitria.hexon.viewmodel.account.ResetPasswordViewModel
import eric.bitria.hexon.viewmodel.account.ForgotPasswordViewModel
import eric.bitria.hexon.viewmodel.account.DeleteAccountViewModel
import eric.bitria.hexon.viewmodel.auth.LoginViewModel
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
    viewModelOf(::ResetPasswordViewModel)
    viewModelOf(::ForgotPasswordViewModel)
    viewModelOf(::ChangePasswordViewModel)
    viewModelOf(::DeleteAccountViewModel)
}

fun initKoin(config: KoinAppDeclaration? = null){
    startKoin {
        config?.invoke(this)
        modules(
            platformModule,
            platformStorageModule,
            storageModule,
            networkModule,
            repositoryModule,
            viewModelsModule
        )
    }
}
