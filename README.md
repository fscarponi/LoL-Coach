# LoL Support Strategist

A real-time desktop coach for **League of Legends**, focused on the **Support** role.  
The application connects to the game client's local APIs (LCU API + Live Client Data API), analyzes the game state, and provides strategic suggestions via visual and vocal notifications (TTS).

---

## Architecture

The project follows a **multi-module** architecture with a clear separation between I/O, decision logic, and presentation:

```
LoL-Coach/
├── bridge/          # I/O: communication with Riot's local APIs (LCU + Live Client Data)
├── brain/           # Logic: state machine, event processing, game strategies
├── app/             # Presentation: Dashboard UI, Overlay, TTS (Compose Desktop)
├── gradle/
│   ├── libs.versions.toml   # Centralized version catalog
│   └── wrapper/
├── build.gradle.kts          # Root build script
├── settings.gradle.kts       # Multi-module configuration
└── gradle.properties
```

### Data Flow

```
                    ┌─────────────────────────────────────────────────────┐
                    │                    LoL Client                       │
                    └──────┬────────────────────────┬─────────────────────┘
                           │                        │
                   LCU API (WebSocket)    Live Client Data API (HTTP polling)
                   dynamic port           port 2999
                           │                        │
                    ┌──────▼────────────────────────▼─────────────────────┐
                    │                  BRIDGE MODULE                       │
                    │  LockfileMonitor → KtorClientFactory                │
                    │  LcuWebSocketClient    LiveClientPoller             │
                    │  BridgeFacade (orchestrator)                        │
                    └──────┬────────────────────────┬─────────────────────┘
                           │ SharedFlow             │ SharedFlow
                           │ <ChampSelectSession>   │ <GameSnapshot>
                    ┌──────▼────────────────────────▼─────────────────────┐
                    │                   BRAIN MODULE                       │
                    │  GameStateMachine (StateFlow<GameState>)            │
                    │  EventProcessor + Strategy Engine                    │
                    │  → EarlyGameStrategy                                │
                    │  → VisionMacroStrategy                              │
                    │  → ChampSelectStrategy                              │
                    └──────┬──────────────────────────────────────────────┘
                           │ SharedFlow<GameEvent>
                    ┌──────▼──────────────────────────────────────────────┐
                    │                    APP MODULE                        │
                    │  Dashboard Window (main informative window)          │
                    │  Overlay Window (compact, semi-transparent)          │
                    │  TTS Engine (asynchronous voice notifications)       │
                    │  AppLogger (centralized log with levels)             │
                    └─────────────────────────────────────────────────────┘
```

---

## Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Language | Kotlin | 2.1.20 |
| Build System | Gradle (Kotlin DSL + Version Catalog) | 8.11 |
| Networking | Ktor Client (CIO engine + WebSocket) | 3.1.1 |
| UI | Compose Multiplatform Desktop + Material 3 | 1.7.3 |
| Concurrency | Kotlin Coroutines & Flow | 1.10.1 |
| Serialization | kotlinx.serialization (JSON) | 1.8.0 |
| TTS | System API (`say` macOS / `espeak` Linux) or Piper | — |

---

## Detailed Modules

### Bridge Module (`bridge/`)

Responsible exclusively for I/O with Riot's local APIs. No decision logic.

| Class | Responsibility |
|-------|----------------|
| `LockfileReader` | Parses LoL's `lockfile` → extracts PID, port, token, protocol (`LockfileData`) |
| `LockfileMonitor` | Coroutine-based polling (every 2s) to detect client open/close → `StateFlow<LockfileData?>` |
| `KtorClientFactory` | Factory for `HttpClient(CIO)` with self-signed SSL bypass, Basic Auth (base64), JSON content negotiation |
| `LcuWebSocketClient` | WebSocket connection to LCU API → subscribes to `OnJsonApiEvent_lol-champ-select_v1_session` → `SharedFlow<ChampSelectSession>` |
| `LiveClientPoller` | HTTP polling ≤1Hz on `https://127.0.0.1:2999/liveclientdata/allgamedata` → `SharedFlow<GameSnapshot>` |
| `RetryUtils` | `retryWithBackoff` utility with configurable exponential backoff (initial delay, max delay, max retries) |
| `BridgeFacade` | Orchestrator: manages the lifecycle of all components, exposes Flow to the consumer module |

**Data Models** (`bridge/model/`):
- **LCU API**: `ChampSelectSession`, `ChampSelectPlayerSelection`, `ChampSelectAction`
- **Live Client Data**: `GameSnapshot`, `ActivePlayer`, `ChampionStats`, `Abilities`, `FullRunes`, `Player`, `PlayerItem`, `PlayerScores`, `GameData`, `GameEvents`
- **Internal**: `LockfileData`

### Brain Module (`brain/`)

Pure decision logic. No dependencies on network or I/O.

