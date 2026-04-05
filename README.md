# LoL Support Strategist

Un coach in tempo reale per League of Legends, focalizzato sul ruolo del Support.

## Architettura

Il progetto è suddiviso in 3 moduli:

```
LoL-Coach/
├── bridge/    # I/O e comunicazione con le API Riot (LCU + Live Client Data)
├── brain/     # Logica decisionale, state machine, strategie
└── app/       # UI Overlay (Compose Desktop) + TTS
```

### Flusso Dati

```
[LoL Client] → Bridge (Lockfile + Ktor) → Brain (StateMachine + Strategies) → App (Overlay UI + TTS)
```

## Stack Tecnologico

- **Linguaggio**: Kotlin 2.1.20
- **Networking**: Ktor Client (CIO engine)
- **UI**: Compose Multiplatform Desktop
- **Concorrenza**: Kotlin Coroutines & Flow
- **Serializzazione**: kotlinx.serialization (JSON)
- **TTS**: System API (`say` su macOS, `espeak` su Linux) o Piper (processo esterno)

## Moduli

### Bridge Module
- **LockfileReader**: Legge il file `lockfile` di LoL per estrarre porta e token.
- **LockfileMonitor**: Polling coroutine-based (ogni 2s) che emette `StateFlow<LockfileData?>`.
- **KtorClientFactory**: Factory per client HTTP con bypass certificati SSL self-signed e Basic Auth.
- **LcuWebSocketClient**: WebSocket verso la LCU API per eventi Champion Select.
- **LiveClientPoller**: Polling ≤1Hz sulla porta 2999 per i dati in-game.
- **BridgeFacade**: Orchestratore che espone i Flow ai moduli consumer.

### Brain Module
- **GameStateMachine**: Gestione stati (Idle → ChampSelect → Loading → InGame → PostGame).
- **EventProcessor**: Riceve dati dal Bridge, applica le strategie, genera `GameEvent` con deduplicazione.
- **Strategy Engine**: Sistema estensibile di strategie:
  - `EarlyGameStrategy`: Monitoraggio livello 2 e timing minion wave.
  - `VisionMacroStrategy`: Suggerimenti ward, control ward e oracle lens.
  - `ChampSelectStrategy`: Analisi sinergie ADC-Support e detection support nemico.

### App Module
- **Overlay UI**: Finestra trasparente `alwaysOnTop` con notifiche colorate per tipo di evento.
- **StatusBar**: Indicatore stato connessione e fase di gioco.
- **TTS**: Sistema text-to-speech con coda asincrona (SystemTTS o Piper).

## Requisiti

- JDK 21+
- League of Legends installato (per le API locali)

## Build & Run

```bash
# Build
./gradlew build

# Esegui
./gradlew :app:run

# Test
./gradlew test

# Distribuzione nativa
./gradlew :app:packageDmg    # macOS
./gradlew :app:packageMsi    # Windows
./gradlew :app:packageDeb    # Linux
```

## Vincoli di Sicurezza e Performance

- Il polling non supera 1Hz (1 richiesta/secondo) per non impattare le performance del gioco.
- Separazione netta tra lettura dati (Bridge) e logica decisionale (Brain).
- Codice thread-safe tramite `StateFlow`/`SharedFlow` (inherently thread-safe).
- Retry con exponential backoff per le connessioni LCU/Live Client.
- Graceful degradation se il client LoL non è disponibile.

## Estensibilità

Per aggiungere una nuova strategia:

1. Implementare l'interfaccia `Strategy`:
```kotlin
class MyStrategy : Strategy {
    override fun evaluate(snapshot: GameSnapshot, state: GameState): List<GameEvent> {
        // Logica personalizzata
        return listOf(GameEvent.GenericTip("Il mio consiglio"))
    }
}
```

2. Registrarla nel `StrategyEngine`:
```kotlin
strategyEngine.addStrategy(MyStrategy())
```
