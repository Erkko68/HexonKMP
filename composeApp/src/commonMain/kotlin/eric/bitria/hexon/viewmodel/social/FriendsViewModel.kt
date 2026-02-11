package eric.bitria.hexon.viewmodel.social

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.data.repository.ApiResult
import eric.bitria.hexon.data.repository.SocialRepository
import eric.bitria.hexon.dtos.social.AddFriendResult
import eric.bitria.hexon.dtos.social.FriendRequestAction
import eric.bitria.hexon.dtos.social.GetFriendRequestsResult
import eric.bitria.hexon.dtos.social.GetFriendsResult
import eric.bitria.hexon.dtos.social.RespondFriendResult
import eric.bitria.hexon.viewmodel.data.Friend
import kotlinx.coroutines.launch

class FriendsViewModel(
    private val socialRepository: SocialRepository
) : ViewModel() {

    var friendsState by mutableStateOf<ApiResult<List<Friend>>>(ApiResult.Idle)
        private set

    var requestsState by mutableStateOf<ApiResult<List<Friend>>>(ApiResult.Idle)
        private set

    var addFriendState by mutableStateOf<ApiResult<AddFriendResult>>(ApiResult.Idle)
        private set

    init {
        refresh()
    }

    fun refresh() {
        refreshFriends()
        refreshRequests()
    }

    fun refreshFriends() {
        viewModelScope.launch {
            friendsState = ApiResult.Loading
            
            when (val result = socialRepository.getFriends()) {
                is ApiResult.Success -> {
                    val response = result.data
                    friendsState = if (response.result == GetFriendsResult.SUCCESS) {
                        ApiResult.Success(
                            response.friends.map { Friend(it.id, it.username, it.isOnline) }
                        )
                    } else {
                        ApiResult.Error(response.message ?: "Failed to load friends")
                    }
                }
                is ApiResult.NetworkError -> {
                    friendsState = ApiResult.NetworkError
                }
                is ApiResult.Error -> {
                    friendsState = ApiResult.Error(result.message ?: "Failed to load friends")
                }
                else -> {}
            }
        }
    }

    fun refreshRequests() {
        viewModelScope.launch {
            requestsState = ApiResult.Loading
            
            when (val result = socialRepository.getFriendRequests()) {
                is ApiResult.Success -> {
                    val response = result.data
                    if (response.result == GetFriendRequestsResult.SUCCESS) {
                        requestsState = ApiResult.Success(
                            response.requests.map { Friend(it.id, it.username, it.isOnline) }
                        )
                    } else {
                        requestsState = ApiResult.Error(response.message ?: "Failed to load requests")
                    }
                }
                is ApiResult.NetworkError -> {
                    requestsState = ApiResult.NetworkError
                }
                is ApiResult.Error -> {
                    requestsState = ApiResult.Error(result.message ?: "Failed to load requests")
                }
                else -> {}
            }
        }
    }

    fun onAddFriendClicked(username: String) {
        viewModelScope.launch {
            addFriendState = ApiResult.Loading
            
            when (val result = socialRepository.addFriend(username)) {
                is ApiResult.Success -> {
                    addFriendState = when (result.data) {
                        AddFriendResult.SUCCESS -> ApiResult.Success(AddFriendResult.SUCCESS)
                        AddFriendResult.USER_NOT_FOUND -> ApiResult.Error("User not found")
                        AddFriendResult.ALREADY_FRIENDS -> ApiResult.Error("Already friends")
                        AddFriendResult.REQUEST_ALREADY_SENT -> ApiResult.Error("Request already sent")
                        AddFriendResult.CANNOT_ADD_SELF -> ApiResult.Error("Cannot add yourself")
                        else -> ApiResult.Error("Something went wrong")
                    }
                }
                is ApiResult.NetworkError -> {
                    addFriendState = ApiResult.NetworkError
                }
                is ApiResult.Error -> {
                    addFriendState = ApiResult.Error(result.message ?: "Error sending request")
                }
                else -> {}
            }
        }
    }

    fun onAcceptRequest(username: String) {
        viewModelScope.launch {
            when (val result = socialRepository.respondToRequest(username, FriendRequestAction.ACCEPT)) {
                is ApiResult.Success -> {
                    if (result.data == RespondFriendResult.SUCCESS) {
                        refresh()
                    }
                }
                else -> {
                    // Handle failure if needed
                }
            }
        }
    }

    fun onDeclineRequest(username: String) {
        viewModelScope.launch {
            when (val result = socialRepository.respondToRequest(username, FriendRequestAction.DECLINE)) {
                is ApiResult.Success -> {
                    if (result.data == RespondFriendResult.SUCCESS) {
                        refreshRequests()
                    }
                }
                else -> {
                    // Handle failure if needed
                }
            }
        }
    }

    fun resetAddFriendState() {
        addFriendState = ApiResult.Idle
    }
}
