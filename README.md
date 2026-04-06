# LoL Support Strategist

Un coach desktop in tempo reale per **League of Legends**, focalizzato sul ruolo del **Support**.  
L'applicazione si connette alle API locali del client di gioco (LCU API + Live Client Data API), analizza lo stato della partita e fornisce suggerimenti strategici tramite notifiche visive e vocali (TTS).

---

## Architettura

Il progetto segue un'architettura **multi-modulo** con separazione netta tra I/O, logica decisionale e presentazione:

```
LoL-Coach/
├── bridge/          # I/O: comunicazione con le API locali di Riot (LCU + Live Client Data)
├── brain/           # Logica: state machine, event processing, strategie di gioco
├── app/             # Presentazione: Dashboard UI, Overlay, TTS (Compose Desktop)
├── gradle/
│   ├── libs.versions.toml   # Version catalog centralizzato
│   └── wrapper/
├── build.gradle.kts          # Root build script
├── settings.gradle.kts       # Configurazione multi-modulo
└── gradle.properties
```

### Flusso Dati

```
                    ┌─────────────────────────────────────────────────────┐
                    │                    LoL Client                       │
                    └──────┬────────────────────────┬─────────────────────┘
                           │                        │
                   LCU API (WebSocket)    Live Client Data API (HTTP polling)
                   porta dinamica         porta 2999
                           │                        │
                    ┌──────▼────────────────────────▼─────────────────────┐
                    │                  BRIDGE MODULE                       │
                    │  LockfileMonitor → KtorClientFactory                │
                    │  LcuWebSocketClient    LiveClientPoller             │
                    │  BridgeFacade (orchestratore)                       │
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
                    │  Dashboard Window (finestra principale informativa) │
                    │  Overlay Window (compatta, semi-trasparente)        │
                    │  TTS Engine (notifiche vocali asincrone)            │
                    │  AppLogger (log centralizzato con livelli)          │
                    └─────────────────────────────────────────────────────┘
```

---

## Stack Tecnologico

| Componente       | Tecnologia                                       | Versione  |
|------------------|--------------------------------------------------|-----------|
| Linguaggio       | Kotlin                                           | 2.1.20    |
| Build System     | Gradle (Kotlin DSL + Version Catalog)            | 8.11      |
| Networking       | Ktor Client (CIO engine + WebSocket)             | 3.1.1     |
| UI               | Compose Multiplatform Desktop + Material 3       | 1.7.3     |
| Concorrenza      | Kotlin Coroutines & Flow                         | 1.10.1    |
| Serializzazione  | kotlinx.serialization (JSON)                     | 1.8.0     |
| TTS              | System API (`say` macOS / `espeak` Linux) o Piper| —         |

---

## Moduli in Dettaglio

### Bridge Module (`bridge/`)

Responsabile esclusivamente dell'I/O con le API locali di Riot. Nessuna logica decisionale.

| Classe                   | Responsabilità                                                                                  |
|--------------------------|-------------------------------------------------------------------------------------------------|
| `LockfileReader`         | Parsing del file `lockfile` di LoL → estrae PID, porta, token, protocollo (`LockfileData`)     |
| `LockfileMonitor`        | Polling coroutine-based (ogni 2s) per rilevare apertura/chiusura del client → `StateFlow<LockfileData?>` |
| `KtorClientFactory`      | Factory per `HttpClient(CIO)` con bypass SSL self-signed, Basic Auth (base64), content negotiation JSON |
| `LcuWebSocketClient`     | Connessione WebSocket alla LCU API → subscribe a `OnJsonApiEvent_lol-champ-select_v1_session` → `SharedFlow<ChampSelectSession>` |
| `LiveClientPoller`       | HTTP polling ≤1Hz su `https://127.0.0.1:2999/liveclientdata/allgamedata` → `SharedFlow<GameSnapshot>` |
| `RetryUtils`             | Utility `retryWithBackoff` con exponential backoff configurabile (delay iniziale, max delay, max retries) |
| `BridgeFacade`           | Orchestratore: gestisce il ciclo di vita di tutti i componenti, espone i Flow al modulo consumer |

**Data Models** (`bridge/model/`):
- **LCU API**: `ChampSelectSession`, `ChampSelectPlayerSelection`, `ChampSelectAction`
- **Live Client Data**: `GameSnapshot`, `ActivePlayer`, `ChampionStats`, `Abilities`, `FullRunes`, `Player`, `PlayerItem`, `PlayerScores`, `GameData`, `GameEvents`
- **Interno**: `LockfileData`

### Brain Module (`brain/`)

Logica decisionale pura. Nessuna dipendenza su rete o I/O.

| Classe                   | Responsabilità                                                                                  |
|--------------------------|-------------------------------------------------------------------------------------------------|
| `GameStateMachine`       | Macchina a stati: `Idle` → `ChampSelect` → `Loading` → `InGame` → `PostGame` → `Idle`. Espone `StateFlow<GameState>` |
| `EventProcessor`         | Riceve snapshot dal Bridge, applica le strategie, genera `GameEvent` con **deduplicazione** (evita notifiche ripetute) |
| `Strategy` (interface)   | Contratto per le strategie: `evaluate(snapshot, state)` e `evaluateChampSelect(session, state)` |
| `StrategyEngine`         | Registry estensibile delle strategie. Di default include le 3 strategie built-in               |

**Strategie Built-in**:

| Strategia               | Logica                                                                                          |
|-------------------------|-------------------------------------------------------------------------------------------------|
| `EarlyGameStrategy`     | Calcola i minion necessari per il livello 2 (prima wave = 6 melee + 3 caster; liv 2 a ~7 minion). Avvisa quando il threshold è vicino |
| `VisionMacroStrategy`   | Se `gameTime > 10min` e l'inventario contiene ≥3 cariche ward, suggerisce posizionamento. Monitora Control Ward e Oracle Lens |
| `ChampSelectStrategy`   | Analizza `myTeam`/`theirTeam` via LCU per: detection support nemico, suggerimenti sinergie ADC-Support (tabella hardcoded estendibile) |

**Eventi Generati** (`GameEvent` sealed class):
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
