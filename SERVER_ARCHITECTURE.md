# Hexon Server Architecture

A **Kotlin Multiplatform** game server built with **Ktor** (HTTP + WebSocket), **Koin** (DI), **Jetbrains Exposed** (ORM), and **PostgreSQL**.

---

## System Overview

```mermaid
flowchart TB
    subgraph Clients["Clients"]
        AND["Android App"]
        DSK["Desktop App"]
    end

    subgraph KMP["Shared KMP Module"]
        SH["Game Models · DTOs · WebSocket Messages"]
    end

    subgraph Server["Ktor Server"]
        direction TB
        subgraph Routes["Routes Layer"]
            AR["AuthRoutes\nPOST /auth/*"]
            UR["UsersRoutes\nGET|POST|DELETE /users/*"]
            SR["SocialRoutes\nGET|POST /friends/*"]
            MR["MatchmakingRoutes\nPOST /game · POST /lobby · WS /game/{id}"]
        end
        subgraph Services["Service Layer"]
            direction LR
            AUTH["Auth Services\nLogin · Register · Refresh · Logout\nToken · EmailVerification · AccountVerification\nUserAccount · UserProfile"]
            SOC["SocialService"]
            GAME["Game Services\nMatchmaking · Lobby\nGameSessions · GameEngine"]
        end
        subgraph Repos["Repository Layer"]
            AUTHR["Auth + Profile + Social\nRepositories (Exposed)"]
            EMAILR["EmailVerification\nRepository (Exposed)"]
            GAMER["GameSession\nRepository (In-Memory)"]
        end
        Routes --> Services
        Services --> Repos
    end

    subgraph External["External"]
        DB[("PostgreSQL")]
        SMTP["SMTP Server"]
    end

    Clients -->|"HTTP / JWT"| Routes
    Clients <-->|"WebSocket JSON"| MR
    Repos --> DB
    AUTH -->|"Verification Codes"| SMTP
    Server -.->|"uses"| KMP
    Clients -.->|"uses"| KMP
```

---

## Auth & User Domain

