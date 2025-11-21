package com.rotdex.data.models

/**
 * Predefined fusion recipes
 */
object FusionRecipes {
    val ALL_RECIPES = listOf(
        // Beginner recipes
        FusionRecipe(
            id = "triple_common",
            name = "Triple Threat",
            description = "Fuse 3 Common cards for guaranteed Rare",
            requiredCards = listOf(
                FusionCardRequirement(CardRarity.COMMON, 3)
            ),
            guaranteedRarity = CardRarity.RARE,
            isSecret = false
        ),

        FusionRecipe(
            id = "quad_common",
            name = "Four Leaf Clover",
            description = "Fuse 4 Common cards for guaranteed Rare with 50% Epic chance",
            requiredCards = listOf(
                FusionCardRequirement(CardRarity.COMMON, 4)
            ),
            guaranteedRarity = CardRarity.RARE, // Guaranteed, with 50% Epic chance
            isSecret = false
        ),

        FusionRecipe(
            id = "triple_rare",
            name = "Rare Trinity",
            description = "Fuse 3 Rare cards for 60% Epic chance",
            requiredCards = listOf(
                FusionCardRequirement(CardRarity.RARE, 3)
            ),
            guaranteedRarity = CardRarity.RARE,
            isSecret = false
        ),

        // Advanced recipes
        FusionRecipe(
            id = "double_epic",
            name = "Epic Gamble",
            description = "Fuse 2 Epic cards for 50% Legendary chance",
            requiredCards = listOf(
                FusionCardRequirement(CardRarity.EPIC, 2)
            ),
            guaranteedRarity = CardRarity.EPIC,
            isSecret = false
        ),

        // Secret recipes (discovered by experimentation)
        FusionRecipe(
            id = "cursed_fusion",
            name = "Cursed Ritual",
            description = "Fuse 3 'cursed' cards for guaranteed Epic",
            requiredCards = listOf(
                FusionCardRequirement(CardRarity.COMMON, 3, tagRequired = "cursed")
            ),
            guaranteedRarity = CardRarity.EPIC,
            guaranteedTags = listOf("cursed", "fusion"),
            isSecret = true
        ),

        FusionRecipe(
            id = "sigma_fusion",
            name = "Sigma Grindset",
            description = "Fuse 5 cards with 'sigma' tag",
            requiredCards = listOf(
                FusionCardRequirement(CardRarity.COMMON, 5, tagRequired = "sigma")
            ),
            guaranteedRarity = CardRarity.LEGENDARY,
            guaranteedTags = listOf("sigma", "based", "legendary"),
            isSecret = true
        ),

        FusionRecipe(
            id = "based_fusion",
            name = "Based Beyond Belief",
            description = "Fuse 3 'based' Rare cards for guaranteed Epic",
            requiredCards = listOf(
                FusionCardRequirement(CardRarity.RARE, 3, tagRequired = "based")
            ),
            guaranteedRarity = CardRarity.EPIC,
            guaranteedTags = listOf("based", "ultra-based"),
            isSecret = true
        ),

        FusionRecipe(
            id = "rainbow_fusion",
            name = "Rainbow Road",
            description = "One of each rarity = guaranteed Legendary",
            requiredCards = listOf(
                FusionCardRequirement(CardRarity.COMMON, 1),
                FusionCardRequirement(CardRarity.RARE, 1),
                FusionCardRequirement(CardRarity.EPIC, 1),
                FusionCardRequirement(CardRarity.LEGENDARY, 1)
            ),
            guaranteedRarity = CardRarity.LEGENDARY,
            guaranteedTags = listOf("rainbow", "special"),
            isSecret = true
        )
    )

    /**
     * Find a matching recipe for the given cards
     */
    fun findMatchingRecipe(cards: List<Card>): FusionRecipe? {
        return ALL_RECIPES.find { recipe ->
            matchesRecipe(cards, recipe)
        }
    }

    /**
     * Check if cards match a recipe
     */
    private fun matchesRecipe(cards: List<Card>, recipe: FusionRecipe): Boolean {
        val cardsByRarity = cards.groupBy { it.rarity }

        return recipe.requiredCards.all { requirement ->
            val matchingCards = cardsByRarity[requirement.rarity] ?: emptyList()

            if (requirement.tagRequired != null) {
                matchingCards.count { card ->
                    card.tags.contains(requirement.tagRequired)
                } >= requirement.count
            } else {
                matchingCards.size >= requirement.count
            }
        }
    }

    /**
     * Get all non-secret recipes (for displaying in UI)
     */
    fun getPublicRecipes(): List<FusionRecipe> = ALL_RECIPES.filter { !it.isSecret }

    /**
     * Check if a recipe has been discovered
     */
    fun getRecipeById(id: String): FusionRecipe? = ALL_RECIPES.find { it.id == id }
}
