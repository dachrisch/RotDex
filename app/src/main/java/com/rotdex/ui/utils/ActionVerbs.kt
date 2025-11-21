package com.rotdex.ui.utils

/**
 * Playful action verbs for card generation and fusion animations
 * Mix of real actions, tech jargon, and pure brainrot chaos
 */
object ActionVerbs {
    val generationVerbs = listOf(
        // Real actions
        "Creating your card",
        "Generating masterpiece",
        "Crafting something epic",
        "Forging the card",
        "Building your dream",
        "Conjuring magic",
        "Manifesting vibes",
        "Summoning powers",
        "Brewing chaos",
        "Mixing ingredients",

        // Tech jargon
        "Compiling brainrot",
        "Rendering pixels",
        "Processing requests",
        "Computing rarity",
        "Encrypting data",
        "Debugging universe",
        "Optimizing vibes",
        "Syncing dimensions",
        "Deploying card",
        "Initializing chaos",

        // Pure brainrot
        "Rizzing up the card",
        "Yapping to the AI",
        "Mogging the database",
        "Gyattifying pixels",
        "Skibidi-fying image",
        "Making it bussin",
        "Absolutely slaying",
        "Sheeesh-ing hard",
        "Vibing intensely",
        "Tweaking parameters",
        "Zapping electrons",
        "Bopping bits",
        "Zooming through data",
        "Whooshing magic",
        "Bamboozling physics",
        "Discombobulating atoms",
        "Glitching reality",
        "Beaming energy",
        "Edging closer",
        "Gooning the AI"
    )

    val fusionVerbs = listOf(
        // Real fusion actions
        "Fusing cards together",
        "Merging powers",
        "Combining forces",
        "Blending energies",
        "Mixing chaos",
        "Melting cards down",
        "Smashing together",
        "Colliding universes",
        "Brewing fusion",
        "Cooking up heat",

        // Tech fusion
        "Compiling combo",
        "Processing fusion",
        "Calculating synergy",
        "Rendering result",
        "Optimizing merge",
        "Syncing energies",
        "Encrypting combo",
        "Computing outcome",
        "Deploying result",
        "Initializing mash",

        // Brainrot fusion
        "Rizzing the blend",
        "Yapping cards together",
        "Mogging the fusion",
        "Gyattifying combo",
        "Skibidi-fying merge",
        "Making it bussin",
        "Slaying the mix",
        "Sheeesh-ing fusion",
        "Vibing cards hard",
        "Tweaking the mash",
        "Zapping them together",
        "Bopping the blend",
        "Zooming fusion",
        "Whooshing magic",
        "Bamboozling cards",
        "Discombobulating deck",
        "Glitching spacetime",
        "Beaming powers",
        "Edging to victory",
        "Gooning the mash"
    )

    /**
     * Get a random generation verb
     */
    fun randomGenerationVerb(): String = generationVerbs.random()

    /**
     * Get a random fusion verb
     */
    fun randomFusionVerb(): String = fusionVerbs.random()

    /**
     * Get multiple random generation verbs for cycling animation
     */
    fun getGenerationVerbCycle(count: Int = 5): List<String> {
        return generationVerbs.shuffled().take(count)
    }

    /**
     * Get multiple random fusion verbs for cycling animation
     */
    fun getFusionVerbCycle(count: Int = 5): List<String> {
        return fusionVerbs.shuffled().take(count)
    }
}