```mermaid
classDiagram
    direction TB

    namespace Routes {
        class AuthRoutes {
            <<Route>>
            POST /auth/register
            POST /auth/login
            POST /auth/refresh
            POST /auth/logout
        }
        class UsersRoutes {
            <<Route>>
            POST /users/email/confirm|resend
            POST /users/password/change|forgot|reset
            POST /users/me/delete/initiate
            DELETE /users/me
            GET /users/me
            GET /users/id
        }
        class SocialRoutes {
            <<Route>>
            GET /friends
            GET /friends/requests
            POST /friends/add
            POST /friends/respond
        }
    }

    namespace AuthServices {
        class LoginService {
            <<interface>>
            +login(req) LoginResponse
        }
        class RegisterService {
            <<interface>>
            +register(req) RegisterResponse
        }
        class RefreshService {
            <<interface>>
            +refresh(req) RefreshResponse
        }
        class LogoutService {
            <<interface>>
            +logout(req) LogoutResponse
        }
        class TokenService {
            <<interface>>
            +generateAccessToken(userId, username) String
            +generateRefreshToken(userId) String
            +validateRefreshToken(token) String?
        }
        class AccountVerificationService {
            <<interface>>
            +verifyEmail(req) VerifyEmailResponse
            +resendVerificationCode(req)
        }
        class UserAccountService {
            <<interface>>
            +changePassword(userId, req)
            +forgotPassword(req)
            +resetPassword(req)
            +requestAccountDeletion(userId)
            +confirmAccountDeletion(userId, req)
        }
        class EmailVerificationService {
            <<interface>>
            +sendVerificationCode(email, type)
            +verifyCode(email, code, type) Boolean
        }
        class SmtpService {
            <<interface>>
            +sendEmail(to, subject, body)
        }
    }

    namespace UserServices {
        class UserProfileService {
            <<interface>>
            +getMyProfile(userId) UserProfileResponse
            +getPublicProfile(userId) PublicUserProfileResponse
        }
        class SocialService {
            <<interface>>
            +getFriends(userId) List~FriendDto~
            +getFriendRequests(userId) List~FriendDto~
            +sendFriendRequest(userId, target)
            +respondToRequest(userId, requester, action)
        }
    }

    namespace Repositories {
        class AuthRepository {
            <<interface>>
            +createUser() / findUserBy*()
            +addRefreshToken() / revokeRefreshToken()
            +revokeAllRefreshTokens() / hasRefreshTokenHash()
            +verifyUser() / updatePassword() / deleteUser()
        }
        class ProfileRepository {
            <<interface>>
            +createProfile(userId)
            +getUserProfile(userId) UserProfile
            +updateStats(userId, won)
        }
        class FriendsRepository {
            <<interface>>
            +getFriends(userId) List~Friend~
            +addFriendship(a, b) / removeFriendship(a, b)
            +areFriends(a, b) Boolean
        }
        class FriendRequestRepository {
            <<interface>>
            +createRequest(from, to)
            +deleteRequest(from, to)
            +getIncomingRequests(userId) List~Friend~
            +hasPendingRequest(a, b) Boolean
        }
        class EmailVerificationRepository {
            <<interface>>
            +saveVerificationCode(email, hash, type)
            +getVerificationCode(email) StoredVerificationCode?
            +deleteVerificationCode(email)
            +deleteExpiredCodes()
        }
    }

    namespace Database {
        class DatabaseTables {
            <<Exposed DSL>>
            Users: id · email · username · passwordHash · isVerified
            Sessions: userId · refreshTokenHash · expiresAt
            Profiles: userId · gamesWon · gamesLost
            Friends: userId · friendId
            FriendRequests: requesterId · receiverId
            EmailVerificationCodes: email · codeHash · type · expiresAt · attempts
        }
    }

    AuthRoutes --> LoginService
    AuthRoutes --> RegisterService
    AuthRoutes --> RefreshService
    AuthRoutes --> LogoutService

    UsersRoutes --> AccountVerificationService
    UsersRoutes --> UserAccountService
    UsersRoutes --> UserProfileService

    SocialRoutes --> SocialService

    LoginService --> AuthRepository
    LoginService --> TokenService
    RegisterService --> AuthRepository
    RegisterService --> EmailVerificationService
    RefreshService --> AuthRepository
    RefreshService --> TokenService
    LogoutService --> AuthRepository
    LogoutService --> TokenService

    AccountVerificationService --> AuthRepository
    AccountVerificationService --> EmailVerificationService
    AccountVerificationService --> TokenService
    AccountVerificationService --> ProfileRepository

    UserAccountService --> AuthRepository
    UserAccountService --> EmailVerificationService

    EmailVerificationService --> EmailVerificationRepository
    EmailVerificationService --> SmtpService
    EmailVerificationService --> AuthRepository

    UserProfileService --> ProfileRepository
    SocialService --> AuthRepository
    SocialService --> FriendsRepository
    SocialService --> FriendRequestRepository

    AuthRepository <|.. ExposedAuthRepository
    ProfileRepository <|.. ExposedProfileRepository
    FriendsRepository <|.. ExposedFriendsRepository
    FriendRequestRepository <|.. ExposedFriendRequestRepository
    EmailVerificationRepository <|.. ExposedEmailVerificationRepository

    ExposedAuthRepository --> DatabaseTables
    ExposedProfileRepository --> DatabaseTables
    ExposedFriendsRepository --> DatabaseTables
    ExposedFriendRequestRepository --> DatabaseTables
    ExposedEmailVerificationRepository --> DatabaseTables
```

---

## Game Domain

