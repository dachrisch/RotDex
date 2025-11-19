package com.rotdex.data.models

/**
 * Central configuration for game economy costs and limits.
 *
 * This object defines all resource costs for gameplay actions,
 * ensuring consistent economy balance across the app.
 */
object GameConfig {
    /**
     * Card Generation Costs
     */
    const val CARD_GENERATION_ENERGY_COST = 1

    /**
     * Fusion Costs
     */
    const val FUSION_COIN_COST = 50

    /**
     * Card Pack Costs (Future Implementation)
     */
    const val BASIC_PACK_COIN_COST = 100
    const val PREMIUM_PACK_GEM_COST = 50

    /**
     * Rarity Boost Costs (Future Implementation)
     */
    const val RARITY_BOOST_GEM_COST = 25

    /**
     * Energy System
     */
    const val MAX_ENERGY = 10
    const val ENERGY_REGEN_INTERVAL_MINUTES = 30

    /**
     * Daily Rewards
     */
    const val DAILY_REWARD_BASE_COINS = 50
    const val DAILY_REWARD_BASE_ENERGY = 3
}
