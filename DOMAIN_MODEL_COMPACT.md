# Hexon Domain Model (Compact)

Single-diagram overview of the Hexon server and shared KMP module — every domain on one page. For grouped per-domain diagrams with method bundles, see [DOMAIN_MODEL.md](DOMAIN_MODEL.md).

```mermaid
classDiagram
    direction TB

    %% ── Application & Routing ─────────────────────────────────────────────
    namespace Application {
        class Application { <<Ktor>> module, configure* }
        class AppModule { <<Koin DI>> appModule() }
        class DatabaseFactory { <<object>> init, dbQuery }
        class Configs { <<data>> Jwt, Smtp, Cookie }
        class AuthRoutes { <<REST>> /auth/* }
        class UsersRoutes { <<REST>> /users/me, /{id}, /email/*, /password/*, /me/delete/* }
        class SocialRoutes { <<REST>> /friends, /requests, /add, /respond }
        class MatchmakingRoutes { <<REST + WS>> POST /game, /lobby · WS /game/{id} }
    }

    %% ── Identity & Account ────────────────────────────────────────────────
    namespace Identity {
        class User { <<entity>> id, email, username, isVerified }
        class TokenService { <<interface>> generate / validate tokens }
        class AuthRepository { <<interface>> users, refresh tokens, deletion }
        class LoginService { <<interface>> login }
        class RegisterService { <<interface>> register }
        class RefreshService { <<interface>> refresh }
        class LogoutService { <<interface>> logout }
        class AccountVerificationService { <<interface>> verifyEmail, resendCode }
        class UserAccountService { <<interface>> password & deletion flows }
    }

    %% ── Email, Profile & Social ──────────────────────────────────────────
    namespace Communication {
        class SmtpService { <<interface>> sendEmail }
        class EmailVerificationRepository { <<interface>> store / fetch codes }
        class EmailVerificationService { <<interface>> sendCode, verifyCode }
        class EmailVerificationType { <<enum>> CONFIRM, RESET, DELETE }
        class UserProfile { <<entity>> userId, username, stats }
        class ProfileRepository { <<interface>> create, get, updateStats }
        class UserProfileService { <<interface>> getMy / Public Profile }
        class Friend { <<entity>> id, username, isOnline }
        class FriendsRepository { <<interface>> friendship CRUD }
        class FriendRequestRepository { <<interface>> request CRUD }
        class SocialService { <<interface>> getFriends, sendRequest, respond }
    }

    %% ── Matchmaking & Sessions ───────────────────────────────────────────
    namespace Matchmaking {
        class GameMode { <<enum>> CLASSIC }
        class LobbyPlayer { <<entity>> id, name, color, isReady, isHost }
        class MatchmakingService { <<interface>> findGameForPlayer }
        class LobbyService { <<interface>> createCustomGame, invitePlayer }
        class GameSessionRepository { <<interface>> add, get, findAvailable }
        class SessionLifecycleListener { <<interface>> onStarting/Ended/Empty }
        class GameMessageSender { <<interface>> sendToPlayer, broadcast }
        class BaseGameSession { <<interface>> connect, remove, handleMessage }
        class MatchmakingGameSession { <<state>> WAITING→RUNNING→DISPOSED }
        class CustomLobbySession { <<state>> LOBBY⇄GAME_RUNNING }
        class GameEngine { <<interface>> start, onMessage, endGame }
    }

    %% ── Shared · Board & Configuration ───────────────────────────────────
    namespace SharedBoard {
        class HexCoord { <<value>> q, r · id helpers }
        class GameConfig { <<data>> seed, rules, defs, layout }
        class GameConfigLoader { <<object>> default(seed), grid, pools }
        class ResourceDef { id, name }
        class BuildingDef { id, type, cost, production, points }
        class PlacementType { <<enum>> EDGE, VERTEX }
        class PortDef { hexes, resource, ratio }
        class HexTile { coord, resource, number }
        class PlacedBuilding { ownerId, def }
        class Board { tiles, buildings, ports, robber · place, canPlace, production, moveRobber }
    }

    %% ── Shared · Player & Turn Model ─────────────────────────────────────
    namespace SharedPlayer {
        class GamePlayer { resources, ports, devCards, VP · trade & cost helpers }
        class PlayerSnapshot { <<dto>> public stats }
        class BuildingSnapshot { <<dto>> ownerId, typeId, hexes }
        class TradeOffer { give, want }
        class TurnPhase { <<enum>> SETUP, TRADE, MAIN, ROBBER, GAME_OVER }
        class UpdateReason { <<enum>> PRODUCTION, BUILD, TRADE, THEFT, BANK, ... }
        class GameErrorCode { <<enum>> NOT_YOUR_TURN, INVALID_*, GAME_ENDED }
    }

    %% ── Shared · WebSocket Protocol ──────────────────────────────────────
    namespace Protocol {
        class GameMessage { <<sealed>> }
        class LobbyIntent { <<client→server>> Leave, ToggleReady, ChangeColor, Start, Ready }
        class LobbyEvent { <<server→client>> Snapshot, Joined/Left/Updated, Started, Error }
        class GameplayIntent { <<client→server>> EndTurn, Build, MoveRobber, Trade*, Bank }
        class GameplayEvent { <<server→client>> Snapshot, Dice, Turn, Built, Resources*, Trade*, Error, Ended }
    }

    %% ── Routing → Services ────────────────────────────────────────────────
    Application --> AppModule
    Application --> DatabaseFactory
    AppModule --> Configs
    AuthRoutes ..> LoginService
    AuthRoutes ..> RegisterService
    AuthRoutes ..> RefreshService
    AuthRoutes ..> LogoutService
    UsersRoutes ..> AccountVerificationService
    UsersRoutes ..> UserAccountService
    UsersRoutes ..> UserProfileService
    SocialRoutes ..> SocialService
    MatchmakingRoutes ..> MatchmakingService
    MatchmakingRoutes ..> LobbyService
    MatchmakingRoutes ..> BaseGameSession

    %% ── Identity wiring ─────────────────────────────────────────────────
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

    %% ── Communication wiring ────────────────────────────────────────────
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

    %% ── Game session wiring ──────────────────────────────────────────────
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
    BaseGameSession --> LobbyPlayer
    BaseGameSession --> GameMode
    GameEngine ..> GameMessageSender

    %% ── Engine ↔ Shared model ────────────────────────────────────────────
    GameEngine --> Board
    GameEngine --> GamePlayer
    GameEngine --> GameConfig
    GameConfigLoader ..> GameConfig
    GameConfig --> ResourceDef
    GameConfig --> BuildingDef
    GameConfig --> PortDef
    GameConfig --> HexCoord
    BuildingDef --> PlacementType
    Board --> HexTile
    Board --> PlacedBuilding
    Board --> PortDef
    HexTile --> HexCoord
    PlacedBuilding --> BuildingDef
    GamePlayer ..> PlayerSnapshot
    GamePlayer --> TradeOffer

    %% ── Protocol carried over WebSockets ─────────────────────────────────
    BaseGameSession ..> GameMessage : exchanges
    GameMessage <|-- LobbyIntent
    GameMessage <|-- LobbyEvent
    GameMessage <|-- GameplayIntent
    GameMessage <|-- GameplayEvent
```

## Key Architectural Patterns

- **Layered separation** — Routes (HTTP/WS) → Services (business logic) → Repositories (Exposed ORM or in-memory). Every capability is exposed as an interface; concrete `*Impl`, `Exposed*`, `InMemory*` classes are wired through Koin.
- **In-memory game state** — Active sessions live in `InMemoryGameSessionRepository` for low-latency turn handling; only durable user data hits the database.
- **Sealed messaging protocol** — All client/server traffic uses sealed `GameMessage` hierarchies (`LobbyIntent/Event`, `GameplayIntent/Event`), giving compile-time exhaustiveness on both server and KMP clients.
- **Shared domain core** — `Board`, `GamePlayer`, `GameConfig` and all DTOs live in the shared module, so server and clients agree on rules, geometry, and wire format by construction.
- **Two session shapes, one contract** — `MatchmakingGameSession` (auto-queue) and `CustomLobbySession` (host-driven) both implement `BaseGameSession` and `GameMessageSender`, letting the `GameEngine` stay session-agnostic.
