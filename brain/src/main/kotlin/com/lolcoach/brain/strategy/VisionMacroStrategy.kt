package com.lolcoach.brain.strategy

import com.lolcoach.brain.event.GameEvent
import com.lolcoach.brain.event.Strategy
import com.lolcoach.brain.state.GameState
import com.lolcoach.bridge.model.liveclient.GameSnapshot

class VisionMacroStrategy : Strategy {

    companion object {
        const val WARD_REMINDER_TIME_THRESHOLD = 600.0 // 10 minutes
        const val CONTROL_WARD_ITEM_ID = 2055
        const val STEALTH_WARD_ITEM_ID = 3340
        const val ORACLE_LENS_ITEM_ID = 3364
        const val FARSIGHT_ALTERATION_ITEM_ID = 3363
    }

    override fun evaluate(snapshot: GameSnapshot, state: GameState): List<GameEvent> {
        val events = mutableListOf<GameEvent>()
        val gameTime = snapshot.gameData?.gameTime ?: return events
        val activePlayer = snapshot.activePlayer ?: return events

        val ourPlayer = snapshot.allPlayers.find {
            it.summonerName == activePlayer.summonerName ||
                it.riotId == activePlayer.riotId
        } ?: return events

        // Check ward charges
        val wardItem = ourPlayer.items.find { it.itemID == STEALTH_WARD_ITEM_ID }
        val wardCharges = wardItem?.count ?: 0

        // After 10 minutes, if ward charges are available, suggest warding
        if (gameTime > WARD_REMINDER_TIME_THRESHOLD && wardCharges >= 2) {
            events.add(
                GameEvent.VisionNeeded(
                    "Hai $wardCharges cariche di ward disponibili. Warda gli obiettivi!"
                )
            )
        }

        // Check if player has no control ward in inventory
        val hasControlWard = ourPlayer.items.any { it.itemID == CONTROL_WARD_ITEM_ID }
        if (gameTime > WARD_REMINDER_TIME_THRESHOLD && !hasControlWard) {
            events.add(
                GameEvent.ItemSuggestion(
                    "Control Ward",
                    "Non hai Control Ward in inventario, comprala al prossimo back"
                )
            )
        }

        // Suggest oracle lens upgrade after laning phase
        if (gameTime > 900.0) { // 15 minutes
            val hasStealthWard = ourPlayer.items.any { it.itemID == STEALTH_WARD_ITEM_ID }
            if (hasStealthWard) {
                events.add(
                    GameEvent.ItemSuggestion(
                        "Oracle Lens",
                        "Considera di switchare a Oracle Lens per il mid-game"
                    )
                )
            }
        }

        return events
    }
}
