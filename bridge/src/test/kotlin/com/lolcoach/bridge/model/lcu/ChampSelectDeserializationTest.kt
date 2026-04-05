package com.lolcoach.bridge.model.lcu

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ChampSelectDeserializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `deserialize ChampSelectSession`() {
        val raw = """
        {
          "actions": [
            [
              {
                "actorCellId": 0,
                "championId": 89,
                "completed": true,
                "id": 1,
                "isAllyAction": true,
                "isInProgress": false,
                "type": "pick"
              }
            ]
          ],
          "counter": 5,
          "localPlayerCellId": 4,
          "myTeam": [
            {
              "assignedPosition": "bottom",
              "cellId": 3,
              "championId": 222,
              "spell1Id": 4,
              "spell2Id": 7,
              "summonerId": 12345,
              "team": 1
            },
            {
              "assignedPosition": "utility",
              "cellId": 4,
              "championId": 89,
              "spell1Id": 4,
              "spell2Id": 14,
              "summonerId": 67890,
              "team": 1
            }
          ],
          "theirTeam": [
            {
              "assignedPosition": "bottom",
              "cellId": 5,
              "championId": 51,
              "team": 2
            },
            {
              "assignedPosition": "utility",
              "cellId": 6,
              "championId": 412,
              "team": 2
            }
          ],
          "timer": {
            "adjustedTimeLeftInPhase": 15000,
            "internalNowInEpochMs": 1700000000000,
            "isInfinite": false,
            "phase": "BAN_PICK",
            "totalTimeInPhase": 30000
          }
        }
        """.trimIndent()

        val session = json.decodeFromString<ChampSelectSession>(raw)

        assertEquals(4, session.localPlayerCellId)
        assertEquals(2, session.myTeam.size)
        assertEquals(2, session.theirTeam.size)

        assertEquals("bottom", session.myTeam[0].assignedPosition)
        assertEquals(222, session.myTeam[0].championId)
        assertEquals("utility", session.myTeam[1].assignedPosition)
        assertEquals(89, session.myTeam[1].championId)

        assertEquals(412, session.theirTeam[1].championId)

        assertNotNull(session.timer)
        assertEquals("BAN_PICK", session.timer!!.phase)

        assertEquals(1, session.actions.size)
        assertEquals(1, session.actions[0].size)
        assertEquals("pick", session.actions[0][0].type)
        assertEquals(89, session.actions[0][0].championId)
    }

    @Test
    fun `deserialize empty ChampSelectSession`() {
        val raw = """{}"""
        val session = json.decodeFromString<ChampSelectSession>(raw)
        assertEquals(-1, session.localPlayerCellId)
        assertEquals(emptyList(), session.myTeam)
        assertEquals(emptyList(), session.theirTeam)
    }
}
