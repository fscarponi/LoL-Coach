# Riot APIs Documentation for LoL Support Strategist

To analyze the game state in real-time and provide strategic coaching, you'll need to integrate several APIs provided by Riot Games. Below are the most up-to-date links and documentation, divided by game phase and utility.

## 1. Local Game APIs (LCU & Live Client Data)

These APIs run directly on the user's computer while the League of Legends client is open. They are the most critical for real-time coaching as they provide low-latency data without requiring an official Riot API Key (though the LCU requires local authentication).

### A. League Client Update (LCU) API
**Use for**: Champion Select, Lobby, End of Game, Friends List, and general client state.

*   **Authentication**: Requires a `remoting-auth-token` and `port` found in the `lockfile` within the LoL installation directory.
*   **Protocol**: HTTPS (Port 127.0.0.1:port) or WSS (WebSocket).
*   **Security**: Uses self-signed SSL certificates. Your HTTP client must be configured to bypass SSL verification.
*   **Documentation**:
    *   [Riot LCU API Documentation (Unofficial/Community)](https://hextechdocs.dev/getting-started-with-the-lcu-api/)
    *   [LCU Explorer](https://github.com/Pupix/lcu-explorer): A tool to browse available endpoints locally.
*   **Key Endpoints**:
    *   `/lol-champ-select/v1/session`: Real-time data of the current Champion Select.
    *   `/lol-gameflow/v1/gameflow-phase`: Current phase (None, Lobby, Matchmaking, ReadyCheck, ChampSelect, InProgress, EndOfGame).

### B. Live Client Data API
**Use for**: In-game data (active player stats, items, events like kills/towers, dragon/baron timers).

*   **Availability**: Only available once the game has started (loading screen and active match).
*   **Port**: Fixed at `2999`.
*   **Authentication**: None required.
*   **Documentation**: [Riot Live Client Data API Official Docs](https://developer.riotgames.com/docs/lol#live-client-data-api)
*   **Key Endpoints**:
    *   `https://127.0.0.1:2999/liveclientdata/allgamedata`: Returns a massive JSON with everything (player stats, all players, events, scores).
    *   `https://127.0.0.1:2999/liveclientdata/activeplayer`: Specific stats (AP, AD, CDR, etc.) and ability levels for the active player.

## 2. Remote Riot Games APIs (Developer Portal)

These require a standard API Key from the [Riot Developer Portal](https://developer.riotgames.com/).

*   **SUMMONER-V4**: To get basic summoner info (Level, ID, AccountID, PUUID).
*   **MATCH-V5**: To get match history and detailed timelines of past games (useful for post-game analysis).
*   **LEAGUE-V4**: To get rank info (Tier, Division, LP).
*   **SPECTATOR-V4**: To check if a player is in-game and get general match data if you don't want to use local polling.

## 3. Static Data (Assets & Metadata)

To display icons, ability names, and updated descriptions, you must use Data Dragon.

*   **Data Dragon (DDragon)**: The official source for static assets (images for champions, items, runes) and JSON files containing descriptions.
    *   Documentation: [Riot Data Dragon Docs](https://developer.riotgames.com/docs/lol#data-dragon)
*   **Community Dragon**: For more specific assets (e.g., rune icons or "raw" versions of files), use CommunityDragon.
    *   Documentation: [Community Dragon Docs](https://raw.communitydragon.org/)

## Best Practices for This Project

1.  **Polling Frequency**: For the Live Client Data API, do not exceed 1Hz (1 request per second) to avoid impacting game performance.
2.  **WebSockets**: For Champion Select, instead of continuous polling, it's better to connect to the LCU WebSocket for real-time updates.
3.  **Riot ID**: Remember that Riot has transitioned to Riot IDs (Name#Tag). Use the ACCOUNT-V1 API to handle these correctly.
