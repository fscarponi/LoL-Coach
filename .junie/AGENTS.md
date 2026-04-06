# Project Guidelines for Junie

## Project Overview

**LoL Support Strategist** is a real-time desktop coaching app for League of Legends, focused on the Support role. It connects to the local game client APIs (LCU API + Live Client Data API), analyzes game state, and provides strategic suggestions via visual notifications and TTS.

## Architecture

Multi-module Gradle project (Kotlin DSL + Version Catalog):

- **`bridge/`** — I/O layer: communication with Riot's local APIs (LCU WebSocket + Live Client Data HTTP polling). No game logic here.
- **`brain/`** — Decision logic: state machine, event processing, strategy engine. No network/I/O dependencies.
- **`app/`** — Presentation: Dashboard UI, Overlay, TTS (Compose Desktop + Material 3).

### Data Flow

```
LoL Client → bridge (LCU WebSocket + Live Client polling)
           → brain (GameStateMachine + EventProcessor + Strategies)
           → app (Dashboard + Overlay + TTS)
```

Key reactive types: `StateFlow<GameState>`, `SharedFlow<ChampSelectSession>`, `SharedFlow<GameSnapshot>`, `SharedFlow<GameEvent>`.

## Tech Stack

- **Kotlin 2.1.20**, **Gradle 8.11** (Kotlin DSL + `gradle/libs.versions.toml`)
- **Ktor Client** (CIO engine + WebSocket) for networking
- **Compose Multiplatform Desktop** + Material 3 for UI
- **Kotlin Coroutines & Flow** for concurrency
- **kotlinx.serialization** (JSON) for data models
- **JDK 21+** required

## Build & Run

```bash
./gradlew build              # Build all modules
./gradlew :app:run           # Run the application
./gradlew test               # Run all tests
./gradlew :bridge:test       # Test bridge module only
./gradlew :brain:test        # Test brain module only
```

## Testing

Tests exist for critical components in `bridge/` and `brain/` modules:

- **bridge**: `LockfileReaderTest`, `ChampSelectDeserializationTest`, `GameSnapshotDeserializationTest`
- **brain**: `GameStateMachineTest`, `EarlyGameStrategyTest`, `VisionMacroStrategyTest`, `ChampSelectStrategyTest`

### Testing Guidelines

- Always run `./gradlew test` to verify changes don't break existing tests.
- When modifying `bridge/` or `brain/`, run the respective module tests.
- The `app/` module has no tests currently; verify UI changes by building (`./gradlew :app:build`).

## Code Style

- **Language**: Kotlin throughout. Comments and documentation in the codebase are in Italian.
- **Coroutines**: Use `StateFlow`/`SharedFlow` for shared state (thread-safe by design).
- **Module boundaries**: `bridge` handles only I/O, `brain` contains only pure logic (no network dependencies), `app` handles presentation.
- **Strategy pattern**: New strategies implement the `Strategy` interface and register in `StrategyEngine`.
- **Events**: New event types are added to the `GameEvent` sealed class with a deduplication key in `EventProcessor`.

## Key Design Constraints

- Polling ≤1Hz for Live Client Data API.
- Overlay is non-intrusive: `focusable=false`, semi-transparent.
- Graceful degradation if LoL client is unavailable.
- Retry with exponential backoff for connections.

## Git Policy
- Agents MUST NOT commit or push changes automatically. All git operations must be explicitly requested by the user.
