# Hexon

A multiplayer Catan-like board game built with Kotlin Multiplatform. Runs on Android, iOS, Desktop (JVM), and Web, with a shared Ktor game server.

## Architecture

```
HexonKMP/
├── core/          Pure game engine — board, rules, actions, events (shared across all targets)
├── server/        Ktor WebSocket server — lobbies, matchmaking, game sessions
└── app/
    ├── shared/    Compose Multiplatform UI — screens, components, ViewModels
    ├── androidApp Android entry point
    ├── desktopApp Desktop (JVM) entry point
    ├── webApp     Kotlin/JS browser entry point
    └── iosApp     iOS / SwiftUI entry point
```

The game logic lives entirely in `:core` as a pure function `reduce(state, action) → (state, events)`. The server and all clients share this module — no logic is duplicated.

## Running locally

Copy `.env.example` to `.env` and adjust if needed, then generate the platform configs:

```bash
./gradlew generateEnvConfig
```

| Target | Command |
|---|---|
| Android | `./gradlew :app:androidApp:assembleDebug` |
| Desktop | `./gradlew :app:desktopApp:run` |
| Web (dev) | `./gradlew :app:webApp:jsBrowserDevelopmentRun` |
| Server | `./gradlew :server:run` |
| iOS | Open `app/iosApp` in Xcode |

## Deployment

### Server (Docker)

```bash
docker compose up -d --build
```

The server binds on `0.0.0.0:8080` by default. Override via environment variables:

```
SERVER_BIND_HOST=0.0.0.0
SERVER_BIND_PORT=8080
```

### Web (GitHub Pages)

Pushed automatically on every merge to `main` via [.github/workflows/deploy-web.yml](.github/workflows/deploy-web.yml). The JS bundle connects to `https://hexon-api.biri.es` (baked in at build time; the host must be a single-label subdomain so Cloudflare's `*.biri.es` cert covers it).

### Deploy script

```bash
DEPLOY_HOST=<host> DEPLOY_USER=<user> ./deploy.sh
```

SSHes into `$DEPLOY_HOST`, pulls the latest `main`, and runs `docker compose up -d --build`.

## Environment

| Variable | Side | Description |
|---|---|---|
| `SERVER_HOST` | Client | Host to connect to (compiled into the app) |
| `SERVER_PORT` | Client | Port to connect to (compiled into the app) |
| `SERVER_BIND_HOST` | Server | Interface Ktor binds on |
| `SERVER_BIND_PORT` | Server | Port Ktor binds on |

For the web target, `SERVER_HOST` defaults to `window.location.hostname` in dev mode (when set to `localhost`). Set it to an explicit value (e.g. `hexon-api.biri.es`) for production builds.