| Class | Responsibility |
|-------|----------------|
| `GameStateMachine` | State machine: `Idle` → `ChampSelect` → `Loading` → `InGame` → `PostGame` → `Idle`. Exposes `StateFlow<GameState>` |
| `EventProcessor` | Receives snapshots from Bridge, applies strategies, generates `GameEvent` with **deduplication** (avoids repeated notifications) |
| `Strategy` (interface) | Strategy contract: `evaluate(snapshot, state)` and `evaluateChampSelect(session, state)` |
| `StrategyEngine` | Extensible registry of strategies. By default includes the 3 built-in strategies |

**Built-in Strategies**:

| Strategy | Logic |
|----------|-------|
| `EarlyGameStrategy` | Calculates minions needed for level 2 (first wave = 6 melee + 3 caster; level 2 at ~7 minions). Warns when threshold is near |
| `VisionMacroStrategy` | If `gameTime > 10min` and inventory contains ≥3 ward charges, suggests warding. Monitors Control Ward and Oracle Lens |
| `ChampSelectStrategy` | Analyzes `myTeam`/`theirTeam` via LCU for: enemy support detection, ADC-Support synergy suggestions (hardcoded, extensible table) |

**Generated Events** (`GameEvent` sealed class):
- `EnemySupportSelected` — Enemy support identified in champ select
- `Level2Approaching` — Minion countdown to level 2
- `Level2Reached` — Level 2 reached, trade window
- `VisionNeeded` — Warding suggestion with reason
- `DragonTimerWarning` — Pre-dragon timer to prepare vision
- `ItemSuggestion` — Item suggestion with reason
- `SynergyAdvice` — ADC-Support synergy advice
- `GenericTip` — Generic suggestion

### App Module (`app/`)

User interface and audio output. Two parallel windows + TTS system.

#### Dashboard Window (main window)

Decorated window, resizable (1100×700), with dark theme. Two-column layout:

| Panel (left column)    | Content                                                                        |
|--------------------------|----------------------------------------------------------------------------------|
| `ConnectionPanel`        | Lockfile connection status (port, PID) and Live Client Data (active/inactive)    |
| `GameStatePanel`         | Current game phase with colored indicator                                        |
| `GameInfoPanel`          | Active champion stats: level, HP, gold, CS                                       |
| `PlayersPanel`           | Allies and enemies list with champion, KDA, level                                |

| Panel (right column)    | Content                                                                        |
|--------------------------|----------------------------------------------------------------------------------|
| `EventsPanel`            | Strategy event history with timestamp, scrollable                                |
| `LogPanel`               | Real-time application logs with level filter chips (DEBUG, INFO, WARN, ERROR, EVENT) |

#### Overlay Window (compact window)

- **Semi-transparent** (`alpha = 0.85`) with visible borders
- `alwaysOnTop = true` — stays on top of the game
- `focusable = false` — does not capture clicks or focus, avoids involuntary misclicks
- `undecorated = true`, `transparent = true`
- Shows the last 5 events with automatic fade-out (8 seconds)
- Colored notifications by event type (orange for combat, green for achievement, red for enemies, blue for vision, etc.)

#### TTS System

| Class               | Description                                                                            |
|----------------------|----------------------------------------------------------------------------------------|
| `TtsManager`         | Common interface for TTS engines                                                       |
| `SystemTtsManager`   | Uses `ProcessBuilder` → `say` (macOS) or `espeak` (Linux)                               |
| `PiperTtsManager`    | Invokes the Piper binary as an external process with stdin/stdout pipe                    |
| `TtsEventListener`   | Subscribes to `SharedFlow<GameEvent>`, converts to text, executes TTS with async queue on `Dispatchers.IO` |

#### Logging System

| Class      | Description                                                                                        |
|-------------|----------------------------------------------------------------------------------------------------|
| `AppLogger` | Centralized singleton with `StateFlow<List<LogEntry>>`. 5 levels (DEBUG, INFO, WARN, ERROR, EVENT). Circular buffer of max 500 entries |

---

## Requirements

- **JDK 21+**
- **League of Legends** installed and running (for local APIs)
- **macOS / Windows / Linux** (Compose Desktop is cross-platform)

### Riot APIs Used

| API                     | Endpoint                                              | Auth         | Use                        |
|-------------------------|-------------------------------------------------------|--------------|----------------------------|
| LCU API (WebSocket)     | `wss://127.0.0.1:{port}/`                             | Basic Auth   | Champion Select events     |
| Live Client Data API    | `https://127.0.0.1:2999/liveclientdata/allgamedata`   | None         | Real-time in-game data     |

> **Note**: both APIs use self-signed SSL certificates from Riot. The Ktor client is configured to accept them.

---

### LLM Module (`brain/llm/`)

Advanced analysis using Large Language Models (LLM) for champion select and game loading.

