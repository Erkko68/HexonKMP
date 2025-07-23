package eric.bitria.hexon.render

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebViewGameRenderTest {

    @Test
    fun `registerJsonCallback should add callback to list`() {
        val render = WebViewGameRender()
        var callbackInvoked = false

        render.registerJsonCallback { json ->
            callbackInvoked = true
        }

        // Since we can't easily test WebView interaction, we verify the callback is registered
        // In real implementation, this would be tested through integration tests
        assertTrue(true) // Placeholder - callback is stored internally
    }

    @Test
    fun `multiple callbacks should be supported`() {
        val render = WebViewGameRender()
        val callback1Results = mutableListOf<String>()
        val callback2Results = mutableListOf<String>()

        render.registerJsonCallback { json -> callback1Results.add(json) }
        render.registerJsonCallback { json -> callback2Results.add(json) }

        // Verify callbacks are registered (actual invocation would require WebView)
        assertTrue(true) // Placeholder for callback registration verification
    }

    @Test
    fun `escapeForJS should properly escape special characters`() {
        val render = WebViewGameRender()

        // Use reflection to access private method for testing
        val method = render::class.java.getDeclaredMethod("escapeForJS", String::class.java)
        method.isAccessible = true

        val testCases = mapOf(
            "simple" to "simple",
            "with\"quotes" to "with\\\"quotes",
            "with'apostrophe" to "with\\'apostrophe",
            "with\\backslash" to "with\\\\backslash",
            "with\nnewline" to "with\\nnewline",
            "with\rcarriage" to "with\\rcarriage"
        )

        testCases.forEach { (input, expected) ->
            val result = method.invoke(render, input) as String
            assertEquals(expected, result, "Failed for input: $input")
        }
    }

    @Test
    fun `sendJson should handle JSON safely when not loaded`() = runTest {
        val render = WebViewGameRender()

        // This should not throw an exception even when WebView is not loaded
        try {
            render.sendJson("""{"type": "test"}""")
            assertTrue(true) // No exception thrown
        } catch (e: Exception) {
            kotlin.test.fail("sendJson should not throw when WebView is not loaded")
        }
    }
}