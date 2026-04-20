# Hexon Domain Model

Complete class-level reference for the server and shared KMP modules, organized by domain.

---

## 1. Infrastructure & Routing

```mermaid
classDiagram
    direction TB

    namespace Infrastructure {
        class Application {
            <<Ktor Module>>
            +main(args: Array~String~)
            +module()
            +configureRouting()
            +configureSecurity()
            +configureSerialization()
            +configureWebSockets()
        }
        class DatabaseFactory {
            <<object>>
            +init(config: ApplicationConfig)
            +dbQuery(block: () → T) T
            -retryConnect(config: ApplicationConfig)
        }
        class AppModule {
            +appModule() Module
        }
        class JwtConfig {
            <<data>>
            +issuer: String
            +audience: String
            +secret: String
            +realm: String
            +accessTokenTtlMillis: Long
            +refreshTokenTtlMillis: Long
        }
        class SmtpConfig {
            <<data>>
            +smtpUser: String
            +smtpPassword: String
            +smtpHost: String
            +smtpPort: String
            +fromConfig(config: ApplicationConfig) SmtpConfig
        }
        class CookieConfig {
            <<data>>
            +secret: String
            +maxAge: Int
        }
    }

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
            POST /users/email/confirm
            POST /users/email/resend
            POST /users/password/change
            POST /users/password/forgot
            POST /users/password/reset
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
        class MatchmakingRoutes {
            <<Route>>
            POST /game
            POST /lobby
            WS /game/sessionId
        }
    }

    Application ..> AppModule : loads DI
    Application ..> DatabaseFactory : initializes
    Application ..> AuthRoutes : registers
    Application ..> UsersRoutes : registers
    Application ..> SocialRoutes : registers
    Application ..> MatchmakingRoutes : registers
    AppModule ..> JwtConfig : provides
    AppModule ..> SmtpConfig : provides
    AppModule ..> CookieConfig : provides
```

---

## 2. Authentication Domain

