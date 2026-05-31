package eric.bitria.hexonkmp.core

import kotlinx.serialization.json.Json

val AppJson = Json {
    classDiscriminator = "type"
    ignoreUnknownKeys = true
}
