package eric.bitria.hexon.threejs.command

import eric.bitria.hexon.render.GameCommand
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromDynamic

/**
 * Handles deserialization of dynamic data into typed GameCommand objects.
 */
@OptIn(ExperimentalSerializationApi::class)
object CommandParser {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun parseSetHex(data: dynamic): GameCommand.SetHex {
        return json.decodeFromDynamic(GameCommand.SetHex.serializer(), data)
    }

    fun parseSetPort(data: dynamic): GameCommand.SetPort {
        return json.decodeFromDynamic(GameCommand.SetPort.serializer(), data)
    }

    fun parsePlaceBuilding(data: dynamic): GameCommand.PlaceBuilding {
        return json.decodeFromDynamic(GameCommand.PlaceBuilding.serializer(), data)
    }

    fun parseDiceRolled(data: dynamic): GameCommand.DiceRolled {
        return json.decodeFromDynamic(GameCommand.DiceRolled.serializer(), data)
    }

    fun parseRobberUpdated(data: dynamic): GameCommand.RobberUpdated {
        return json.decodeFromDynamic(GameCommand.RobberUpdated.serializer(), data)
    }

    fun parseShowVertexBuildingPositions(data: dynamic): GameCommand.ShowVertexBuildingPositions {
        return json.decodeFromDynamic(GameCommand.ShowVertexBuildingPositions.serializer(), data)
    }

    fun parseShowEdgeBuildingPositions(data: dynamic): GameCommand.ShowEdgeBuildingPositions {
        return json.decodeFromDynamic(GameCommand.ShowEdgeBuildingPositions.serializer(), data)
    }
}