```mermaid
classDiagram
    direction TB

    class User {
        <<data>>
        +id: String
        +email: String
        +username: String
        +password: String
        +isVerified: Boolean
    }

    class TokenService {
        <<interface>>
        +generateAccessToken(userId: String, username: String) String
        +generateRefreshToken(userId: String) String
        +validateRefreshToken(token: String) String?
    }

    class JwtTokenService {
        -config: JwtConfig
    }

    class AuthRepository {
        <<interface>>
        +isEmailRegistered(email: String) Boolean
        +isUsernameTaken(username: String) Boolean
        +createUser(email: String, username: String, passwordHash: String) User
        +findUserByEmail(email: String) User?
        +findUserById(userId: String) User?
        +findUserByUsername(username: String) User?
        +addRefreshToken(userId: String, hash: String, expiresAt: LocalDateTime)
        +updateRefreshToken(oldHash: String, newHash: String, expiresAt: LocalDateTime) Boolean
        +hasRefreshTokenHash(hash: String) Boolean
        +revokeRefreshToken(hash: String)
        +revokeAllRefreshTokens(userId: String)
        +clearExpiredSessions()
        +verifyUser(userId: String)
        +updatePassword(userId: String, newHash: String)
        +deleteUser(userId: String)
    }

    class ExposedAuthRepository {
        <<Exposed ORM>>
    }

    class LoginService {
        <<interface>>
        +login(request: LoginRequest) LoginResponse
    }

    class LoginServiceImpl {
        -authRepository: AuthRepository
        -tokenService: TokenService
    }

    class RegisterService {
        <<interface>>
        +register(request: RegisterRequest) RegisterResponse
    }

    class RegisterServiceImpl {
        -authRepository: AuthRepository
        -emailVerificationService: EmailVerificationService
    }

    class RefreshService {
        <<interface>>
        +refresh(request: RefreshRequest) RefreshResponse
    }

    class RefreshServiceImpl {
        -authRepository: AuthRepository
        -tokenService: TokenService
    }

    class LogoutService {
        <<interface>>
        +logout(request: LogoutRequest) LogoutResponse
    }

    class LogoutServiceImpl {
        -authRepository: AuthRepository
        -tokenService: TokenService
    }

    class EmailVerificationService {
        <<interface>>
        +sendVerificationCodeByEmail(email: String, type: EmailVerificationType)
        +sendVerificationCodeByUserId(userId: String, type: EmailVerificationType)
        +verifyCodeByEmail(email: String, code: String, type: EmailVerificationType) Boolean
        +verifyCodeByUserId(userId: String, code: String, type: EmailVerificationType) Boolean
    }

    class AccountVerificationService {
        <<interface>>
        +verifyEmail(request: VerifyEmailRequest) VerifyEmailResponse
        +resendVerificationCode(request: ResendVerificationCodeRequest) ResendVerificationCodeResponse
    }

    class AccountVerificationServiceImpl {
        -authRepository: AuthRepository
        -emailVerificationService: EmailVerificationService
        -tokenService: TokenService
        -profileRepository: ProfileRepository
    }

    class UserAccountService {
        <<interface>>
        +changePassword(userId: String, request: ChangePasswordRequest) ChangePasswordResponse
        +forgotPassword(request: ForgotPasswordRequest) ForgotPasswordResponse
        +resetPassword(request: ResetPasswordRequest) ResetPasswordResponse
        +requestAccountDeletion(userId: String) RequestDeleteAccountResponse
        +confirmAccountDeletion(userId: String, request: ConfirmDeleteAccountRequest) ConfirmDeleteAccountResponse
    }

    class UserAccountServiceImpl {
        -authRepository: AuthRepository
        -emailVerificationService: EmailVerificationService
    }

    TokenService <|.. JwtTokenService
    AuthRepository <|.. ExposedAuthRepository
    LoginService <|.. LoginServiceImpl
    RegisterService <|.. RegisterServiceImpl
    RefreshService <|.. RefreshServiceImpl
    LogoutService <|.. LogoutServiceImpl
    AccountVerificationService <|.. AccountVerificationServiceImpl
    UserAccountService <|.. UserAccountServiceImpl

    LoginServiceImpl --> AuthRepository
    LoginServiceImpl --> TokenService
    RegisterServiceImpl --> AuthRepository
    RegisterServiceImpl --> EmailVerificationService
    RefreshServiceImpl --> AuthRepository
    RefreshServiceImpl --> TokenService
    LogoutServiceImpl --> AuthRepository
    LogoutServiceImpl --> TokenService
    AccountVerificationServiceImpl --> AuthRepository
    AccountVerificationServiceImpl --> EmailVerificationService
    AccountVerificationServiceImpl --> TokenService
    UserAccountServiceImpl --> AuthRepository
    UserAccountServiceImpl --> EmailVerificationService
    AuthRepository ..> User : returns
```

---

## 3. Email, Profile & Social Domain

