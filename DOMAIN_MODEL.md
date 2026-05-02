# Hexon Domain Model

Visual class-level overview of the Hexon server and the shared KMP module. Interfaces represent each capability; concrete implementations are noted but not detailed. Methods are grouped by intent to keep the diagrams readable.

---

## 1. Application Layer — Bootstrap & HTTP/WS Routing

```mermaid
classDiagram
    direction TB

    class Application {
        <<Ktor>>
        + module / configure*()
    }
    class AppModule {
        <<Koin DI>>
        + appModule() : Module
    }
    class DatabaseFactory {
        <<object>>
        + init(config)
        + dbQuery(block)
    }
    class Configs {
        <<data>>
        JwtConfig
        SmtpConfig
        CookieConfig
    }

    class AuthRoutes {
        <<REST>>
        /auth/register · /login
        /refresh · /logout
    }
    class UsersRoutes {
        <<REST>>
        /users/me · /id
        /email/* · /password/*
        /me/delete/*
    }
    class SocialRoutes {
        <<REST>>
        /friends · /requests
        /add · /respond
    }
    class MatchmakingRoutes {
        <<REST + WS>>
        POST /game · /lobby
        WS   /game/{sessionId}
    }

    Application --> AppModule : loads
    Application --> DatabaseFactory : initializes
    Application --> AuthRoutes
    Application --> UsersRoutes
    Application --> SocialRoutes
    Application --> MatchmakingRoutes
    AppModule --> Configs : provides
```

---

## 2. Authentication & User Account Domain

```mermaid
classDiagram
    direction TB

    class User {
        <<entity>>
        id, email, username
        password, isVerified
    }

    class TokenService {
        <<interface>>
        + generate / validate tokens
    }

    class AuthRepository {
        <<interface>>
        + user CRUD & lookup
        + refresh-token store
        + verification & deletion
    }

    class LoginService {
        <<interface>>
        + login(request)
    }
    class RegisterService {
        <<interface>>
        + register(request)
    }
    class RefreshService {
        <<interface>>
        + refresh(request)
    }
    class LogoutService {
        <<interface>>
        + logout(request)
    }
    class AccountVerificationService {
        <<interface>>
        + verifyEmail()
        + resendCode()
    }
    class UserAccountService {
        <<interface>>
        + change / forgot / reset password
        + request / confirm deletion
    }

    LoginService ..> AuthRepository
    LoginService ..> TokenService
    RegisterService ..> AuthRepository
    RegisterService ..> EmailVerificationService
    RefreshService ..> AuthRepository
    RefreshService ..> TokenService
    LogoutService ..> AuthRepository
    LogoutService ..> TokenService
    AccountVerificationService ..> AuthRepository
    AccountVerificationService ..> EmailVerificationService
    AccountVerificationService ..> ProfileRepository
    UserAccountService ..> AuthRepository
    UserAccountService ..> EmailVerificationService
    AuthRepository ..> User
```

> Implementations: `JwtTokenService`, `ExposedAuthRepository`, and `*ServiceImpl` classes wired through Koin.

---

## 3. Email Verification, Profiles & Social Domain

```mermaid
classDiagram
    direction TB

    class EmailVerificationType {
        <<enum>>
        EMAIL_CONFIRMATION
        PASSWORD_RESET
        ACCOUNT_DELETION
    }

    class SmtpService {
        <<interface>>
        + sendEmail(to, subject, body)
    }
    class EmailVerificationRepository {
        <<interface>>
        + save / get / delete codes
        + incrementAttempts()
    }
    class EmailVerificationService {
        <<interface>>
        + sendCode(byEmail / byUserId)
        + verifyCode(byEmail / byUserId)
    }

    class UserProfile {
        <<entity>>
        userId, email, username
        gamesWon, gamesLost
    }
    class ProfileRepository {
        <<interface>>
        + create / get / updateStats
    }
    class UserProfileService {
        <<interface>>
        + getMyProfile()
        + getPublicProfile()
    }

    class Friend {
        <<entity>>
        id, username, isOnline
    }
    class FriendsRepository {
        <<interface>>
        + list / check / add / remove
    }
    class FriendRequestRepository {
        <<interface>>
        + create / list / delete requests
    }
    class SocialService {
        <<interface>>
        + getFriends / getRequests
        + sendRequest / respond
    }

    EmailVerificationService ..> EmailVerificationRepository
    EmailVerificationService ..> SmtpService
    EmailVerificationService ..> AuthRepository
    EmailVerificationRepository ..> EmailVerificationType

    UserProfileService ..> ProfileRepository
    ProfileRepository ..> UserProfile

    SocialService ..> FriendsRepository
    SocialService ..> FriendRequestRepository
    SocialService ..> AuthRepository
    FriendsRepository ..> Friend
    FriendRequestRepository ..> Friend
```

