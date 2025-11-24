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

    // Battle story for progressive display
    val battleStory: StateFlow<List<BattleStorySegment>> = battleManager.battleStory

    // Currently displayed story segment index
    private val _currentStoryIndex = MutableStateFlow(0)
    val currentStoryIndex: StateFlow<Int> = _currentStoryIndex.asStateFlow()

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
        battleManager.selectCard(card)
    }

    fun setReady() {
        battleManager.setReady()

        // Start story animation when battle begins
        viewModelScope.launch {
            // Wait for battle to complete
            battleManager.battleState.collect { state ->
                if (state == BattleState.BATTLE_IN_PROGRESS || state == BattleState.BATTLE_COMPLETE) {
                    animateStory()
                    return@collect
                }
            }
        }
    }

    private suspend fun animateStory() {
        _currentStoryIndex.value = 0
        val segments = battleManager.battleStory.value

        for (i in segments.indices) {
            _currentStoryIndex.value = i
            // Delay between segments for dramatic effect
            delay(2000L)
        }
    }

    fun resetStoryAnimation() {
        _currentStoryIndex.value = 0
    }

    fun stopAll() {
        battleManager.stopAll()
        _currentStoryIndex.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        battleManager.stopAll()
    }
}
