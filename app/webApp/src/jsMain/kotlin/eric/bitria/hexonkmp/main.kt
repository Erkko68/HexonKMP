@file:OptIn(kotlin.js.ExperimentalJsExport::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package eric.bitria.hexonkmp

import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window

// Web entry point. Filament's WebGL runtime (filament.js / filament.wasm) must be
// initialized before any Filament Kotlin/JS external is touched, so we init it
// first and only then mount Compose. See filament-kmp docs/getting-started.md.
fun main() {
    window.asDynamic()._filamentReady = ::startApp

    // Spread Filament's module into `window` so the Kotlin externals (which expect
    // top-level globals) resolve. Filament names nested types with '$'
    // (Camera$Fov) but the externals use '_' (Camera_Fov), so alias those too.
    js(
        """
        Filament.init([], function() {
            var nativeFetch = window.fetch;
            Object.assign(window, Filament);
            window.fetch = nativeFetch;
            Object.getOwnPropertyNames(Filament).forEach(function(k) {
                if (k.indexOf('${'$'}') !== -1) window[k.replace(/\${'$'}/g, '_')] = Filament[k];
            });
            window._filamentReady();
        });
        """,
    )
}

@JsExport
@Suppress("unused")
fun startApp() {
    val container = document.getElementById("root") ?: error("No #root element found")
    ComposeViewport(container) {
        App()
    }
}