```mermaid
classDiagram
    direction TB

    class StoredVerificationCode {
        <<data>>
        +codeHash: String
        +type: EmailVerificationType
        +expiresAt: Instant
    }

    class EmailVerificationType {
        <<enum>>
        EMAIL_CONFIRMATION
        PASSWORD_RESET
        ACCOUNT_DELETION
    }

    class EmailVerificationRepository {
        <<interface>>
        +saveVerificationCode(email: String, hash: String, type: EmailVerificationType, expiresAt: Instant)
        +getVerificationCode(email: String) StoredVerificationCode?
        +incrementAttempts(email: String)
        +deleteVerificationCode(email: String)
        +deleteExpiredCodes()
    }

    class ExposedEmailVerificationRepository {
        <<Exposed ORM>>
    }

    class SmtpService {
        <<interface>>
        +sendEmail(to: String, subject: String, body: String)
    }

    class SmtpServiceImp {
        -smtpConfig: SmtpConfig
        -session: Session
    }

    class EmailVerificationServiceImpl {
        -verificationRepo: EmailVerificationRepository
        -smtpService: SmtpService
        -authRepository: AuthRepository
        -codeValidityDuration: Duration
        -bcryptCost: Int
        +sendVerificationCodeByEmail(email, type)
        +sendVerificationCodeByUserId(userId, type)
        +verifyCodeByEmail(email, code, type) Boolean
        +verifyCodeByUserId(userId, code, type) Boolean
    }

    class UserProfile {
        <<data>>
        +userId: String
        +email: String
        +username: String
        +gamesWon: Int
        +gamesLost: Int
    }

    class ProfileRepository {
        <<interface>>
        +createProfile(userId: String)
        +getUserProfile(userId: String) UserProfile?
        +updateStats(userId: String, isWin: Boolean)
    }

    class ExposedProfileRepository {
        <<Exposed ORM>>
    }

    class UserProfileService {
        <<interface>>
        +getMyProfile(userId: String) UserProfileResponse
        +getPublicProfile(userId: String) PublicUserProfileResponse?
    }

    class UserProfileServiceImpl {
        -profileRepository: ProfileRepository
    }

    class Friend {
        <<data>>
        +id: String
        +username: String
        +isOnline: Boolean
    }

    class FriendsRepository {
        <<interface>>
        +getFriendsForUser(userId: String) List~Friend~
        +areFriends(userId1: String, userId2: String) Boolean
        +addFriendship(userId1: String, userId2: String) Boolean
        +removeFriendship(userId1: String, userId2: String) Boolean
    }

    class ExposedFriendsRepository {
        <<Exposed ORM>>
    }

    class FriendRequestRepository {
        <<interface>>
        +hasPendingRequest(requesterId: String, receiverId: String) Boolean
        +getIncomingRequests(receiverId: String) List~Friend~
        +createRequest(requesterId: String, receiverId: String) Boolean
        +deleteRequest(requesterId: String, receiverId: String) Boolean
    }

    class ExposedFriendRequestRepository {
        <<Exposed ORM>>
    }

    class SocialService {
        <<interface>>
        +getFriends(userId: String) GetFriendsResponse
        +getFriendRequests(userId: String) GetFriendRequestsResponse
        +sendFriendRequest(requesterId: String, targetUsername: String) AddFriendResponse
        +respondToRequest(userId: String, requesterUsername: String, action: FriendRequestAction) RespondFriendResponse
    }

    class SocialServiceImpl {
        -friendsRepository: FriendsRepository
        -requestsRepository: FriendRequestRepository
        -authRepository: AuthRepository
    }

    EmailVerificationRepository <|.. ExposedEmailVerificationRepository
    EmailVerificationRepository ..> StoredVerificationCode : returns
    SmtpService <|.. SmtpServiceImp
    EmailVerificationServiceImpl --> EmailVerificationRepository
    EmailVerificationServiceImpl --> SmtpService
    ProfileRepository <|.. ExposedProfileRepository
    ProfileRepository ..> UserProfile : returns
    UserProfileService <|.. UserProfileServiceImpl
    UserProfileServiceImpl --> ProfileRepository
    FriendsRepository <|.. ExposedFriendsRepository
    FriendsRepository ..> Friend : returns
    FriendRequestRepository <|.. ExposedFriendRequestRepository
    FriendRequestRepository ..> Friend : returns
    SocialService <|.. SocialServiceImpl
    SocialServiceImpl --> FriendsRepository
    SocialServiceImpl --> FriendRequestRepository
```

---

## 4. Game Session Domain

