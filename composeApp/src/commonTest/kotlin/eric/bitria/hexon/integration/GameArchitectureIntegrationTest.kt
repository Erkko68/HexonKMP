package eric.bitria.hexon.integration

import eric.bitria.hexon.communication.GameCommunication
import eric.bitria.hexon.viewmodel.GameViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GameArchitectureIntegrationTest {

    @Test
    fun `full communication flow should work end-to-end`() = runTest {
        val viewModel = GameViewModel()
        val communication = viewModel.gameCommunication
        val receivedCommands = mutableListOf<String>()
        val receivedEvents = mutableListOf<String>()

        // Set up handler to capture outgoing commands
        communication.setSendJsonHandler { json ->
            receivedCommands.add(json)
        }

        // Set up collector for events
        val eventJob = launch {
            communication.gameEvents.collect { event ->
                receivedEvents.add(event)
            }
        }

        // Send command through ViewModel
        val testCommand = """{"type": "INIT_BOARD", "radius": 2}"""
        viewModel.sendCommand(testCommand)

        // Simulate receiving response from WebView
        val responseEvent = """{"type": "BOARD_INITIALIZED", "success": true}"""
        communication.handleReceivedJson(responseEvent)

        kotlinx.coroutines.delay(10)
        eventJob.cancel()

        // Verify command was sent
        assertEquals(1, receivedCommands.size)
        assertEquals(testCommand, receivedCommands[0])

        // Verify events were received
        assertEquals(2, receivedEvents.size)
        assertEquals(testCommand, receivedEvents[0]) // Command echoed as event
        assertEquals(responseEvent, receivedEvents[1]) // Response from WebView
    }

    @Test
    fun `bidirectional communication should maintain order`() = runTest {
        val communication = GameCommunication()
        val allMessages = mutableListOf<String>()

        val eventJob = launch {
            communication.gameEvents.collect { event ->
                allMessages.add(event)
            }
        }

        // Send alternating commands and responses
        communication.sendJson("""{"type": "command1"}""")
        communication.handleReceivedJson("""{"type": "response1"}""")
        communication.sendJson("""{"type": "command2"}""")
        communication.handleReceivedJson("""{"type": "response2"}""")

        kotlinx.coroutines.delay(10)
        eventJob.cancel()

        assertEquals(4, allMessages.size)
        assertEquals("""{"type": "command1"}""", allMessages[0])
        assertEquals("""{"type": "response1"}""", allMessages[1])
        assertEquals("""{"type": "command2"}""", allMessages[2])
        assertEquals("""{"type": "response2"}""", allMessages[3])
    }
}