```mermaid
classDiagram
    direction TB

    namespace Routes {
        class MatchmakingRoutes {
            <<Route>>
            POST /game → findGameForPlayer()
            POST /lobby → createCustomGame()
            WS /game/sessionId → connectPlayer()
        }
    }

    namespace GameServices {
        class MatchmakingService {
            <<interface>>
            +findGameForPlayer(userId, mode, maxPlayers) JoinGameResponse
        }
        class LobbyService {
            <<interface>>
            +createCustomGame(userId, mode, maxPlayers) CreateLobbyResponse
        }
        class MatchmakingServiceImpl {
            -gameSessionRepository GameSessionRepository
            Find or create MatchmakingGameSession
            Auto-start when session is full
        }
        class LobbyServiceImpl {
            -gameSessionRepository GameSessionRepository
            Create CustomLobbySession
            Reserve host slot
        }
    }

    namespace SessionRepository {
        class GameSessionRepository {
            <<interface>>
            +addSession(session, mode)
            +removeSession(sessionId)
            +getSession(sessionId) BaseGameSession?
            +findAvailableSession(mode) BaseGameSession?
        }
        class InMemoryGameSessionRepository {
            <<In-Memory>>
            -allSessions ConcurrentHashMap
            -availableQueuesByMode ConcurrentHashMap
            implements SessionLifecycleListener
        }
    }

    namespace Sessions {
        class BaseGameSession {
            <<interface>>
            +hasAvailableSlots() Boolean
            +reserveSlot(userId)
            +connectPlayer(userId, wsSession)
            +removePlayer(userId)
            +handleIncomingMessage(userId, msg)
            +setLifecycleListener(listener)
        }
        class SessionLifecycleListener {
            <<interface>>
            +onGameStarting(session)
            +onGameEnded(session)
            +onSessionEmpty(session)
        }
        class GameMessageSender {
            <<interface>>
            +sendToPlayer(userId, msg)
            +broadcast(msg)
            +broadcast(msg, excludeUserId)
            +onGameEnded()
        }
        class MatchmakingGameSession {
            State: WAITING → STARTING → RUNNING → DISPOSED
            -connectedPlayers ConcurrentHashMap
            -reservedSlots ConcurrentHashMap
            -lobbyPlayers ConcurrentHashMap
            Auto-ready all players on connect
            Auto-start when full · 10s slot timeout
            implements BaseGameSession, GameMessageSender
        }
        class CustomLobbySession {
            State: LOBBY ⇄ GAME_STARTING → GAME_RUNNING → LOBBY
            -connectedPlayers ConcurrentHashMap
            -lobbyPlayers ConcurrentHashMap
            Host controls: color · start · reassign on leave
            Supports rematch (returns to LOBBY)
            implements BaseGameSession, GameMessageSender
        }
    }

    namespace Engine {
        class GameEngine {
            <<interface>>
            +start(players, sender)
            +endGame()
            +onMessage(userId, intent)
            +onPlayerLeave(userId)
            +onPlayerRejoin(userId, wsSession)
        }
        class GameEngineImpl {
            -board Board
            -players Map~PlayerId, GamePlayer~
            -trades Map~PlayerId, TradeOffer~
            -playerQueue / setupQueue ArrayDeque
            -currentPhase TurnPhase
            -currentTurnPlayerId String
            Setup phase: snake draft (1..N, N..1)
            Main phase: dice → produce → act → end turn
            Victory: 3+ points → endGame()
        }
    }

    namespace SharedModule {
        class SharedGameModule {
            <<Shared KMP>>
            Board: tiles · buildings · ports · robberLocation
            GamePlayer: resources · buildingCounts · victoryPoints
            GameConfig: seed · gridCoords · resourceDefs · buildingDefs · ports
            GameConfigLoader: default(seed) → 19-tile hex grid
            HexCoord: axial q/r · getVertexId() · getEdgeId()
            GameplayIntent: Build · EndTurn · ProposeTrade · MoveRobber · ExchangeWithBank
            GameplayEvent: GameSnapshot · ResourcesUpdated · ObjectBuilt · DiceRolled · GameEnded
            LobbyIntent: ToggleReady · ChangeColor · RequestStartGame
            LobbyEvent: LobbySnapshot · PlayerJoined · GameStarted
        }
    }

    MatchmakingRoutes --> MatchmakingService
    MatchmakingRoutes --> LobbyService
    MatchmakingRoutes --> GameSessionRepository

    MatchmakingService <|.. MatchmakingServiceImpl
    LobbyService <|.. LobbyServiceImpl
    MatchmakingServiceImpl --> GameSessionRepository
    LobbyServiceImpl --> GameSessionRepository

    GameSessionRepository <|.. InMemoryGameSessionRepository
    InMemoryGameSessionRepository ..|> SessionLifecycleListener

    BaseGameSession <|.. MatchmakingGameSession
    BaseGameSession <|.. CustomLobbySession
    GameMessageSender <|.. MatchmakingGameSession
    GameMessageSender <|.. CustomLobbySession

    MatchmakingGameSession --> GameEngine
    CustomLobbySession --> GameEngine
    GameEngine <|.. GameEngineImpl
    GameEngineImpl --> SharedGameModule
```

---

## Database Schema

```mermaid
erDiagram
    USERS {
        uuid id PK
        string email UK
        string username UK
        string passwordHash
        bool isVerified
    }
    SESSIONS {
        uuid id PK
        uuid userId FK
        string refreshTokenHash UK
        timestamp expiresAt
    }
    PROFILES {
        uuid userId PK
        int gamesWon
        int gamesLost
    }
    FRIENDS {
        uuid userId PK
        uuid friendId PK
        timestamp createdAt
    }
    FRIEND_REQUESTS {
        uuid requesterId PK
        uuid receiverId PK
        timestamp createdAt
    }
    EMAIL_VERIFICATION_CODES {
        string email PK
        string codeHash
        string type
        timestamp expiresAt
        int attempts
    }

    USERS ||--o{ SESSIONS : "has refresh tokens"
    USERS ||--o| PROFILES : "has stats"
    USERS ||--o{ FRIENDS : "is friends with"
    USERS ||--o{ FRIEND_REQUESTS : "sends/receives"
    USERS ||--o| EMAIL_VERIFICATION_CODES : "has pending code"
```

