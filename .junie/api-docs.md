Per sviluppare un software di coaching per League of Legends che segua il game dalla Champion Select fino alla fine  
della partita, dovrai integrare diverse API fornite da Riot Games. Di seguito trovi i link e le documentazioni più   
aggiornate, suddivise per fase di gioco e utilità.

1. Champion Select: LCU API League Client Update                                                                     
   La Riot Games API standard basata su cloud non fornisce dati in tempo reale sulla Champion Select per motivi di      
   privacy e integrità competitiva. Per ottenere questi dati, devi connetterti localmente al client di gioco tramite la
   LCU API.

• Documentazione principale Community: Hextech Docs (https://hextechdocs.dev/getting-started-with-the-lcu-api/)      
(https://hextechdocs.dev/getting-started-with-the-lcu-api/)-                                                         
(https://hextechdocs.dev/getting-started-with-the-lcu-api/)                                                          
(https://hextechdocs.dev/getting-started-with-the-lcu-api/)LCU                                                       
(https://hextechdocs.dev/getting-started-with-the-lcu-api/)                                                          
• Endpoint Chiave: GET /lol-champ-select/v1/session per ottenere lo stato attuale della selezione, campioni scelti,  
ban, timer, ecc.                                                                                                     
• Strumenti utili:                                                                                                   
• Rift Explorer: Un'app per esplorare tutti gli endpoint disponibili localmente sul tuo client.                    
• Librerie di connessione: lcu-driver (https://lcu-driver.readthedocs.io/)                                         
(https://lcu-driver.readthedocs.io/)Python (https://lcu-driver.readthedocs.io/), PoniLCU                           
(https://github.com/Ponita0/PoniLCU)  (https://github.com/Ponita0/PoniLCU)C# (https://github.com/Ponita0/PoniLCU),
o lcu-connector (https://github.com/pupix/lcu-connector)  (https://github.com/pupix/lcu-connector)Node.js          
(https://github.com/pupix/lcu-connector).

2. In-Game Live: Live Client Data API                                                                                
   Una volta che il gioco è iniziato schermata di caricamento e partita in corso, puoi ottenere dati granulari in tempo
   reale HP, gold, cooldown delle summoner spells, oggetti tramite la Live Client Data API, che risponde localmente     
   sulla porta 2999.

• Documentazione Ufficiale: Riot Developer Portal (https://developer.riotgames.com/docs/lol#live-client-data-api)    
(https://developer.riotgames.com/docs/lol#live-client-data-api)-                                                     
(https://developer.riotgames.com/docs/lol#live-client-data-api)                                                      
(https://developer.riotgames.com/docs/lol#live-client-data-api)Live Client Data                                      
(https://developer.riotgames.com/docs/lol#live-client-data-api)                                                      
• Endpoint Chiave: https://127.0.0.1:2999/liveclientdata/allgamedata                                                 
• Dati disponibili: Statistiche dei giocatori, eventi di gioco uccisioni, distruzione torri, inventario e cooldown   
delle abilità per il giocatore attivo.

3. Analisi Dati Storici e Player: Riot Games API Cloud                                                               
   Per il coaching, avrai bisogno di analizzare le statistiche passate dei giocatori es. winrate con un campione, match
   history.

• Portale Sviluppatori: Riot Developer Portal (https://developer.riotgames.com/)                                     
• API Fondamentali:                                                                                                  
• SUMMONER-V4: Per ottenere il PUUID e l'ID del summoner.                                                          
• MATCH-V5: Per recuperare lo storico delle partite e i dettagli dei match passati.                                
• SPECTATOR-V4: Per verificare se un giocatore è in partita e ottenere i dati generali del match se non vuoi usare
le API locali.

4. Dati Statici Immagini e Info Campioni: Data Dragon                                                                
   Per visualizzare icone, nomi delle abilità e descrizioni aggiornate, devi usare Data Dragon.

• Documentazione: Data Dragon Guide (https://developer.riotgames.com/docs/lol#data-dragon)                           
• Link Versioni: https://ddragon.leagueoflegends.com/api/versions.json                                               
(https://ddragon.leagueoflegends.com/api/versions.json) usa sempre l'ultima versione per i tuoi asset.               
• Community Dragon: Per asset più specifici es. icone delle rune o versioni "raw" dei file, usa CommunityDragon      
(https://www.communitydragon.org/).

Consigli per lo Sviluppo
1. CORS & SSL: Le API locali LCU e Live Client usano certificati auto-firmati. Dovrai configurare il tuo software    
   per ignorare gli errori SSL o includere il certificato root di Riot.
2. WebSockets: Per la Champion Select, invece di fare polling continuo, è meglio connettersi al WebSocket della LCU  
   per ricevere notifiche istantanee sui cambiamenti di stato es. quando un campione viene lockato.
3. Riot ID: Ricorda che Riot è passata ai Riot ID Nome#Tag. Usa l'API ACCOUNT-V1 per gestire correttamente la        
   ricerca dei giocatori.   
