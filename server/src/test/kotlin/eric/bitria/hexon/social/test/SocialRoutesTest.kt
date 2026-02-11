package eric.bitria.hexon.social.test

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import eric.bitria.hexon.auth.mock.MockAuthRepository
import eric.bitria.hexon.dtos.social.AddFriendRequest
import eric.bitria.hexon.dtos.social.AddFriendResponse
import eric.bitria.hexon.dtos.social.AddFriendResult
import eric.bitria.hexon.dtos.social.FriendRequestAction
import eric.bitria.hexon.dtos.social.RespondFriendRequest
import eric.bitria.hexon.dtos.social.RespondFriendResponse
import eric.bitria.hexon.dtos.social.RespondFriendResult
import eric.bitria.hexon.routes.socialRoutes
import eric.bitria.hexon.services.auth.repository.AuthRepository
import eric.bitria.hexon.services.auth.repository.User
import eric.bitria.hexon.services.social.SocialService
import eric.bitria.hexon.services.social.SocialServiceImpl
import eric.bitria.hexon.services.social.repository.FriendRequestRepository
import eric.bitria.hexon.services.social.repository.FriendsRepository
import eric.bitria.hexon.social.mock.MockFriendRequestRepository
import eric.bitria.hexon.social.mock.MockFriendsRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

class SocialRoutesTest {

    private val authRepository = MockAuthRepository()
    private val friendsRepository = MockFriendsRepository()
    private val friendRequestRepository = MockFriendRequestRepository()

    private val socialService = SocialServiceImpl(
        friendsRepository,
        friendRequestRepository,
        authRepository
    )

    private fun testSocialApplication(block: suspend (HttpClient) -> Unit) = testApplication {
        install(Koin) {
            modules(module {
                single<AuthRepository> { authRepository }
                single<FriendsRepository> { friendsRepository }
                single<FriendRequestRepository> { friendRequestRepository }
                single<SocialService> { socialService }
            })
        }
        install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
        install(Authentication) {
            jwt {
                verifier(JWT.require(Algorithm.HMAC256("secret")).build())
                validate { credential ->
                    if (credential.payload.subject != null) {
                        JWTPrincipal(credential.payload)
                    } else {
                        null
                    }
                }
            }
        }
        routing {
            socialRoutes()
        }
        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }
        block(client)
    }

    private fun generateTestToken(userId: String): String {
        return JWT.create()
            .withSubject(userId)
            .sign(Algorithm.HMAC256("secret"))
    }

    @Test
    fun `send friend request success`() = testSocialApplication { client ->
        val userId = "user1"
        val targetUsername = "user2"

        authRepository.addUser(User("user1", "user1@test.com", "user1", "pass", true))
        authRepository.addUser(User("user2", "user2@test.com", targetUsername, "pass", true))

        val response: AddFriendResponse = client.post("/friends/add") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken(userId)}")
            contentType(ContentType.Application.Json)
            setBody(AddFriendRequest(targetUsername))
        }.body()

        assertEquals(AddFriendResult.SUCCESS, response.result)
        assertTrue(friendRequestRepository.hasPendingRequest("user1", "user2"))
    }

    @Test
    fun `send friend request fails for non-existent user`() = testSocialApplication { client ->
        val userId = "user1"
        authRepository.addUser(User(userId, "user1@test.com", "user1", "pass", true))

        val response: AddFriendResponse = client.post("/friends/add") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken(userId)}")
            contentType(ContentType.Application.Json)
            setBody(AddFriendRequest("nonexistent"))
        }.body()

        assertEquals(AddFriendResult.USER_NOT_FOUND, response.result)
    }

    @Test
    fun `send friend request fails when adding self`() = testSocialApplication { client ->
        val userId = "user1"
        authRepository.addUser(User(userId, "user1@test.com", "user1", "pass", true))

        val response: AddFriendResponse = client.post("/friends/add") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken(userId)}")
            contentType(ContentType.Application.Json)
            setBody(AddFriendRequest("user1"))
        }.body()

        assertEquals(AddFriendResult.CANNOT_ADD_SELF, response.result)
    }

    @Test
    fun `send friend request fails when already friends`() = testSocialApplication { client ->
        val userId = "user1"
        val targetId = "user2"

        authRepository.addUser(User(userId, "user1@test.com", "user1", "pass", true))
        authRepository.addUser(User(targetId, "user2@test.com", "user2", "pass", true))
        friendsRepository.addFriendship(userId, targetId)

        val response: AddFriendResponse = client.post("/friends/add") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken(userId)}")
            contentType(ContentType.Application.Json)
            setBody(AddFriendRequest("user2"))
        }.body()

        assertEquals(AddFriendResult.ALREADY_FRIENDS, response.result)
    }

    @Test
    fun `send friend request fails when request already sent`() = testSocialApplication { client ->
        val userId = "user1"
        val targetId = "user2"

        authRepository.addUser(User(userId, "user1@test.com", "user1", "pass", true))
        authRepository.addUser(User(targetId, "user2@test.com", "user2", "pass", true))
        friendRequestRepository.createRequest(userId, targetId)

        val response: AddFriendResponse = client.post("/friends/add") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken(userId)}")
            contentType(ContentType.Application.Json)
            setBody(AddFriendRequest("user2"))
        }.body()

        assertEquals(AddFriendResult.REQUEST_ALREADY_SENT, response.result)
    }

    @Test
    fun `accept friend request success`() = testSocialApplication { client ->
        val userId = "user2"
        val requesterId = "user1"

        authRepository.addUser(User(requesterId, "user1@test.com", "user1", "pass", true))
        authRepository.addUser(User(userId, "user2@test.com", "user2", "pass", true))
        friendRequestRepository.createRequest(requesterId, userId)

        val response: RespondFriendResponse = client.post("/friends/respond") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken(userId)}")
            contentType(ContentType.Application.Json)
            setBody(RespondFriendRequest("user1", FriendRequestAction.ACCEPT))
        }.body()

        assertEquals(RespondFriendResult.SUCCESS, response.result)
        assertTrue(friendsRepository.areFriends(userId, requesterId))
        assertFalse(friendRequestRepository.hasPendingRequest(requesterId, userId))
    }

    @Test
    fun `decline friend request success`() = testSocialApplication { client ->
        val userId = "user2"
        val requesterId = "user1"

        authRepository.addUser(User(requesterId, "user1@test.com", "user1", "pass", true))
        authRepository.addUser(User(userId, "user2@test.com", "user2", "pass", true))
        friendRequestRepository.createRequest(requesterId, userId)

        val response: RespondFriendResponse = client.post("/friends/respond") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken(userId)}")
            contentType(ContentType.Application.Json)
            setBody(RespondFriendRequest("user1", FriendRequestAction.DECLINE))
        }.body()

        assertEquals(RespondFriendResult.SUCCESS, response.result)
        assertFalse(friendsRepository.areFriends(userId, requesterId))
        assertFalse(friendRequestRepository.hasPendingRequest(requesterId, userId))
    }
}

