package eric.bitria.hexonkmp.ui.screens.lobby

import eric.bitria.hexonkmp.core.protocol.LobbyMember

// Lobby/main-menu states. Matchmaking and private lobbies share one waiting room
// ([InLobby]); they differ only by data — a private lobby has a [code] and a host,
// a matchmaking lobby has a [countdownSeconds]. The in-game state lives in its own
// screen (navigated to when the game starts).
sealed interface LobbyUiState {
    data object Idle : LobbyUiState
    data object Connecting : LobbyUiState
    data class InLobby(
        val members: List<LobbyMember>,
        val hostId: String? = null,          // which member is host (private lobby)
        val isHost: Boolean,
        val canStart: Boolean,
        val maxPlayers: Int,
        val code: String? = null,            // private lobby only
        val countdownSeconds: Int? = null,   // matchmaking auto-start only
    ) : LobbyUiState
    data class Error(val message: String) : LobbyUiState
}