```mermaid
classDiagram
    direction TB

    class LobbyPlayer {
        <<data>>
        +id: String
        +name: String
        +color: String
        +isReady: Boolean
        +isHost: Boolean
    }

    class GameMode {
        <<enum>>
        CLASSIC
    }

    class MatchmakingService {
        <<interface>>
        +findGameForPlayer(userId: String, mode: GameMode, maxPlayers: Int) JoinGameResponse
    }

    class MatchmakingServiceImpl {
        -repository: GameSessionRepository
        -mutex: Mutex
    }

    class LobbyService {
        <<interface>>
        +createCustomGame(creatorId: String, mode: GameMode, maxPlayers: Int) CreateLobbyResponse
        +invitePlayer(sessionId: String, invitedUserId: String) Boolean
    }

    class LobbyServiceImpl {
        -repository: GameSessionRepository
        -mutex: Mutex
    }

    class GameSessionRepository {
        <<interface>>
        +addSession(mode: GameMode, session: BaseGameSession)
        +removeSession(mode: GameMode, sessionId: String)
        +getSession(sessionId: String) BaseGameSession?
        +findAvailableSession(mode: GameMode) BaseGameSession?
    }

    class InMemoryGameSessionRepository {
        <<In-Memory>>
        -allSessions: ConcurrentHashMap~String, BaseGameSession~
        -availableQueuesByMode: ConcurrentHashMap~GameMode, Queue~
        -sessionModes: ConcurrentHashMap~String, GameMode~
    }

    class SessionLifecycleListener {
        <<interface>>
        +onGameStarting(sessionId: String, gameId: String)
        +onGameEnded(sessionId: String, gameId: String)
        +onSessionEmpty(sessionId: String)
    }

    class GameMessageSender {
        <<interface>>
        +sendToPlayer(receiverId: String, message: GameMessage)
        +broadcast(message: GameMessage)
        +broadcast(message: GameMessage, excludeUserId: String?)
        +onGameEnded(winnerId: String?)
    }

    class BaseGameSession {
        <<interface>>
        +sessionId: String
        +hasAvailableSlots() Boolean
        +reserveSlot(userId: String) Boolean
        +connectPlayer(userId: String, username: String, session: DefaultWebSocketSession) Boolean
        +removePlayer(userId: String)
        +handleIncomingMessage(userId: String, message: GameMessage)
        +setLifecycleListener(listener: SessionLifecycleListener)
    }

    class MatchmakingGameSession {
        State: WAITING → STARTING → RUNNING → DISPOSED
        +sessionId: String
        -mode: GameMode
        -maxPlayers: Int
        -connectedPlayers: ConcurrentHashMap~String, WebSocketSession~
        -lobbyPlayers: ConcurrentHashMap~String, LobbyPlayer~
        -reservedSlots: ConcurrentHashMap~String, Long~
        -playersReadyForGame: ConcurrentSet~String~
        -engine: GameEngine?
        -currentGameId: String?
        -availableColors: List~String~
        +hasAvailableSlots() Boolean
        +reserveSlot(userId) Boolean
        +connectPlayer(userId, username, session) Boolean
        +removePlayer(userId)
        +handleIncomingMessage(userId, message)
        +setLifecycleListener(listener)
        +onGameEnded(winnerId: String?)
    }

    class CustomLobbySession {
        State: LOBBY ⇄ GAME_STARTING → GAME_RUNNING → LOBBY
        +sessionId: String
        -mode: GameMode
        -maxPlayers: Int
        -connectedPlayers: ConcurrentHashMap~String, WebSocketSession~
        -lobbyPlayers: ConcurrentHashMap~String, LobbyPlayer~
        -reservedSlots: ConcurrentHashMap~String, Long~
        -playersReadyForGame: ConcurrentSet~String~
        -engine: GameEngine?
        -currentGameId: String?
        -availableColors: List~String~
        +hasAvailableSlots() Boolean
        +reserveSlot(userId) Boolean
        +connectPlayer(userId, username, session) Boolean
        +removePlayer(userId)
        +handleIncomingMessage(userId, message)
        +setLifecycleListener(listener)
        +onGameEnded(winnerId: String?)
    }

    class GameEngine {
        <<interface>>
        +start(gameId: String, lobbyPlayers: List~LobbyPlayer~, sender: GameMessageSender)
        +endGame(winnerId: String?)
        +onMessage(userId: String, message: GameMessage)
        +onPlayerLeave(userId: String)
        +onPlayerRejoin(userId: String)
    }

    class GameEngineImpl {
        -gameId: String
        -gameConfig: GameConfig
        -sender: GameMessageSender
        -mutex: Mutex
        -players: MutableMap~String, GamePlayer~
        -board: Board
        -buildings: Map~BuildingId, BuildingDef~
        -trades: MutableMap~PlayerId, TradeOffer~
        -tradeAcceptances: MutableMap~PlayerId, Set~PlayerId~~
        -playerQueue: MutableList~String~
        -setupQueue: MutableList~String~
        -setupTurnIndex: Int
        -turnIndex: Int
        -currentTurnPlayerId: String
        -currentPhase: TurnPhase
    }

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
    GameEngineImpl --> GameMessageSender
```

---

## 5. Shared Game Logic

