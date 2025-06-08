package eric.bitria.hexon

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform