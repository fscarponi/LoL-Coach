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
- `EnemySupportSelected` — Support nemico identificato in champ select
- `Level2Approaching` — Countdown minion al livello 2
- `Level2Reached` — Livello 2 raggiunto, finestra di trade
- `VisionNeeded` — Suggerimento ward con motivazione
- `DragonTimerWarning` — Timer pre-dragon per preparare visione
- `ItemSuggestion` — Suggerimento item con motivazione
- `SynergyAdvice` — Consiglio sinergia ADC-Support
- `GenericTip` — Suggerimento generico

### App Module (`app/`)

Interfaccia utente e output audio. Due finestre parallele + sistema TTS.

#### Dashboard Window (finestra principale)

Finestra decorata, ridimensionabile (1100×700), con tema dark. Layout a due colonne:

| Pannello (colonna sx)    | Contenuto                                                                        |
|--------------------------|----------------------------------------------------------------------------------|
| `ConnectionPanel`        | Stato connessione lockfile (porta, PID) e Live Client Data (attivo/inattivo)     |
| `GameStatePanel`         | Fase corrente di gioco con indicatore colorato                                   |
| `GameInfoPanel`          | Statistiche champion attivo: livello, HP, gold, CS                               |
| `PlayersPanel`           | Lista alleati e nemici con champion, KDA, livello                                |

| Pannello (colonna dx)    | Contenuto                                                                        |
|--------------------------|----------------------------------------------------------------------------------|
| `EventsPanel`            | Storico eventi strategia con timestamp, scrollabile                              |
| `LogPanel`               | Log applicativi real-time con filtri chip per livello (DEBUG, INFO, WARN, ERROR, EVENT) |

#### Overlay Window (finestra compatta)

- **Semi-trasparente** (`alpha = 0.85`) con contorni visibili
- `alwaysOnTop = true` — resta sopra il gioco
- `focusable = false` — non cattura click né focus, evita misclick involontari
- `undecorated = true`, `transparent = true`
- Mostra gli ultimi 5 eventi con fade-out automatico (8 secondi)
- Notifiche colorate per tipo di evento (arancione per combat, verde per achievement, rosso per nemici, blu per visione, ecc.)

#### Sistema TTS

| Classe               | Descrizione                                                                            |
|----------------------|----------------------------------------------------------------------------------------|
| `TtsManager`         | Interfaccia comune per i motori TTS                                                    |
| `SystemTtsManager`   | Usa `ProcessBuilder` → `say` (macOS) o `espeak` (Linux)                               |
| `PiperTtsManager`    | Invoca il binario Piper come processo esterno con pipe stdin/stdout                    |
| `TtsEventListener`   | Sottoscrive `SharedFlow<GameEvent>`, converte in testo, esegue TTS con coda asincrona su `Dispatchers.IO` |

#### Sistema di Logging

| Classe      | Descrizione                                                                                        |
|-------------|----------------------------------------------------------------------------------------------------|
| `AppLogger` | Singleton centralizzato con `StateFlow<List<LogEntry>>`. 5 livelli (DEBUG, INFO, WARN, ERROR, EVENT). Buffer circolare di max 500 entry |

---

## Requisiti

- **JDK 21+**
- **League of Legends** installato e in esecuzione (per le API locali)
- **macOS / Windows / Linux** (Compose Desktop è cross-platform)

### API Riot Utilizzate

| API                     | Endpoint                                              | Auth         | Uso                        |
|-------------------------|-------------------------------------------------------|--------------|----------------------------|
| LCU API (WebSocket)     | `wss://127.0.0.1:{port}/`                             | Basic Auth   | Champion Select events     |
| Live Client Data API    | `https://127.0.0.1:2999/liveclientdata/allgamedata`   | Nessuna      | Dati in-game real-time     |

> **Nota**: entrambe le API usano certificati SSL self-signed di Riot. Il client Ktor è configurato per accettarli.

---

## Build & Run

```bash
# Build completo (tutti i moduli)
./gradlew build

# Esegui l'applicazione
./gradlew :app:run

# Esegui tutti i test
./gradlew test

# Test di un singolo modulo
./gradlew :bridge:test
./gradlew :brain:test

# Distribuzione nativa
./gradlew :app:packageDmg    # macOS (.dmg)
./gradlew :app:packageMsi    # Windows (.msi)
./gradlew :app:packageDeb    # Linux (.deb)
```

---

## Test

Il progetto include test unitari per i componenti critici:

| Modulo   | Test                              | Copertura                                                  |
|----------|-----------------------------------|------------------------------------------------------------|
| `bridge` | `LockfileReaderTest`              | Parsing del lockfile: formato valido, campi estratti       |
| `bridge` | `ChampSelectDeserializationTest`  | Deserializzazione JSON LCU → `ChampSelectSession`          |
| `bridge` | `GameSnapshotDeserializationTest` | Deserializzazione JSON Live Client → `GameSnapshot`        |
| `brain`  | `GameStateMachineTest`            | Transizioni di stato: tutti i percorsi e edge case (9 test)|
| `brain`  | `EarlyGameStrategyTest`           | Calcolo minion livello 2, threshold, eventi generati       |
| `brain`  | `VisionMacroStrategyTest`         | Logica ward, timing, Oracle Lens                           |
| `brain`  | `ChampSelectStrategyTest`         | Detection support nemico, sinergie ADC-Support             |

---

## Vincoli di Sicurezza e Performance

- **Polling ≤1Hz**: massimo 1 richiesta/secondo verso la Live Client Data API per non impattare le performance di gioco.
- **Separazione I/O / Logica**: il modulo `bridge` non contiene logica decisionale; il `brain` non ha dipendenze su rete.
- **Thread Safety**: tutti gli stati condivisi usano `StateFlow`/`SharedFlow` (inherently thread-safe).
- **Retry con Exponential Backoff**: per le connessioni LCU e Live Client Data.
- **Graceful Degradation**: se il client LoL non è disponibile o il lockfile scompare mid-game, l'app continua senza crash.
- **Overlay Non Intrusivo**: `focusable=false` + semi-trasparenza per evitare interferenze con il gioco.

---

## Estensibilità

### Aggiungere una nuova Strategia

1. Implementare l'interfaccia `Strategy`:

```kotlin
class MyCustomStrategy : Strategy {
    override fun evaluate(snapshot: GameSnapshot, state: GameState): List<GameEvent> {
        // Logica in-game personalizzata
        if (snapshot.gameData?.gameTime ?: 0.0 > 1200.0) {
            return listOf(GameEvent.GenericTip("Baron tra poco, prepara la visione!"))
        }
        return emptyList()
    }

    override fun evaluateChampSelect(session: ChampSelectSession, state: GameState): List<GameEvent> {
        // Logica champ select personalizzata (opzionale)
        return emptyList()
    }
}
```

2. Registrarla nel `StrategyEngine`:

```kotlin
strategyEngine.addStrategy(MyCustomStrategy())
```

### Aggiungere un nuovo GameEvent

1. Aggiungere un nuovo case alla sealed class `GameEvent` in `brain/event/GameEvent.kt`.
2. Aggiungere la chiave di deduplicazione in `EventProcessor.deduplicationKey()`.
3. (Opzionale) Aggiungere colore e icona in `OverlayPanel.EventCard()`.

### Aggiungere un nuovo motore TTS

Implementare l'interfaccia `TtsManager`:

```kotlin
class MyTtsEngine : TtsManager {
    override suspend fun speak(text: String) {
        // Implementazione custom
    }
    override fun stop() { }
}
```

---

## Licenza

Progetto personale. Non affiliato con Riot Games.  
League of Legends e tutti i relativi asset sono proprietà di Riot Games, Inc.