```mermaid
classDiagram
    direction TB

    class HexCoord {
        <<data>>
        +q: Int
        +r: Int
        +compareTo(other: HexCoord) Int
        +getHexId(h: HexCoord) String$
        +getEdgeId(h1: HexCoord, h2: HexCoord) String$
        +getVertexId(h1: HexCoord, h2: HexCoord, h3: HexCoord) String$
        +fromHexId(id: String) HexCoord$
        +fromEdgeId(id: String) Pair~HexCoord,HexCoord~$
        +fromVertexId(id: String) Triple~HexCoord,HexCoord,HexCoord~$
    }

    class ResourceDef {
        <<data>>
        +id: ResourceId
        +name: String
    }

    class BuildingDef {
        <<data>>
        +id: BuildingId
        +name: String
        +type: PlacementType
        +cost: Map~ResourceId, Int~
        +upgrade: BuildingId?
        +downgrade: BuildingId?
        +production: Int
        +points: Int
        +limitPerPlayer: Int
    }

    class PlacementType {
        <<enum>>
        EDGE
        VERTEX
    }

    class PortDef {
        <<data>>
        +h1: HexCoord
        +h2: HexCoord
        +h3: HexCoord
        +resourceId: String?
        +ratio: Int
    }

    class FixedTile {
        <<data>>
        +resource: ResourceId
        +number: Int
    }

    class GameConfig {
        <<data>>
        +seed: String
        +victoryPoints: Int
        +tradeRatio: Int
        +resourceDefs: List~ResourceDef~
        +buildingDefs: List~BuildingDef~
        +initialBuildings: List~BuildingId~
        +gridCoords: List~HexCoord~
        +ports: List~PortDef~
        +tileResourcePool: List~ResourceId~
        +tileNumberPool: List~Int~
        +fixedTiles: Map~HexCoord, FixedTile~
    }

    class GameConfigLoader {
        <<object>>
        +default(seed: String) GameConfig
        +generateHexGrid(radius: Int) List~HexCoord~
        +defaultResourcePool() List~ResourceId~
        +defaultPorts() List~PortDef~
        +defaultResourceDef() List~ResourceDef~
        +defaultBuildingDef() List~BuildingDef~
        +defaultInitialBuildings() List~BuildingId~
    }

    class HexTile {
        <<data>>
        +coordinate: HexCoord
        +resourceId: ResourceId
        +numberToken: Int
    }

    class PlacedBuilding {
        <<data>>
        +ownerId: PlayerId
        +def: BuildingDef
    }

    class Board {
        +availableResources: MutableMap~ResourceId, ResourceDef~
        +availableBuildings: MutableMap~BuildingId, BuildingDef~
        +tiles: MutableMap~String, HexTile~
        +buildings: MutableMap~String, PlacedBuilding~
        +ports: MutableMap~PortVertex, PortDef~
        +robberLocation: HexCoord
        +initialize(config: GameConfig)
        +addTile(coord: HexCoord, resource: ResourceId, number: Int)
        +addPort(h1: HexCoord, h2: HexCoord, h3: HexCoord, resource: ResourceId?, ratio: Int)
        +placeVertexBuilding(typeId, ownerId, h1, h2, h3, checkConnection) Boolean
        +placeEdgeBuilding(typeId, ownerId, h1, h2) Boolean
        +getBuildingAt(locId: String) PlacedBuilding?
        +canPlaceVertexBuilding(ownerId, h1, h2, h3, targetTypeId, checkConnection) Boolean
        +canPlaceEdgeBuilding(ownerId, h1, h2, targetTypeId) Boolean
        +getAvailableVertexPlacements(ownerId, buildingId, checkConnection) List~Triple~
        +getAvailableEdgePlacements(ownerId, buildingId) List~Pair~
        +getProductionForRoll(roll: Int) Map~String, Map~ResourceId, Int~~
        +moveRobber(target: HexCoord) List~PlayerId~
    }

    class TradeOffer {
        <<data>>
        +give: Map~ResourceId, Int~
        +want: Map~ResourceId, Int~
    }

    class PlayerSnapshot {
        <<data>>
        +id: PlayerId
        +name: String
        +color: String
        +victoryPoints: Int
        +resourceCount: Int
        +devCardCount: Int
        +playedKnights: Int
        +longestRoad: Boolean
        +largestArmy: Boolean
    }

    class BuildingSnapshot {
        <<data>>
        +ownerId: PlayerId
        +typeId: BuildingId
        +hexA: HexCoord
        +hexB: HexCoord
        +hexC: HexCoord?
    }

    class GamePlayer {
        <<data>>
        +id: String
        +name: String
        +color: String
        +isHost: Boolean
        +resources: MutableMap~ResourceId, Int~
        +ports: MutableMap~String, PortDef~
        +buildingCounts: MutableMap~BuildingId, Int~
        +developmentCardsHand: MutableList~String~
        +developmentCardsPlayed: MutableList~String~
        +victoryPoints: Int
        +knightsPlayed: Int
        +hasLongestRoad: Boolean
        +hasLargestArmy: Boolean
        +hasPlayedDevCardThisTurn: Boolean
        +addResources(changes: Map~ResourceId, Int~)
        +deductResources(cost: Map~ResourceId, Int~) Boolean
        +totalResourceCount() Int
        +canDeductResources(cost: Map~ResourceId, Int~) Boolean
        +canProposeTrade(give: Map~ResourceId,Int~, want: Map~ResourceId,Int~) Boolean
        +getPortDiscountRatio(resource: String?) Int
        +getEffectiveTradeRatio(resourceId: ResourceId, defaultRatio: Int) Int
        +calculateBankExchangeCost(give: Map~ResourceId,Int~, get: Map~ResourceId,Int~, defaultRatio: Int) Map~ResourceId,Int~?
        +toSnapshot() PlayerSnapshot
    }

    class TurnPhase {
        <<enum>>
        SETUP
        WAITING
        TRADE
        MAIN_PHASE
        ROBBER_RESOLUTION
        GAME_OVER
    }

    class UpdateReason {
        <<enum>>
        INITIAL
        PRODUCTION
        BUILD
        TRADE
        THEFT
        BANK
        DEV_CARD
        COST
        START
    }

    class GameErrorCode {
        <<enum>>
        NOT_YOUR_TURN
        INSUFFICIENT_RESOURCES
        INVALID_PLACEMENT
        INVALID_TRADE
        UNKNOWN_BUILDING
        UNKNOW_ACTION
        GAME_ENDED
    }

    GameConfigLoader ..> GameConfig : creates
    GameConfig --> ResourceDef
    GameConfig --> BuildingDef
    GameConfig --> PortDef
    GameConfig --> HexCoord
    GameConfig --> FixedTile
    BuildingDef --> PlacementType
    Board --> HexTile
    Board --> PlacedBuilding
    Board --> HexCoord
    Board --> PortDef
    Board --> BuildingDef
    Board --> ResourceDef
    PlacedBuilding --> BuildingDef
    HexTile --> HexCoord
    GamePlayer --> PortDef
    GamePlayer ..> PlayerSnapshot : creates
    BuildingSnapshot --> HexCoord
```

