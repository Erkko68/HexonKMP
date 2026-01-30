package eric.bitria.hexon.api

import com.russhwolf.settings.Settings
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PersistentCookieStorage(private val settings: Settings) : CookiesStorage {
    private val mutex = Mutex()
    private val COOKIE_KEY = "cookie_USER_SESSION"

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        mutex.withLock {
            if (cookie.name == "USER_SESSION") {
                // Check if server is expiring the cookie
                if (cookie.maxAge == 0 || cookie.value.isBlank()) {
                    settings.remove(COOKIE_KEY)
                } else {
                    settings.putString(COOKIE_KEY, cookie.value)
                }
            }
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> {
        mutex.withLock {
            val tokenValue = settings.getString(COOKIE_KEY, "")
            if (tokenValue.isBlank()) return emptyList()

            return listOf(
                Cookie(
                    name = "USER_SESSION",
                    value = tokenValue,
                    domain = requestUrl.host,
                    path = "/",
                    secure = true,
                    httpOnly = true
                )
            )
        }
    }

    suspend fun hasSessionCookie(): Boolean {
        mutex.withLock {
            return settings.getString(COOKIE_KEY, "").isNotBlank()
        }
    }

    suspend fun clear() {
        mutex.withLock {
            settings.remove(COOKIE_KEY)
        }
    }

    override fun close() {}
}