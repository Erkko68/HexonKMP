package eric.bitria.hexon.routes

import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.server.http.content.staticFiles
import io.ktor.server.routing.Route
import java.io.File

fun Route.assetsRoutes(){

    staticFiles("/assets", File("assets")) {
        // 1. Cache Control (e.g., cache images for 1 day)
        cacheControl { _ ->
            listOf(CacheControl.MaxAge(maxAgeSeconds = 86400))
        }

        // 2. Content Type overrides
        contentType { file ->
            when (file.extension) {
                "json" -> ContentType.Application.Json
                "glb" -> ContentType.Application.OctetStream
                else -> null
            }
        }
    }

}