---

## 6. WebSocket Message Protocol

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
        <<sealed · client→server>>
    }

    class LobbyEvent {
        <<sealed · server→client>>
    }

    class GameplayIntent {
        <<sealed · client→server>>
    }

    class GameplayEvent {
        <<sealed · server→client>>
    }

    class LeaveLobby { <<object>> }
    class RequestStartGame { <<object>> }
    class ReadyForGame { <<object>> }
    class ToggleReady { +isReady: Boolean }
    class ChangeColor { +newColor: String }

    class LobbySnapshot {
        +lobbyId: String
        +lobbyPlayers: List~LobbyPlayer~
        +maxPlayers: Int
        +availableColors: List~String~
    }
    class LobbyPlayerJoined { +lobbyPlayer: LobbyPlayer }
    class LobbyPlayerLeft { +playerId: String }
    class LobbyPlayerUpdated { +lobbyPlayer: LobbyPlayer }
    class GameStarted { <<object>> }
    class LobbyError {
        +errorMessage: String
        +code: LobbyErrorCode
    }

    class EndTurn { <<object>> }
    class Build {
        +buildingId: BuildingId
        +h1: HexCoord
        +h2: HexCoord
        +h3: HexCoord?
    }
    class MoveRobber { +hexA: HexCoord }
    class ProposeTrade {
        +give: Map~ResourceId, Int~
        +want: Map~ResourceId, Int~
    }
    class RespondToTrade {
        +offererId: PlayerId
        +accepted: Boolean
    }
    class ConfirmTrade { +responderId: PlayerId }
    class CancelTrade { +offererId: PlayerId }
    class ExchangeWithBank {
        +give: Map~ResourceId, Int~
        +get: Map~ResourceId, Int~
    }

    class GameConfigLoaded { +config: GameConfig }
    class GamePlayerStats { +player: GamePlayer }
    class GameSnapshot {
        +boardState: List~BuildingSnapshot~
        +players: List~PlayerSnapshot~
        +currentTurnPlayerId: String
        +diceResult: List~Int~?
        +robberLocation: HexCoord?
    }
    class GameplayPlayerJoined { +player: PlayerSnapshot }
    class RobberUpdated { +location: HexCoord }
    class ResourcesUpdated {
        +changes: Map~ResourceId, Int~
        +reason: UpdateReason
    }
    class ResourceCountUpdated {
        +playerId: PlayerId
        +changes: Int
        +reason: UpdateReason
    }
    class ObjectBuilt {
        +playerId: PlayerId
        +buildingId: BuildingId
        +hexA: HexCoord
        +hexB: HexCoord
        +hexC: HexCoord?
    }
    class DiceRolled { +values: Pair~Int, Int~ }
    class TurnChanged {
        +turnPhase: TurnPhase
        +newPlayerId: PlayerId
    }
    class TradeProposed {
        +give: Map~ResourceId, Int~
        +want: Map~ResourceId, Int~
        +offererId: PlayerId
    }
    class TradeResponse {
        +offererId: PlayerId
        +responderId: PlayerId
        +accepted: Boolean
    }
    class TradeCompleted {
        +responderId: PlayerId
        +offererId: PlayerId
    }
    class TradeCancelled { +offererId: PlayerId }
    class GameError {
        +message: String
        +code: GameErrorCode
    }
    class GameEnded {
        +winnerId: String?
        +gameId: String
    }

    GameMessage <|-- LobbyMessage
    GameMessage <|-- GameplayMessage
    LobbyMessage <|-- LobbyIntent
    LobbyMessage <|-- LobbyEvent
    GameplayMessage <|-- GameplayIntent
    GameplayMessage <|-- GameplayEvent

    LobbyIntent <|-- LeaveLobby
    LobbyIntent <|-- ToggleReady
    LobbyIntent <|-- ChangeColor
    LobbyIntent <|-- RequestStartGame
    LobbyIntent <|-- ReadyForGame

    LobbyEvent <|-- LobbySnapshot
    LobbyEvent <|-- LobbyPlayerJoined
    LobbyEvent <|-- LobbyPlayerLeft
    LobbyEvent <|-- LobbyPlayerUpdated
    LobbyEvent <|-- GameStarted
    LobbyEvent <|-- LobbyError

    GameplayIntent <|-- EndTurn
    GameplayIntent <|-- Build
    GameplayIntent <|-- MoveRobber
    GameplayIntent <|-- ProposeTrade
    GameplayIntent <|-- RespondToTrade
    GameplayIntent <|-- ConfirmTrade
    GameplayIntent <|-- CancelTrade
    GameplayIntent <|-- ExchangeWithBank

    GameplayEvent <|-- GameConfigLoaded
    GameplayEvent <|-- GameplayPlayerJoined
    GameplayEvent <|-- GamePlayerStats
    GameplayEvent <|-- GameSnapshot
    GameplayEvent <|-- RobberUpdated
    GameplayEvent <|-- ResourcesUpdated
    GameplayEvent <|-- ResourceCountUpdated
    GameplayEvent <|-- ObjectBuilt
    GameplayEvent <|-- DiceRolled
    GameplayEvent <|-- TurnChanged
    GameplayEvent <|-- TradeProposed
    GameplayEvent <|-- TradeResponse
    GameplayEvent <|-- TradeCompleted
    GameplayEvent <|-- TradeCancelled
    GameplayEvent <|-- GameError
    GameplayEvent <|-- GameEnded
```
