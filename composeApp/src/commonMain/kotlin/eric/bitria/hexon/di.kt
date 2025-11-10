package eric.bitria.hexon

import eric.bitria.hexon.viewmodel.FriendProfileViewModel
import eric.bitria.hexon.viewmodel.FriendsViewModel
import eric.bitria.hexon.viewmodel.GameSceneViewModel
import eric.bitria.hexon.viewmodel.GameUIViewModel
import eric.bitria.hexon.viewmodel.LoginViewModel
import eric.bitria.hexon.viewmodel.MainMenuViewModel
import eric.bitria.hexon.viewmodel.ProfileViewModel
import eric.bitria.hexon.viewmodel.SettingsViewModel
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
    viewModelOf(::FriendsViewModel)
    viewModelOf(::FriendProfileViewModel)
}

fun initKoin(config: KoinAppDeclaration? = null){
    startKoin {
        config?.invoke(this)
        modules(viewModelsModule)
    }
}