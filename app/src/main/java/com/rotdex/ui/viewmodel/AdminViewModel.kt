package com.rotdex.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotdex.data.models.Card
import com.rotdex.data.models.CardRarity
import com.rotdex.data.models.UserProfile
import com.rotdex.data.repository.CardRepository
import com.rotdex.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val cardRepository: CardRepository
) : ViewModel() {

    val userProfile: StateFlow<UserProfile?> = userRepository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    fun addEnergy(amount: Int) {
        viewModelScope.launch {
            userRepository.addEnergy(amount)
            _message.value = "Added $amount energy!"
        }
    }

    fun addCoins(amount: Int) {
        viewModelScope.launch {
            userRepository.addCoins(amount)
            _message.value = "Added $amount coins!"
        }
    }

    fun addGems(amount: Int) {
        viewModelScope.launch {
            userRepository.addGems(amount)
            _message.value = "Added $amount gems!"
        }
    }

    fun addTestCard(context: Context, forcedRarity: CardRarity? = null) {
        viewModelScope.launch {
            try {
                val name = generateRandomName()
                val rarity = forcedRarity ?: randomRarity()
                val stats = generateStats(rarity)

                // Create placeholder image file
                val imagePath = createPlaceholderImage(context, name, rarity)

                val card = Card(
                    id = 0, // Auto-generate
                    prompt = "Test card: $name",
                    imageUrl = imagePath,
                    rarity = rarity,
                    name = name,
                    attack = stats.first,
                    health = stats.second,
                    biography = "A mysterious test creature summoned from the void of debugging.",
                    createdAt = System.currentTimeMillis()
                )

                cardRepository.saveCardToCollection(card)
                _message.value = "Added ${rarity.name} card: $name (ATK:${stats.first} HP:${stats.second})"
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
        }
    }

    private fun generateRandomName(): String {
        val prefixes = listOf(
            "Chaos", "Shadow", "Neon", "Turbo", "Mega", "Ultra", "Hyper",
            "Cyber", "Pixel", "Glitch", "Quantum", "Void", "Cosmic", "Astral"
        )
        val creatures = listOf(
            "Goblin", "Dragon", "Slime", "Warrior", "Mage", "Knight", "Beast",
            "Golem", "Spirit", "Phoenix", "Serpent", "Titan", "Demon", "Angel"
        )
        val suffixes = listOf(
            "", " Prime", " X", " Alpha", " Omega", " Zero", " MAX",
            " EX", " Plus", " Neo", " Ultra"
        )
        return "${prefixes.random()} ${creatures.random()}${suffixes.random()}"
    }

    private fun randomRarity(): CardRarity {
        val roll = Random.nextFloat()
        return when {
            roll < 0.05f -> CardRarity.LEGENDARY
            roll < 0.15f -> CardRarity.EPIC
            roll < 0.35f -> CardRarity.RARE
            else -> CardRarity.COMMON
        }
    }

    private fun generateStats(rarity: CardRarity): Pair<Int, Int> {
        val baseAttack = when (rarity) {
            CardRarity.LEGENDARY -> Random.nextInt(80, 100)
            CardRarity.EPIC -> Random.nextInt(60, 80)
            CardRarity.RARE -> Random.nextInt(40, 60)
            CardRarity.COMMON -> Random.nextInt(20, 40)
        }
        val baseHealth = when (rarity) {
            CardRarity.LEGENDARY -> Random.nextInt(150, 200)
            CardRarity.EPIC -> Random.nextInt(120, 150)
            CardRarity.RARE -> Random.nextInt(90, 120)
            CardRarity.COMMON -> Random.nextInt(60, 90)
        }
        return Pair(baseAttack, baseHealth)
    }

    private fun createPlaceholderImage(context: Context, name: String, rarity: CardRarity): String {
        val cardImagesDir = File(context.filesDir, "card_images")
        if (!cardImagesDir.exists()) {
            cardImagesDir.mkdirs()
        }

        val fileName = "test_${name.replace(" ", "_").lowercase()}_${System.currentTimeMillis()}.png"
        val file = File(cardImagesDir, fileName)

        // Create a simple colored bitmap based on rarity
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background color based on rarity
        val bgColor = when (rarity) {
            CardRarity.LEGENDARY -> Color.parseColor("#FFD700") // Gold
            CardRarity.EPIC -> Color.parseColor("#9932CC") // Purple
            CardRarity.RARE -> Color.parseColor("#4169E1") // Blue
            CardRarity.COMMON -> Color.parseColor("#808080") // Gray
        }

        val paint = Paint()
        paint.color = bgColor
        canvas.drawRect(0f, 0f, 512f, 512f, paint)

        // Add gradient effect
        paint.color = Color.argb(80, 255, 255, 255)
        canvas.drawRect(0f, 0f, 512f, 256f, paint)

        // Draw "TEST" text
        paint.color = Color.WHITE
        paint.textSize = 60f
        paint.textAlign = Paint.Align.CENTER
        paint.isFakeBoldText = true
        canvas.drawText("TEST", 256f, 280f, paint)

        // Draw rarity text
        paint.textSize = 40f
        canvas.drawText(rarity.name, 256f, 340f, paint)

        // Save to file
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()

        return file.absolutePath
    }
}
