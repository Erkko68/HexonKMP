package eric.bitria.hexon.di

import eric.bitria.hexon.repository.AuthRepository
import eric.bitria.hexon.repository.KtorAuthRepository
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val sharedModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                })
            }
        }
    }
    single<AuthRepository> { KtorAuthRepository(get()) }
}
