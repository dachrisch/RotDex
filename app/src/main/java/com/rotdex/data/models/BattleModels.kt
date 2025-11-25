package com.rotdex.data.models

/**
 * Battle state machine states
 */
enum class BattleState {
    WAITING_FOR_OPPONENT,  // Waiting for other player to connect
    CARD_SELECTION,        // Both players selecting their card
    READY_TO_BATTLE,       // Both cards selected, ready to start
    BATTLE_ANIMATING,      // Battle animation screen showing story progression
    BATTLE_IN_PROGRESS,    // AI story being generated and displayed
    BATTLE_COMPLETE,       // Battle finished, showing result
    DISCONNECTED           // Opponent disconnected
}

/**
 * Messages sent between devices during battle (using simple string protocol)
 */
sealed class BattleMessage {
    data class CardSelected(val cardId: Long, val cardName: String, val attack: Int, val health: Int, val rarity: String) : BattleMessage()
    data class ReadyToBattle(val ready: Boolean) : BattleMessage()
    data class BattleOutcome(val winnerId: String, val winnerCardId: Long, val isDraw: Boolean, val story: String) : BattleMessage()
    data class HealthUpdate(val cardId: Long, val newHealth: Int) : BattleMessage()
    data class Disconnect(val reason: String) : BattleMessage()

    companion object {
        const val TYPE_CARD_SELECTED = "CARD_SELECTED"
        const val TYPE_READY = "READY"
        const val TYPE_OUTCOME = "OUTCOME"
        const val TYPE_HEALTH = "HEALTH"
        const val TYPE_DISCONNECT = "DISCONNECT"
    }
}

/**
 * Represents a card prepared for battle with calculated bonuses
 */
data class BattleCard(
    val card: Card,
    val effectiveAttack: Int,
    val effectiveHealth: Int,
    var currentHealth: Int
) {
    val isAlive: Boolean get() = currentHealth > 0

    companion object {
        /**
         * Create a BattleCard from a Card with rarity bonuses applied
         */
        fun fromCard(card: Card): BattleCard {
            val (attackBonus, healthBonus) = when (card.rarity) {
                CardRarity.LEGENDARY -> Pair(0.20f, 20)
                CardRarity.EPIC -> Pair(0.10f, 10)
                CardRarity.RARE -> Pair(0.05f, 5)
                CardRarity.COMMON -> Pair(0f, 0)
            }

            val effectiveAttack = (card.attack * (1 + attackBonus)).toInt()
            val effectiveHealth = card.health + healthBonus

            return BattleCard(
                card = card,
                effectiveAttack = effectiveAttack,
                effectiveHealth = effectiveHealth,
                currentHealth = effectiveHealth
            )
        }
    }
}

/**
 * Result of a battle
 */
data class BattleResult(
    val isDraw: Boolean,
    val winnerIsLocal: Boolean?,  // null if draw
    val winnerCardName: String?,
    val loserCardName: String?,
    val localCardFinalHealth: Int,
    val opponentCardFinalHealth: Int,
    val battleStory: String,
    val cardWon: Card? = null  // The card won from opponent (null if lost or draw)
)

/**
 * Battle story segment for progressive display
 */
data class BattleStorySegment(
    val text: String,
    val isLocalAction: Boolean,  // true if local player's card is acting
    val damageDealt: Int? = null
)
