package com.lolcoach.bridge.model.liveclient

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GameSnapshotDeserializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `deserialize minimal GameSnapshot`() {
        val raw = """
        {
          "activePlayer": {
            "level": 5,
            "summonerName": "TestPlayer",
            "currentGold": 1500.0,
            "championStats": {
              "maxHealth": 800.0,
              "currentHealth": 650.0,
              "attackDamage": 70.0,
              "armor": 35.0,
              "magicResist": 30.0,
              "moveSpeed": 330.0
            }
          },
          "allPlayers": [
            {
              "championName": "Leona",
              "level": 5,
              "position": "SUPPORT",
              "team": "ORDER",
              "scores": {
                "kills": 1,
                "deaths": 2,
                "assists": 7,
                "creepScore": 15,
                "wardScore": 12.0
              },
              "items": [
                {
                  "displayName": "Stealth Ward",
                  "itemID": 3340,
                  "count": 1,
                  "slot": 6
                }
              ]
            },
            {
              "championName": "Jinx",
              "level": 5,
              "position": "ADC",
              "team": "ORDER",
              "scores": {
                "kills": 4,
                "deaths": 1,
                "assists": 3,
                "creepScore": 85,
                "wardScore": 5.0
              }
            }
          ],
          "gameData": {
            "gameMode": "CLASSIC",
            "gameTime": 612.5,
            "mapName": "Map11",
            "mapNumber": 11,
            "mapTerrain": "Default"
          },
          "events": {
            "Events": [
              {
                "EventID": 0,
                "EventName": "GameStart",
                "EventTime": 0.0
              },
              {
                "EventID": 1,
                "EventName": "ChampionKill",
                "EventTime": 300.0,
                "KillerName": "Jinx",
                "VictimName": "EnemyBot"
              }
            ]
          }
        }
        """.trimIndent()

        val snapshot = json.decodeFromString<GameSnapshot>(raw)

        assertNotNull(snapshot.activePlayer)
        assertEquals(5, snapshot.activePlayer!!.level)
        assertEquals("TestPlayer", snapshot.activePlayer!!.summonerName)
        assertEquals(1500.0, snapshot.activePlayer!!.currentGold)
        assertEquals(800.0, snapshot.activePlayer!!.championStats?.maxHealth)

        assertEquals(2, snapshot.allPlayers.size)
        assertEquals("Leona", snapshot.allPlayers[0].championName)
        assertEquals("SUPPORT", snapshot.allPlayers[0].position)
        assertEquals(7, snapshot.allPlayers[0].scores?.assists)
        assertEquals(1, snapshot.allPlayers[0].items.size)
        assertEquals(3340, snapshot.allPlayers[0].items[0].itemID)

        assertEquals("Jinx", snapshot.allPlayers[1].championName)
        assertEquals(85, snapshot.allPlayers[1].scores?.creepScore)

        assertNotNull(snapshot.gameData)
        assertEquals("CLASSIC", snapshot.gameData!!.gameMode)
        assertEquals(612.5, snapshot.gameData!!.gameTime)

        assertNotNull(snapshot.events)
        assertEquals(2, snapshot.events!!.events.size)
        assertEquals("GameStart", snapshot.events!!.events[0].eventName)
        assertEquals("ChampionKill", snapshot.events!!.events[1].eventName)
        assertEquals("Jinx", snapshot.events!!.events[1].killerName)
    }

    @Test
    fun `deserialize empty GameSnapshot`() {
        val raw = """{}"""
        val snapshot = json.decodeFromString<GameSnapshot>(raw)
        assertEquals(null, snapshot.activePlayer)
        assertEquals(emptyList(), snapshot.allPlayers)
        assertEquals(null, snapshot.gameData)
    }
}
