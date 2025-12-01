package com.rotdex.ui.viewmodel

import android.content.Context
import android.util.Log
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
import com.rotdex.data.repository.UserRepository
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
    private val cardRepository: CardRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val battleManager = BattleManager(context)

    // Connection state
    val connectionState: StateFlow<ConnectionState> = battleManager.connectionState

    // Battle state
    val battleState: StateFlow<BattleState> = battleManager.battleState

    // Cards
    val localCard: StateFlow<BattleCard?> = battleManager.localCard
    val opponentCard: StateFlow<BattleCard?> = battleManager.opponentCard
    val opponentHasSelectedCard: StateFlow<Boolean> = battleManager.opponentHasSelectedCard

    // Opponent player name
    val opponentName: StateFlow<String> = battleManager.opponentName

    // Stats revealed (after both ready)
    val statsRevealed: StateFlow<Boolean> = battleManager.statsRevealed

    // Battle story for progressive display
    val battleStory: StateFlow<List<BattleStorySegment>> = battleManager.battleStory

    // Ready states
    val localReady: StateFlow<Boolean> = battleManager.localReady
    val opponentReady: StateFlow<Boolean> = battleManager.opponentReady
    val canClickReady: StateFlow<Boolean> = battleManager.canClickReady
    val opponentIsThinking: StateFlow<Boolean> = battleManager.opponentIsThinking
    val shouldRevealCards: StateFlow<Boolean> = battleManager.shouldRevealCards

    // Data synchronization states
    val waitingForOpponentReady: StateFlow<Boolean> = battleManager.waitingForOpponentReady
    val localDataComplete: StateFlow<Boolean> = battleManager.localDataComplete
    val opponentDataComplete: StateFlow<Boolean> = battleManager.opponentDataComplete
    val opponentImageTransferComplete: StateFlow<Boolean> = battleManager.opponentImageTransferComplete
    val localImageSent: StateFlow<Boolean> = battleManager.localImageSent

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

    fun startAutoDiscovery() {
        val name = _playerName.value
        battleManager.startAutoDiscovery(name)
    }

    fun connectToDevice(endpointId: String) {
        battleManager.connectToHost(endpointId)
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
        // Load player name from UserProfile
        viewModelScope.launch {
            userRepository.userProfile.collect { profile ->
                _playerName.value = profile.playerName
                Log.d("BattleArenaViewModel", "👤 Loaded player name: ${profile.playerName}")
            }
        }

        // Observe battle state and story to start animation when both are ready
        // Animation starts when: state is BATTLE_ANIMATING AND story is not empty AND result received AND animation not already running
        // CRITICAL: Must wait for battleResult to ensure CLIENT has received ALL story segments
        // (HOST sends RESULT after all STORY messages, so result arrival = story complete)
        viewModelScope.launch {
            battleManager.battleState.collect { state ->
                if (state == BattleState.BATTLE_ANIMATING &&
                    battleManager.battleStory.value.isNotEmpty() &&
                    battleManager.battleResult.value != null &&
                    storyAnimationJob?.isActive != true) {
                    Log.d("BattleArenaViewModel", "🎬 Starting animation from state change")
                    animateStory()
                }
            }
        }

        // Also watch story changes - for CLIENT who receives story after state change
        viewModelScope.launch {
            battleManager.battleStory.collect { story ->
                if (battleManager.battleState.value == BattleState.BATTLE_ANIMATING &&
                    story.isNotEmpty() &&
                    battleManager.battleResult.value != null &&
                    storyAnimationJob?.isActive != true) {
                    Log.d("BattleArenaViewModel", "🎬 Starting animation from story update")
                    animateStory()
                }
            }
        }

        // Also watch result changes - CLIENT receives result AFTER all story segments
        // This is the reliable trigger point for CLIENT animation
        viewModelScope.launch {
            battleManager.battleResult.collect { result ->
                if (battleManager.battleState.value == BattleState.BATTLE_ANIMATING &&
                    battleManager.battleStory.value.isNotEmpty() &&
                    result != null &&
                    storyAnimationJob?.isActive != true) {
                    Log.d("BattleArenaViewModel", "🎬 Starting animation from result received (CLIENT sync point)")
                    animateStory()
                }
            }
        }

        // Log discovered devices updates for debugging
        viewModelScope.launch {
            discoveredDevices.collect { devices ->
                Log.d("BattleArenaViewModel", "📱 discoveredDevices updated in ViewModel: ${devices.size} devices - $devices")
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
