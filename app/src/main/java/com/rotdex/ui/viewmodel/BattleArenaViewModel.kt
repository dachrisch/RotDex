package com.rotdex.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotdex.data.manager.BattleManager
import com.rotdex.data.manager.ConnectionState
import com.rotdex.data.models.BattleCard
import com.rotdex.data.models.BattleResult
import com.rotdex.data.models.BattleState
import com.rotdex.data.models.BattleStorySegment
import com.rotdex.data.models.Card
import com.rotdex.data.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BattleArenaViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cardRepository: CardRepository
) : ViewModel() {

    private val battleManager = BattleManager(context)

    // Connection state
    val connectionState: StateFlow<ConnectionState> = battleManager.connectionState

    // Battle state
    val battleState: StateFlow<BattleState> = battleManager.battleState

    // Cards
    val localCard: StateFlow<BattleCard?> = battleManager.localCard
    val opponentCard: StateFlow<BattleCard?> = battleManager.opponentCard

    // Stats revealed (after both ready)
    val statsRevealed: StateFlow<Boolean> = battleManager.statsRevealed

    // Battle story for progressive display
    val battleStory: StateFlow<List<BattleStorySegment>> = battleManager.battleStory

    // Currently displayed story segment index
    private val _currentStoryIndex = MutableStateFlow(0)
    val currentStoryIndex: StateFlow<Int> = _currentStoryIndex.asStateFlow()

    // Card transfer state
    private val _cardTransferred = MutableStateFlow(false)
    val cardTransferred: StateFlow<Boolean> = _cardTransferred.asStateFlow()

    // Selected card ID (to delete if lost)
    private var selectedCardId: Long? = null

    // Battle result
    val battleResult: StateFlow<BattleResult?> = battleManager.battleResult

    // Messages/log
    val messages: StateFlow<List<String>> = battleManager.messages

    // Discovered devices
    val discoveredDevices: StateFlow<List<String>> = battleManager.discoveredDevices

    // Player's card collection for selection
    val playerCards: StateFlow<List<Card>> = cardRepository.getAllCards().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Player name
    private val _playerName = MutableStateFlow("Player${(1000..9999).random()}")
    val playerName: StateFlow<String> = _playerName.asStateFlow()

    fun setPlayerName(name: String) {
        _playerName.value = name
    }

    fun startAsHost() {
        battleManager.startAsHost(_playerName.value)
    }

    fun startAsClient() {
        battleManager.startAsClient(_playerName.value)
    }

    fun connectToHost(deviceInfo: String) {
        // deviceInfo format: "name|endpointId"
        val endpointId = deviceInfo.split("|").getOrNull(1) ?: return
        battleManager.connectToHost(endpointId)
    }

    fun selectCard(card: Card) {
        selectedCardId = card.id
        battleManager.selectCard(card)
    }

    init {
        // Observe battle state and start animation when BATTLE_ANIMATING state is entered
        viewModelScope.launch {
            battleManager.battleState.collect { state ->
                if (state == BattleState.BATTLE_ANIMATING && battleManager.battleStory.value.isNotEmpty()) {
                    animateStory()
                }
            }
        }
    }

    fun setReady() {
        battleManager.setReady()
    }

    private var storyAnimationJob: kotlinx.coroutines.Job? = null

    private suspend fun animateStory() {
        _currentStoryIndex.value = 0
        val segments = battleManager.battleStory.value

        storyAnimationJob = viewModelScope.launch {
            for (i in segments.indices) {
                _currentStoryIndex.value = i
                // Delay between segments for dramatic effect (2 seconds per segment)
                delay(2000L)
            }

            // Animation complete, transition to results
            battleManager.completeBattleAnimation()
        }
    }

    /**
     * Skip the battle animation and jump directly to the results
     * This cancels the ongoing animation and shows the final result
     */
    fun skipBattleAnimation() {
        storyAnimationJob?.cancel()
        val segments = battleManager.battleStory.value
        if (segments.isNotEmpty()) {
            _currentStoryIndex.value = segments.size - 1
        }
        // Transition to battle complete state
        battleManager.completeBattleAnimation()
    }

    fun resetStoryAnimation() {
        _currentStoryIndex.value = 0
        storyAnimationJob?.cancel()
        storyAnimationJob = null
    }

    /**
     * Transfer card after battle:
     * - Winner receives opponent's card (added to collection)
     * - Loser's card is deleted from their collection
     * - Draw: both lose their cards (deleted)
     */
    fun processCardTransfer() {
        if (_cardTransferred.value) return

        viewModelScope.launch {
            val result = battleResult.value ?: return@launch

            when {
                result.isDraw -> {
                    // Draw: delete local card
                    selectedCardId?.let { id ->
                        cardRepository.getCardById(id)?.let { card ->
                            cardRepository.deleteCard(card)
                        }
                    }
                }
                result.winnerIsLocal == true -> {
                    // Won: add opponent's card to collection
                    result.cardWon?.let { wonCard ->
                        // Create new card with fresh ID (0 = auto-generate)
                        val newCard = wonCard.copy(
                            id = 0,
                            createdAt = System.currentTimeMillis()
                        )
                        cardRepository.saveCardToCollection(newCard)
                    }
                }
                result.winnerIsLocal == false -> {
                    // Lost: delete local card
                    selectedCardId?.let { id ->
                        cardRepository.getCardById(id)?.let { card ->
                            cardRepository.deleteCard(card)
                        }
                    }
                }
            }

            _cardTransferred.value = true
        }
    }

    fun stopAll() {
        battleManager.stopAll()
        _currentStoryIndex.value = 0
        _cardTransferred.value = false
        selectedCardId = null
    }

    override fun onCleared() {
        super.onCleared()
        battleManager.stopAll()
    }
}
