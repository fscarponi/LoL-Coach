# Analisi Progetto — LoL Support Strategist

**Data**: 6 Aprile 2026  
**Versione analizzata**: 1.0.0  
**Codebase**: ~4530 LOC produzione + ~1045 LOC test (Kotlin)

---

## 1. Panoramica

LoL Support Strategist è un'app desktop di coaching real-time per League of Legends (ruolo Support). Si connette alle API locali del client di gioco (LCU WebSocket + Live Client Data HTTP polling), analizza lo stato della partita e fornisce suggerimenti strategici tramite overlay visivo e TTS.

Il progetto è organizzato in 3 moduli Gradle:

| Modulo | Responsabilità | LOC (prod) | LOC (test) |
|--------|---------------|------------|------------|
| `bridge/` | I/O: comunicazione con API Riot, voice detection | ~900 | ~286 |
| `brain/` | Logica decisionale: state machine, strategie, LLM | ~1800 | ~759 |
| `app/` | Presentazione: UI Compose Desktop, TTS, overlay | ~1800 | 0 |

---

## 2. Punti di Forza (PRO)

### 2.1 Architettura Modulare
- Separazione chiara in 3 layer (I/O → Logica → Presentazione).
- Ogni modulo ha un build Gradle indipendente con dipendenze ben definite.
- Il pattern Strategy è ben implementato per estendere le logiche di gioco.

### 2.2 Stack Tecnologico Moderno
- Kotlin 2.1.20 con coroutine e Flow per concorrenza reattiva.
- Compose Desktop + Material 3 per UI nativa cross-platform.
- kotlinx.serialization per modelli type-safe.
- Ktor Client per networking asincrono.

