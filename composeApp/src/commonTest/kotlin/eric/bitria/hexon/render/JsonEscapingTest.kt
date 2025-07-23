package eric.bitria.hexon.render

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonEscapingTest {

    @Test
    fun `test JSON string escaping edge cases`() {
        val testCases = mapOf(
            // Basic cases
            "" to "",
            "normal text" to "normal text",

            // Quote escaping
            "\"" to "\\\"",
            "'" to "\\'",
            "\"Hello\"" to "\\\"Hello\\\"",
            "'Hello'" to "\\'Hello\\'",

            // Backslash escaping
            "\\" to "\\\\",
            "\\n" to "\\\\n", // Literal \n, not newline

            // Newline and carriage return
            "\n" to "\\n",
            "\r" to "\\r",
            "\r\n" to "\\r\\n",

            // Unicode line separators
            "\u2028" to "\\u2028",
            "\u2029" to "\\u2029",

            // Complex combinations
            "Line 1\nLine \"2\" with 'quotes'\rEnd" to "Line 1\\nLine \\\"2\\\" with \\'quotes\\'\\rEnd",
            "{\"key\": \"value\"}" to "{\\\"key\\\": \\\"value\\\"}",
            "C:\\Users\\Name" to "C:\\\\Users\\\\Name"
        )

        testCases.forEach { (input, expected) ->
            val result = escapeForJS(input)
            assertEquals(expected, result, "Failed for input: '$input'")
        }
    }

    // Helper function that mirrors the private method in WebViewGameRender
    private fun escapeForJS(input: String): String = input
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\'", "\\'")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\u2028", "\\u2028")
        .replace("\u2029", "\\u2029")
}