---

## 4. Matchmaking & Game Session Domain

```mermaid
classDiagram
    direction TB

    class GameMode {
        <<enum>>
        CLASSIC
    }
    class LobbyPlayer {
        <<entity>>
        id, name, color
        isReady, isHost
    }

    class MatchmakingService {
        <<interface>>
        + findGameForPlayer()
    }
    class LobbyService {
        <<interface>>
        + createCustomGame()
        + invitePlayer()
    }

    class GameSessionRepository {
        <<interface>>
        + add / remove / get session
        + findAvailableSession()
    }

    class SessionLifecycleListener {
        <<interface>>
        + onGameStarting / Ended
        + onSessionEmpty
    }
    class GameMessageSender {
        <<interface>>
        + sendToPlayer / broadcast
        + onGameEnded(winnerId)
    }

    class BaseGameSession {
        <<interface>>
        sessionId
        + slot reservation
        + connect / remove player
        + handleIncomingMessage
        + setLifecycleListener
    }
    class MatchmakingGameSession {
        <<WAITING→RUNNING→DISPOSED>>
    }
    class CustomLobbySession {
        <<LOBBY⇄GAME_RUNNING>>
    }

    class GameEngine {
        <<interface>>
        + start(gameId, players, sender)
        + onMessage / onPlayerLeave / onPlayerRejoin
        + endGame(winnerId)
    }

    MatchmakingService ..> GameSessionRepository
    LobbyService ..> GameSessionRepository
    GameSessionRepository ..> BaseGameSession
    GameSessionRepository ..|> SessionLifecycleListener

    BaseGameSession <|.. MatchmakingGameSession
    BaseGameSession <|.. CustomLobbySession
    MatchmakingGameSession ..|> GameMessageSender
    CustomLobbySession ..|> GameMessageSender
    MatchmakingGameSession --> GameEngine
    CustomLobbySession --> GameEngine
    GameEngine ..> GameMessageSender
    BaseGameSession --> LobbyPlayer
    BaseGameSession --> GameMode
```

> Implementations: `MatchmakingServiceImpl`, `LobbyServiceImpl`, `InMemoryGameSessionRepository`, `GameEngineImpl`.

---

## 5. Shared — Game Configuration & Board Model

```mermaid
classDiagram
    direction TB

    class HexCoord {
        <<value>>
        q, r
        + hex/edge/vertex id helpers
    }

    class GameConfig {
        <<data>>
        seed, victoryPoints, tradeRatio
        resourceDefs, buildingDefs
        gridCoords, ports, fixedTiles
    }
    class GameConfigLoader {
        <<object>>
        + default(seed)
        + generateHexGrid()
        + default* pools
    }

    class ResourceDef {
        id, name
    }
    class BuildingDef {
        id, name, type
        cost, production, points
        upgrade / downgrade chain
    }
    class PlacementType {
        <<enum>>
        EDGE · VERTEX
    }
    class PortDef {
        h1, h2, h3
        resourceId, ratio
    }
    class FixedTile {
        resource, number
    }
    class HexTile {
        coordinate, resourceId, numberToken
    }
    class PlacedBuilding {
        ownerId, def
    }

    class Board {
        + initialize(config)
        + add tile / port
        + place / canPlace (vertex|edge)
        + getAvailable*Placements
        + getProductionForRoll(roll)
        + moveRobber(target)
    }

    GameConfigLoader ..> GameConfig
    GameConfig --> ResourceDef
    GameConfig --> BuildingDef
    GameConfig --> PortDef
    GameConfig --> FixedTile
    GameConfig --> HexCoord
    BuildingDef --> PlacementType
    Board --> HexTile
    Board --> PlacedBuilding
    Board --> PortDef
    HexTile --> HexCoord
    PlacedBuilding --> BuildingDef
```

