package eric.bitria.hexon.data.serializable

import eric.bitria.hexon.data.serializable.actions.HexonAction
import eric.bitria.hexon.data.serializable.actions.PlaceBuildingAction
import eric.bitria.hexon.data.serializable.actions.PlaceRobberAction
import eric.bitria.hexon.data.serializable.actions.RollDiceAction
import eric.bitria.hexon.data.serializable.actions.TradeAction
import eric.bitria.hexon.data.serializable.actions.UseDevelopmentCardAction
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

val HexonSerializersModule = SerializersModule {
    polymorphic(HexonAction::class) {
        subclass(RollDiceAction::class)
        subclass(PlaceBuildingAction::class)
        subclass(UseDevelopmentCardAction::class)
        subclass(TradeAction::class)
        subclass(PlaceRobberAction::class)
        // Add more subclasses here
    }
}

val HexonJson = Json {
    serializersModule = HexonSerializersModule
    classDiscriminator = "type"
    encodeDefaults = true
}
