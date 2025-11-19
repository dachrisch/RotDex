package com.rotdex.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotdex.data.models.Card
import com.rotdex.data.models.GameConfig
import com.rotdex.data.models.InsufficientEnergyException
import com.rotdex.data.models.UserProfile
import com.rotdex.data.repository.CardRepository
import com.rotdex.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for card creation
 * Manages card generation with energy cost checking
 */
@HiltViewModel
class CardCreateViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val userProfile: StateFlow<UserProfile?> = userRepository.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _cardGenerationState = MutableStateFlow<CardGenerationState>(CardGenerationState.Idle)
    val cardGenerationState: StateFlow<CardGenerationState> = _cardGenerationState.asStateFlow()

    /**
     * Generate a new card with the given prompt
     * Checks and spends energy before generating
     */
    fun generateCard(prompt: String) {
        if (prompt.isBlank()) {
            _cardGenerationState.value = CardGenerationState.Error("Please enter a prompt")
            return
        }

        viewModelScope.launch {
            try {
                _cardGenerationState.value = CardGenerationState.Generating

                val result = cardRepository.generateCard(prompt)

                result.fold(
                    onSuccess = { card ->
                        _cardGenerationState.value = CardGenerationState.Success(card)
                    },
                    onFailure = { error ->
                        when (error) {
                            is InsufficientEnergyException -> {
                                _cardGenerationState.value = CardGenerationState.InsufficientEnergy(
                                    required = GameConfig.CARD_GENERATION_ENERGY_COST,
                                    current = userProfile.value?.currentEnergy ?: 0
                                )
                            }
                            else -> {
                                _cardGenerationState.value = CardGenerationState.Error(
                                    error.message ?: "Failed to generate card"
                                )
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                _cardGenerationState.value = CardGenerationState.Error(
                    e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    /**
     * Reset the generation state back to idle
     */
    fun resetState() {
        _cardGenerationState.value = CardGenerationState.Idle
    }

    /**
     * Check if user has enough energy to generate a card
     */
    fun hasEnoughEnergy(): Boolean {
        val profile = userProfile.value ?: return false
        return profile.currentEnergy >= GameConfig.CARD_GENERATION_ENERGY_COST
    }
}

/**
 * States for card generation
 */
sealed class CardGenerationState {
    object Idle : CardGenerationState()
    object Generating : CardGenerationState()
    data class Success(val card: Card) : CardGenerationState()
    data class InsufficientEnergy(val required: Int, val current: Int) : CardGenerationState()
    data class Error(val message: String) : CardGenerationState()
}