### 2.3 Integrazione LLM
- Supporto multi-provider (Gemini nativo + OpenAI-compatible).
- System prompt specifici per modalità di gioco (Summoner's Rift, ARAM, ARAM Mayhem).
- `RequestBuilder` strutturato per costruire contesto di gioco per l'LLM.

### 2.4 Resilienza
- Retry con exponential backoff per connessioni (`RetryUtils`).
- Graceful degradation quando il client LoL non è disponibile.
- State machine robusta con transizioni ben definite.

### 2.5 Testing
- Buona copertura per `brain/` (state machine, strategie, LLM service).
- Test di deserializzazione per i modelli `bridge/`.
- Tutti i test passano (BUILD SUCCESSFUL).

### 2.6 Feature Avanzate
- Wake word detection con Vosk per comandi vocali.
- TTS con supporto Piper (locale) e system TTS.
- Supporto multi-modalità: Summoner's Rift e ARAM.

---

## 3. Criticità e Punti Deboli (CONTRO)

### 3.1 🔴 Violazione Architetturale: `brain` dipende da `bridge`
Il modulo `brain` ha una dipendenza diretta su `bridge`:
```kotlin
// brain/build.gradle.kts
implementation(project(":bridge"))
```
Questo viola il principio dichiarato che `brain` dovrebbe contenere "solo logica pura senza dipendenze di rete". `EventProcessor` importa direttamente i modelli di `bridge` (`ChampSelectSession`, `GameSnapshot`), creando un accoppiamento stretto tra logica e I/O.

**Impatto**: Rende difficile testare `brain` in isolamento e impedisce il riuso della logica senza il layer di rete.

### 3.2 🔴 Nessun Test per il Modulo `app/`
Il modulo `app/` (1800 LOC) non ha alcun test:
```
> Task :app:compileTestKotlin NO-SOURCE
```
ViewModel, logica di coordinamento e TTS non sono verificati automaticamente.

### 3.3 🟡 God Object: `Panels.kt` (745 righe)
Il file `app/.../dashboard/Panels.kt` è il più grande del progetto. Contiene probabilmente troppi composable in un singolo file, rendendo difficile la manutenzione e la navigazione.

### 3.4 🟡 Nessun Framework di Dependency Injection
Le dipendenze sono create e collegate manualmente (probabilmente in `Main.kt`). Questo rende il wiring fragile e difficile da testare/sostituire.

### 3.5 🟡 Logging Custom Minimale
`AppLogger` è un'implementazione custom basata su `StateFlow<List<LogEntry>>`. Non supporta output su file, livelli configurabili per modulo, o rotazione dei log. È adeguato per la dashboard UI ma insufficiente per debugging in produzione.

### 3.6 🟡 Gestione Errori LLM
Le chiamate LLM (Gemini/OpenAI) possono fallire per rate limiting, timeout, o risposte malformate. La gestione degli errori potrebbe non coprire tutti i casi edge (token scaduti, risposte troncate, content filtering).

### 3.7 🟢 Assenza di CI/CD
Non sono presenti file di configurazione per pipeline CI (GitHub Actions, GitLab CI, etc.). I test vengono eseguiti solo localmente.

### 3.8 🟢 Nessuna Persistenza
Non c'è storage locale per:
- Storico delle partite analizzate
- Configurazioni utente
- Statistiche di performance dei suggerimenti
- Cache delle risposte LLM

### 3.9 🟢 Documentazione API Interna Limitata
I file Kotlin hanno pochi KDoc. Le interfacce pubbliche (`Strategy`, `LlmProvider`, `TtsManager`) beneficerebbero di documentazione formale.

---

## 4. Rischi Tecnici

| Rischio | Probabilità | Impatto | Mitigazione |
|---------|-------------|---------|-------------|
| Riot cambia le API locali | Media | Alto | Versionare i modelli, test di deserializzazione |
| Rate limiting LLM in partita | Alta | Medio | Cache risposte, throttling, fallback a strategie rule-based |
| Vosk model download fallisce | Media | Basso | Fallback a modalità senza voice |
| Compose Desktop bug/breaking changes | Bassa | Medio | Pinning versioni, test UI |
| Memory leak su partite lunghe (Flow collection) | Bassa | Medio | Profiling, scope management |

---

## 5. Spunti di Sviluppo

### 5.1 Architettura
- **Introdurre un modulo `model/` condiviso**: Estrarre i data class comuni (GameSnapshot, ChampSelectSession, etc.) in un modulo `model/` puro, eliminando la dipendenza `brain → bridge`.
- **Dependency Injection**: Adottare Koin (leggero, Kotlin-native) per gestire il wiring dei componenti.
- **Modularizzare `Panels.kt`**: Spezzare in file per pannello (VisionPanel, EventLogPanel, StrategyPanel, etc.).

### 5.2 Testing & Quality
- **Test per ViewModel**: Testare `DashboardViewModel` e `OverlayViewModel` con coroutine test.
- **Integration test**: Simulare un flusso completo champ select → in game → post game.
- **CI/CD Pipeline**: GitHub Actions con `./gradlew test` su ogni PR.
- **Code coverage**: Integrare Kover per monitorare la copertura.

### 5.3 Funzionalità
- **Storico partite**: Persistenza locale (SQLite/Room o file JSON) per tracciare suggerimenti dati e risultati.
- **Dashboard post-game**: Analisi retrospettiva con timeline degli eventi e suggerimenti.
- **Supporto multi-ruolo**: Estendere oltre il Support (ADC, Jungle, etc.) con strategie dedicate.
- **Matchup database**: Database locale di matchup con win rate e consigli specifici per coppia di campioni.
- **Rune/item suggestions**: Suggerimenti di rune e build basati su matchup e stato della partita.
- **LLM caching**: Cache locale delle risposte LLM per situazioni simili, riducendo latenza e costi.
- **Modalità replay**: Analisi post-partita da file replay.

### 5.4 UX & Accessibilità
- **Settings UI**: Pannello configurazione per API key LLM, volume TTS, posizione overlay, lingua.
- **Notifiche prioritizzate**: Sistema di priorità per i suggerimenti (urgente vs. informativo) con stili visivi diversi.
- **Temi personalizzabili**: Light/dark mode e colori personalizzabili per l'overlay.
- **Localizzazione (i18n)**: Il file `Strings.kt` esiste già — estendere con supporto multi-lingua completo.

### 5.5 DevOps & Distribuzione
- **Auto-update**: Meccanismo di aggiornamento automatico dell'app.
- **Packaging**: Migliorare i native distributables (DMG firmato per macOS, MSI per Windows).
- **Telemetria opt-in**: Raccolta anonima di crash report e metriche d'uso.
- **Feature flags**: Sistema per abilitare/disabilitare feature sperimentali.

### 5.6 Performance
- **Profiling memoria**: Monitorare l'uso di memoria durante partite lunghe (>40 min).
- **Ottimizzazione polling**: Adattare la frequenza di polling in base alla fase di gioco (più frequente in teamfight, meno in laning tranquilla).
- **Lazy loading UI**: Caricare pannelli della dashboard on-demand.

---

## 6. Riepilogo Priorità Suggerite

| Priorità | Azione | Effort |
|----------|--------|--------|
| 🔴 P0 | Estrarre modulo `model/` per eliminare dipendenza `brain → bridge` | Medio |
| 🔴 P0 | Aggiungere test per ViewModel (`app/`) | Medio |
| 🟡 P1 | Spezzare `Panels.kt` in file separati | Basso |
| 🟡 P1 | Setup CI/CD (GitHub Actions) | Basso |
| 🟡 P1 | LLM response caching | Medio |
| 🟢 P2 | Persistenza storico partite | Alto |
| 🟢 P2 | Settings UI | Medio |
| 🟢 P2 | Dependency Injection (Koin) | Medio |
| 🟢 P3 | Supporto multi-ruolo | Alto |
| 🟢 P3 | Modalità replay | Alto |

---

*Report generato automaticamente — LoL Support Strategist Project Analysis*
