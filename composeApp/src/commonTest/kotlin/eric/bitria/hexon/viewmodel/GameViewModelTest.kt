package eric.bitria.hexon.viewmodel

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GameViewModelTest {

    @Test
    fun `gameCommunication should be initialized`() {
        val viewModel = GameViewModel()
        assertNotNull(viewModel.gameCommunication)
    }

    @Test
    fun `gameEvents flow should be exposed`() {
        val viewModel = GameViewModel()
        assertNotNull(viewModel.gameEvents)
    }

    @Test
    fun `sendCommand should send JSON through communication`() = runTest {
        val viewModel = GameViewModel()
        val testCommand = """{"type": "INIT_BOARD", "radius": 2}"""

        val job = launch {
            val receivedEvent = viewModel.gameEvents.first()
            assertEquals(testCommand, receivedEvent)
        }

        viewModel.sendCommand(testCommand)
        job.join()
    }

    @Test
    fun `sendCommand with complex JSON should work`() = runTest {
        val viewModel = GameViewModel()
        val complexCommand = """
            {
              "type": "INIT_BOARD",
              "config": {
                "radius": 2,
                "tiles": [
                  {
                    "type": "forest",
                    "position": { "q": 0, "r": 0 },
                    "token": 5
                  }
                ]
              }
            }
        """.trimIndent()

        val job = launch {
            val receivedEvent = viewModel.gameEvents.first()
            assertEquals(complexCommand, receivedEvent)
        }

        viewModel.sendCommand(complexCommand)
        job.join()
    }

    @Test
    fun `multiple sendCommand calls should emit multiple events`() = runTest {
        val viewModel = GameViewModel()
        val receivedEvents = mutableListOf<String>()

        val job = launch {
            viewModel.gameEvents.collect { event ->
                receivedEvents.add(event)
            }
        }

        viewModel.sendCommand("""{"type": "command1"}""")
        viewModel.sendCommand("""{"type": "command2"}""")
        viewModel.sendCommand("""{"type": "command3"}""")

        kotlinx.coroutines.delay(10)
        job.cancel()

        assertEquals(3, receivedEvents.size)
        assertEquals("""{"type": "command1"}""", receivedEvents[0])
        assertEquals("""{"type": "command2"}""", receivedEvents[1])
        assertEquals("""{"type": "command3"}""", receivedEvents[2])
    }
}