---

## WebSocket Protocol

```mermaid
classDiagram
    direction TB

    class GameMessage { <<sealed>> }

    class LobbyMessage { <<sealed>> }
    class GameplayMessage { <<sealed>> }

    class LobbyIntent {
        <<sealed · client→server>>
        LeaveLobby
        ToggleReady(isReady)
        ChangeColor(newColor)
        RequestStartGame
        ReadyForGame
    }
    class LobbyEvent {
        <<sealed · server→client>>
        LobbySnapshot(id, players, maxPlayers, colors)
        PlayerJoined(player)
        PlayerLeft(playerId)
        PlayerUpdated(player)
        GameStarted
        LobbyError(message, code)
    }
    class GameplayIntent {
        <<sealed · client→server>>
        EndTurn
        Build(buildingId, h1, h2, h3?)
        MoveRobber(hexA)
        ProposeTrade(give, want)
        RespondToTrade(offererId, accepted)
        ConfirmTrade(responderId)
        CancelTrade(offererId)
        ExchangeWithBank(give, get)
    }
    class GameplayEvent {
        <<sealed · server→client>>
        GameConfigLoaded(config)
        GameSnapshot(board, players, turn, dice, robber)
        PlayerJoined(snapshot)
        GamePlayerStats(gamePlayer)
        DiceRolled(roll1, roll2)
        TurnChanged(phase, playerId)
        ObjectBuilt(ownerId, buildingId, coords)
        RobberUpdated(location)
        ResourcesUpdated(changes, reason)
        ResourceCountUpdated(playerId, count, reason)
        TradeProposed / TradeResponse / TradeCompleted / TradeCancelled
        GameError(message, code)
        GameEnded(winnerId, gameId)
    }

    GameMessage <|-- LobbyMessage
    GameMessage <|-- GameplayMessage
    LobbyMessage <|-- LobbyIntent
    LobbyMessage <|-- LobbyEvent
    GameplayMessage <|-- GameplayIntent
    GameplayMessage <|-- GameplayEvent
```

---

## Session State Machines

```mermaid
stateDiagram-v2
    direction LR

    state "MatchmakingGameSession" as MM {
        [*] --> WAITING : created
        WAITING --> STARTING : session full\n(auto-ready all players)
        STARTING --> RUNNING : all ReadyForGame received
        RUNNING --> DISPOSED : game ended
        DISPOSED --> [*]
    }

    state "CustomLobbySession" as CL {
        [*] --> LOBBY : created
        LOBBY --> GAME_STARTING : host RequestStartGame\n(all players ready)
        GAME_STARTING --> GAME_RUNNING : all ReadyForGame received
        GAME_RUNNING --> LOBBY : game ended\n(rematch available)
        LOBBY --> [*] : all players left
    }
```

---

## Key Flows

### Registration & Email Verification
```
POST /auth/register → RegisterServiceImpl
  → validate email/username/password
  → BCrypt hash password (cost 12)
  → AuthRepository.createUser() (isVerified = false)
  → EmailVerificationService.sendCode(email, EMAIL_CONFIRMATION)
      → generate 6-digit code → BCrypt hash (cost 10) → store (15 min TTL)
      → SmtpService.sendEmail()

POST /users/email/confirm → AccountVerificationServiceImpl
  → EmailVerificationService.verifyCode()
  → AuthRepository.verifyUser()
  → ProfileRepository.createProfile()
  → TokenService.generateAccessToken() + generateRefreshToken()
  → AuthRepository.addRefreshToken(hash)
  → Return access + refresh tokens
```

### Token Lifecycle (Rotation)
```
POST /auth/refresh → RefreshServiceImpl
  → TokenService.validateRefreshToken() (signature + expiry)
  → AuthRepository.hasRefreshTokenHash() (revocation check)
  → Generate new access token + new refresh token
  → AuthRepository.updateRefreshToken() (replace old hash)
  → Return new token pair
```

### Game Matchmaking → Gameplay
```
POST /game → MatchmakingServiceImpl
  → GameSessionRepository.findAvailableSession(CLASSIC)
  → If none: create MatchmakingGameSession → addSession()
  → session.reserveSlot(userId) → return sessionId

WS /game/{sessionId} → MatchmakingRoutes
  → JWT auth → getSession(sessionId)
  → session.connectPlayer(userId, wsSession)
  → [session auto-starts when full]
  → GameEngine.start() → broadcast GameConfigLoaded + GameSnapshot
  → Message loop: deserialize → session.handleIncomingMessage()
      → LobbyIntent → session lobby logic
      → GameplayIntent → GameEngineImpl.onMessage()
  → Player disconnect: session.removePlayer()
```
