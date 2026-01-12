package eric.bitria.hexon.viewmodel.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.client.SocialClient
import eric.bitria.hexon.dtos.social.AddFriendRequest
import eric.bitria.hexon.dtos.social.AddFriendResult
import eric.bitria.hexon.dtos.social.FriendRequestAction
import eric.bitria.hexon.dtos.social.GetFriendRequestsResult
import eric.bitria.hexon.dtos.social.GetFriendsResult
import eric.bitria.hexon.dtos.social.RespondFriendRequest
import eric.bitria.hexon.viewmodel.data.Friend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FriendsUiState(
    val friends: List<Friend> = emptyList(),
    val friendRequests: List<Friend> = emptyList(),
    val isLoading: Boolean = false,
    val addFriendMessage: String? = null,
    val friendsError: String? = null,
    val requestsError: String? = null
)

class FriendsViewModel(
    private val socialClient: SocialClient
) : ViewModel() {
    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        refreshFriends()
        refreshRequests()
    }

    fun refreshFriends() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, friendsError = null) }
            try {
                val response = socialClient.getFriends()
                if (response.result == GetFriendsResult.SUCCESS) {
                    _uiState.update { it.copy(
                        friends = response.friends.map { Friend(it.id, it.username, it.isOnline) }
                    ) }
                } else {
                    _uiState.update { it.copy(friendsError = response.message ?: "Failed to load friends") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(friendsError = "Connection error") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun refreshRequests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, requestsError = null) }
            try {
                val response = socialClient.getFriendRequests()
                if (response.result == GetFriendRequestsResult.SUCCESS) {
                    _uiState.update { it.copy(
                        friendRequests = response.requests.map { Friend(it.id, it.username, it.isOnline) }
                    ) }
                } else {
                    _uiState.update { it.copy(requestsError = response.message ?: "Failed to load requests") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(requestsError = "Connection error") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onAddFriendClicked(username: String) {
        viewModelScope.launch {
            // We clear it only when starting a new request to show fresh status
            _uiState.update { it.copy(addFriendMessage = null) }
            try {
                val response = socialClient.addFriend(AddFriendRequest(username))
                val message = when (response.result) {
                    AddFriendResult.SUCCESS -> "Request sent!"
                    AddFriendResult.USER_NOT_FOUND -> "User not found"
                    AddFriendResult.ALREADY_FRIENDS -> "Already friends"
                    AddFriendResult.REQUEST_ALREADY_SENT -> "Request already sent"
                    AddFriendResult.CANNOT_ADD_SELF -> "Cannot add yourself"
                    AddFriendResult.UNKNOWN_ERROR -> response.message ?: "Something went wrong"
                }
                _uiState.update { it.copy(addFriendMessage = message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(addFriendMessage = "Error sending request") }
            }
        }
    }

    fun onAcceptRequest(username: String) {
        viewModelScope.launch {
            try {
                socialClient.respondToFriendRequest(
                    RespondFriendRequest(username, FriendRequestAction.ACCEPT)
                )
                refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(requestsError = "Failed to accept request") }
            }
        }
    }

    fun onDeclineRequest(username: String) {
        viewModelScope.launch {
            try {
                socialClient.respondToFriendRequest(
                    RespondFriendRequest(username, FriendRequestAction.DECLINE)
                )
                refreshRequests()
            } catch (e: Exception) {
                _uiState.update { it.copy(requestsError = "Failed to decline request") }
            }
        }
    }
}
