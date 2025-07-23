package eric.bitria.hexon.communication

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameCommunicationTest {

    @Test
    fun `sendJson should emit to gameEvents flow`() = runTest {
        val communication = GameCommunication()
        val testJson = """{"type": "test", "data": "value"}"""

        val job = launch {
            val receivedJson = communication.gameEvents.first()
            assertEquals(testJson, receivedJson)
        }

        communication.sendJson(testJson)
        job.join()
    }

    @Test
    fun `setSendJsonHandler should register handler correctly`() = runTest {
        val communication = GameCommunication()
        var capturedJson: String? = null

        communication.setSendJsonHandler { json ->
            capturedJson = json
        }

        val testJson = """{"type": "command", "action": "move"}"""
        communication.sendJson(testJson)

        assertEquals(testJson, capturedJson)
    }

    @Test
    fun `handleReceivedJson should emit to gameEvents flow`() = runTest {
        val communication = GameCommunication()
        val testJson = """{"type": "event", "result": "success"}"""

        val job = launch {
            val receivedJson = communication.gameEvents.first()
            assertEquals(testJson, receivedJson)
        }

        communication.handleReceivedJson(testJson)
        job.join()
    }

    @Test
    fun `multiple handlers should all receive sendJson calls`() = runTest {
        val communication = GameCommunication()
        val handler1Results = mutableListOf<String>()
        val handler2Results = mutableListOf<String>()

        communication.setSendJsonHandler { json -> handler1Results.add(json) }
        communication.setSendJsonHandler { json -> handler2Results.add(json) }

        val testJson1 = """{"type": "first"}"""
        val testJson2 = """{"type": "second"}"""

        communication.sendJson(testJson1)
        communication.sendJson(testJson2)

        assertEquals(listOf(testJson1, testJson2), handler1Results)
        assertEquals(listOf(testJson1, testJson2), handler2Results)
    }

    @Test
    fun `gameEvents flow should handle multiple emissions`() = runTest {
        val communication = GameCommunication()
        val receivedEvents = mutableListOf<String>()

        val job = launch {
            communication.gameEvents.collect { json ->
                receivedEvents.add(json)
            }
        }

        communication.handleReceivedJson("""{"event": 1}""")
        communication.handleReceivedJson("""{"event": 2}""")
        communication.sendJson("""{"command": 1}""")

        // Give coroutines time to process
        kotlinx.coroutines.delay(10)
        job.cancel()

        assertEquals(3, receivedEvents.size)
        assertTrue(receivedEvents.contains("""{"event": 1}"""))
        assertTrue(receivedEvents.contains("""{"event": 2}"""))
        assertTrue(receivedEvents.contains("""{"command": 1}"""))
    }
}