---

## 6. Shared — Player State & Turn Model

```mermaid
classDiagram
    direction TB

    class GamePlayer {
        id, name, color, isHost
        resources, ports, buildingCounts
        developmentCards, victoryPoints
        knightsPlayed, longestRoad, largestArmy
        + add / deduct resources
        + canDeduct / canProposeTrade
        + port & bank exchange ratios
        + toSnapshot()
    }
    class PlayerSnapshot {
        <<dto>>
        id, name, color, victoryPoints
        resourceCount, devCardCount
        playedKnights, longestRoad, largestArmy
    }
    class BuildingSnapshot {
        <<dto>>
        ownerId, typeId
        hexA, hexB, hexC
    }
    class TradeOffer {
        give, want
    }
    class TurnPhase {
        <<enum>>
        SETUP · WAITING · TRADE
        MAIN_PHASE · ROBBER_RESOLUTION
        GAME_OVER
    }
    class UpdateReason {
        <<enum>>
        INITIAL · PRODUCTION · BUILD
        TRADE · THEFT · BANK · DEV_CARD
        COST · START
    }
    class GameErrorCode {
        <<enum>>
        NOT_YOUR_TURN
        INSUFFICIENT_RESOURCES
        INVALID_PLACEMENT · INVALID_TRADE
        UNKNOWN_BUILDING · UNKNOWN_ACTION
        GAME_ENDED
    }

    GamePlayer ..> PlayerSnapshot
    GamePlayer --> TradeOffer
```

---

## 7. Shared — WebSocket Message Protocol

```mermaid
classDiagram
    direction TB

    class GameMessage {
        <<sealed>>
    }
    class LobbyMessage {
        <<sealed>>
    }
    class GameplayMessage {
        <<sealed>>
    }

    class LobbyIntent {
        <<client→server>>
        LeaveLobby · ToggleReady
        ChangeColor · RequestStartGame
        ReadyForGame
    }
    class LobbyEvent {
        <<server→client>>
        LobbySnapshot · PlayerJoined/Left/Updated
        GameStarted · LobbyError
    }

    class GameplayIntent {
        <<client→server>>
        EndTurn · Build · MoveRobber
        ProposeTrade · RespondToTrade
        ConfirmTrade · CancelTrade
        ExchangeWithBank
    }
    class GameplayEvent {
        <<server→client>>
        GameConfigLoaded · GameSnapshot
        PlayerJoined · PlayerStats
        DiceRolled · TurnChanged
        ObjectBuilt · RobberUpdated
        Resources(Count)Updated
        Trade(Proposed/Response/Completed/Cancelled)
        GameError · GameEnded
    }

    GameMessage <|-- LobbyMessage
    GameMessage <|-- GameplayMessage
    LobbyMessage <|-- LobbyIntent
    LobbyMessage <|-- LobbyEvent
    GameplayMessage <|-- GameplayIntent
    GameplayMessage <|-- GameplayEvent
```

---

### Reading guide

- **Interfaces** describe every capability of the server. Concrete implementations (`*Impl`, `Exposed*`, `InMemory*`) are wired via Koin and follow a one-to-one mapping with their interface.
- **Solid arrows (→)** are *uses / depends on* relationships; **dashed-impl arrows (..|>)** denote interface implementation; **inheritance triangles (<|--)** denote sealed-class hierarchies.
- The shared module (sections 5–7) is consumed by both the server and the KMP clients, guaranteeing a single source of truth for game rules and the WebSocket protocol.