| Class | Responsibility |
|-------|----------------|
| `LlmCoachService` | Orchestrates LLM requests, parses structured responses into `GameEvent.LlmAnalysis` sections. |
| `LlmProvider` | Abstract interface for LLM backends. |
| `OpenAiCompatibleProvider` | Provider for OpenAI, Ollama, LM Studio, etc. |
| `GeminiProvider` | Dedicated provider for Google Gemini API (1.5 Flash/Pro). |
| `RequestBuilder` | Builds structured JSON prompts from `ChampSelectSession` and `GameSnapshot`. |
| `PromptLoader` | Loads specialized System Prompts from `.md` files in resources for different game modes. |

---

## Configuration

The application can be configured via environment variables, especially for LLM integration:

| Variable | Description | Default |
|----------|-------------|---------|
| `LLM_ENABLED` | Enable or disable LLM analysis | `true` |
| `LLM_PROVIDER` | `GEMINI` or `OPENAI_COMPATIBLE` | `OPENAI_COMPATIBLE` |
| `GEMINI_API_KEY` | API Key for Google AI Studio | — |
| `OPENAI_API_KEY` | API Key for OpenAI | — |
| `LLM_MODEL` | Model name (e.g. `gemini-1.5-flash`, `gpt-4o-mini`, `llama3`) | based on provider |
| `LLM_BASE_URL` | Base URL for OpenAI compatible APIs | `http://localhost:11434/v1` (Ollama) |
| `LLM_TEMPERATURE` | Generation temperature (0.0 to 1.0/2.0) | `0.7` |
| `LLM_MAX_TOKENS` | Maximum tokens in the response | `1024` |

---

## Build & Run

```bash
# Full build (all modules)
./gradlew build

# Run the application
./gradlew :app:run

# Run all tests
./gradlew test

# Test a single module
./gradlew :bridge:test
./gradlew :brain:test

# Native distribution
./gradlew :app:packageDmg    # macOS (.dmg)
./gradlew :app:packageMsi    # Windows (.msi)
./gradlew :app:packageDeb    # Linux (.deb)
```

---

## Test

The project includes unit tests for critical components:

| Module   | Test                              | Coverage                                                   |
|----------|-----------------------------------|------------------------------------------------------------|
| `bridge` | `LockfileReaderTest`              | Lockfile parsing: valid format, extracted fields           |
| `bridge` | `ChampSelectDeserializationTest`  | LCU JSON deserialization → `ChampSelectSession`            |
| `bridge` | `GameSnapshotDeserializationTest` | Live Client JSON deserialization → `GameSnapshot`          |
| `brain`  | `GameStateMachineTest`            | State transitions: all paths and edge cases (9 tests)      |
| `brain`  | `EarlyGameStrategyTest`           | Level 2 minion calculation, threshold, events generated    |
| `brain`  | `VisionMacroStrategyTest`         | Ward logic, timing, Oracle Lens                            |
| `brain`  | `ChampSelectStrategyTest`         | Enemy support detection, ADC-Support synergies             |

---

## Security and Performance Constraints

- **Polling ≤1Hz**: maximum 1 request/second to the Live Client Data API to avoid impacting game performance.
- **I/O / Logic Separation**: the `bridge` module contains no decision logic; the `brain` has no network dependencies.
- **Thread Safety**: all shared states use `StateFlow`/`SharedFlow` (inherently thread-safe).
- **Retry with Exponential Backoff**: for LCU and Live Client Data connections.
- **Graceful Degradation**: if the LoL client is unavailable or the lockfile disappears mid-game, the app continues without crashing.
- **Non-Intrusive Overlay**: `focusable=false` + semi-transparency to avoid interfering with the game.

---

## Extensibility

### Adding a new Strategy

1. Implement the `Strategy` interface:

```kotlin
class MyCustomStrategy : Strategy {
    override fun evaluate(snapshot: GameSnapshot, state: GameState): List<GameEvent> {
        // Custom in-game logic
        if (snapshot.gameData?.gameTime ?: 0.0 > 1200.0) {
            return listOf(GameEvent.GenericTip("Baron soon, prepare vision!"))
        }
        return emptyList()
    }

    override fun evaluateChampSelect(session: ChampSelectSession, state: GameState): List<GameEvent> {
        // Custom champ select logic (optional)
        return emptyList()
    }
}
```

2. Register it in the `StrategyEngine`:

```kotlin
strategyEngine.addStrategy(MyCustomStrategy())
```

### Adding a new GameEvent

1. Add a new case to the `GameEvent` sealed class in `brain/event/GameEvent.kt`.
2. Add the deduplication key in `EventProcessor.deduplicationKey()`.
3. (Optional) Add color and icon in `OverlayPanel.EventCard()`.

### Adding a new TTS Engine

Implement the `TtsManager` interface:

```kotlin
class MyTtsEngine : TtsManager {
    override suspend fun speak(text: String) {
        // Custom implementation
    }
    override fun stop() { }
}
```

---

## License

Personal project. Not affiliated with Riot Games.  
League of Legends and all related assets are property of Riot Games, Inc.
