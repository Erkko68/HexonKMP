package eric.bitria.hexonkmp.core.game

import eric.bitria.hexonkmp.core.game.action.PlaceRoad
import eric.bitria.hexonkmp.core.game.action.PlaceSettlement
import eric.bitria.hexonkmp.core.game.engine.CatanEngine
import eric.bitria.hexonkmp.core.game.model.GamePhase
import eric.bitria.hexonkmp.core.game.model.GameState

// Test helper: plays out the entire snake-draft setup by always taking the first
// legal settlement and a connecting legal road for whoever is current, leaving
// the game in the Play phase. Lets dice/turn tests start from a real played game.
fun CatanEngine.completeSetup(start: GameState): GameState {
    var state = start
    var guard = 0
    while (state.phase is GamePhase.Setup) {
        check(guard++ < 1000) { "setup did not terminate" }
        val current = state.currentPlayer
        val vertex = legalSettlements(state, current).first()
        state = reduce(state, current, PlaceSettlement(vertex)).state
        val edge = legalRoads(state, current).first()
        state = reduce(state, current, PlaceRoad(edge)).state
    }
    return state
}
