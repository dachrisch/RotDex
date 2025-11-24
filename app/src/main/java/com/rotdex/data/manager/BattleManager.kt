package com.rotdex.data.manager

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.rotdex.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Manages battle arena logic including:
 * - Nearby Connections for device communication
 * - Card selection and synchronization
 * - Battle damage calculation with chance factor
 * - AI-generated battle story
 */
class BattleManager(private val context: Context) {

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val serviceId = "com.rotdex.battle.arena"

    // Connection state
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Battle state
    private val _battleState = MutableStateFlow(BattleState.WAITING_FOR_OPPONENT)
    val battleState: StateFlow<BattleState> = _battleState.asStateFlow()

    // Selected cards
    private val _localCard = MutableStateFlow<BattleCard?>(null)
    val localCard: StateFlow<BattleCard?> = _localCard.asStateFlow()

    private val _opponentCard = MutableStateFlow<BattleCard?>(null)
    val opponentCard: StateFlow<BattleCard?> = _opponentCard.asStateFlow()

    // Battle story segments for progressive display
    private val _battleStory = MutableStateFlow<List<BattleStorySegment>>(emptyList())
    val battleStory: StateFlow<List<BattleStorySegment>> = _battleStory.asStateFlow()

    // Battle result
    private val _battleResult = MutableStateFlow<BattleResult?>(null)
    val battleResult: StateFlow<BattleResult?> = _battleResult.asStateFlow()

    // Messages/log
    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages.asStateFlow()

    // Discovered devices
    private val _discoveredDevices = MutableStateFlow<List<String>>(emptyList())
    val discoveredDevices: StateFlow<List<String>> = _discoveredDevices.asStateFlow()

    private var currentEndpointId: String? = null
    private var isHost: Boolean = false
    private var opponentReady: Boolean = false
    private var localReady: Boolean = false
    private var playerName: String = ""

    // Connection lifecycle callbacks
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(TAG, "Connection initiated with: ${info.endpointName}")
            addMessage("Connection initiated with ${info.endpointName}")
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "Connection successful!")
                    currentEndpointId = endpointId
                    _connectionState.value = ConnectionState.Connected(endpointId)
                    _battleState.value = BattleState.CARD_SELECTION
                    addMessage("Connected! Select your card for battle.")
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    _connectionState.value = ConnectionState.Error("Connection rejected")
                    addMessage("Connection rejected")
                }
                else -> {
                    _connectionState.value = ConnectionState.Error("Connection failed")
                    addMessage("Connection failed")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from: $endpointId")
            _connectionState.value = ConnectionState.Disconnected
            _battleState.value = BattleState.DISCONNECTED
            currentEndpointId = null
            addMessage("Opponent disconnected")
        }
    }

    // Payload callback for battle messages
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { bytes ->
                val messageStr = String(bytes, Charsets.UTF_8)
                Log.d(TAG, "Received: $messageStr")
                handleReceivedMessage(messageStr)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Not needed for text messages
        }
    }

    // Discovery callback
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "Endpoint found: ${info.endpointName}")
            val devices = _discoveredDevices.value.toMutableList()
            devices.add("${info.endpointName}|$endpointId")
            _discoveredDevices.value = devices
            addMessage("Found: ${info.endpointName}")
        }

        override fun onEndpointLost(endpointId: String) {
            val devices = _discoveredDevices.value.filterNot { it.contains(endpointId) }
            _discoveredDevices.value = devices
        }
    }

    /**
     * Start as host (advertise)
     */
    fun startAsHost(name: String) {
        playerName = name
        isHost = true
        _connectionState.value = ConnectionState.Advertising
        _battleState.value = BattleState.WAITING_FOR_OPPONENT
        resetBattleState()
        addMessage("Waiting for opponent...")

        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT)
            .build()

        connectionsClient.startAdvertising(name, serviceId, connectionLifecycleCallback, options)
            .addOnSuccessListener { addMessage("Ready for battle!") }
            .addOnFailureListener { e ->
                _connectionState.value = ConnectionState.Error("Failed: ${e.message}")
            }
    }

    /**
     * Start as client (discover)
     */
    fun startAsClient(name: String) {
        playerName = name
        isHost = false
        _connectionState.value = ConnectionState.Discovering
        _battleState.value = BattleState.WAITING_FOR_OPPONENT
        resetBattleState()
        _discoveredDevices.value = emptyList()
        addMessage("Scanning for battles...")

        val options = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT)
            .build()

        connectionsClient.startDiscovery(serviceId, endpointDiscoveryCallback, options)
            .addOnSuccessListener { addMessage("Scanning...") }
            .addOnFailureListener { e ->
                _connectionState.value = ConnectionState.Error("Failed: ${e.message}")
            }
    }

    /**
     * Connect to a discovered host
     */
    fun connectToHost(endpointId: String) {
        _connectionState.value = ConnectionState.Connecting
        addMessage("Connecting...")

        connectionsClient.requestConnection(playerName, endpointId, connectionLifecycleCallback)
            .addOnFailureListener { e ->
                _connectionState.value = ConnectionState.Error("Failed: ${e.message}")
            }
    }

    // Store full card data for transfer
    private var localFullCard: Card? = null

    // Track if stats should be revealed
    private val _statsRevealed = MutableStateFlow(false)
    val statsRevealed: StateFlow<Boolean> = _statsRevealed.asStateFlow()

    /**
     * Select a card for battle
     */
    fun selectCard(card: Card) {
        val battleCard = BattleCard.fromCard(card)
        _localCard.value = battleCard
        localFullCard = card
        addMessage("Selected: ${card.name}")

        // Send card preview (image only, no stats) during selection
        // Format: CARDPREVIEW|id|name|rarity|imageUrl
        val previewData = listOf(
            "CARDPREVIEW",
            card.id.toString(),
            card.name,
            card.rarity.name,
            card.imageUrl.replace("|", "~")
        ).joinToString("|")
        sendMessage(previewData)
    }

    /**
     * Mark ready to battle
     */
    fun setReady() {
        val card = localFullCard ?: return
        val battleCard = _localCard.value ?: return

        localReady = true

        // Now send full card data (with stats) for battle
        // Format: CARD|id|name|attack|health|rarity|prompt|imageUrl|biography
        val cardData = listOf(
            "CARD",
            card.id.toString(),
            card.name,
            battleCard.effectiveAttack.toString(),
            battleCard.effectiveHealth.toString(),
            card.rarity.name,
            card.prompt.replace("|", "~"),
            card.imageUrl.replace("|", "~"),
            card.biography.replace("|", "~")
        ).joinToString("|")
        sendMessage(cardData)

        sendMessage("READY|true")
        addMessage("Ready to battle!")

        checkBothReady()
    }

    /**
     * Execute the battle (called by host when both ready)
     */
    fun executeBattle() {
        val local = _localCard.value ?: return
        val opponent = _opponentCard.value ?: return

        _battleState.value = BattleState.BATTLE_IN_PROGRESS

        // Generate battle story and calculate outcome
        val (story, result) = calculateBattle(local, opponent)

        _battleStory.value = story

        // Send story segments to opponent for synchronized animation
        // Format: STORY|index|text|isLocalAction|damage
        story.forEachIndexed { index, segment ->
            val storyMsg = "STORY|$index|${segment.text.replace("|", "~")}|${segment.isLocalAction}|${segment.damageDealt ?: -1}"
            sendMessage(storyMsg)
        }

        // Update result with full card data for transfer
        val finalResult = result.copy(
            cardWon = if (!result.isDraw && result.winnerIsLocal == true) opponentFullCard else null
        )
        _battleResult.value = finalResult

        // Send result to opponent (they won if host lost)
        val resultMsg = "RESULT|${if (finalResult.isDraw) "DRAW" else if (finalResult.winnerIsLocal == true) "LOCAL" else "OPPONENT"}"
        sendMessage(resultMsg)

        _battleState.value = BattleState.BATTLE_COMPLETE
    }

    /**
     * Calculate battle outcome with damage and chance factor
     * Returns story segments and final result
     */
    private fun calculateBattle(local: BattleCard, opponent: BattleCard): Pair<List<BattleStorySegment>, BattleResult> {
        val segments = mutableListOf<BattleStorySegment>()
        var localHealth = local.effectiveHealth
        var opponentHealth = opponent.effectiveHealth

        // Opening narration
        segments.add(BattleStorySegment(
            "The arena crackles with energy as ${local.card.name} faces ${opponent.card.name}!",
            isLocalAction = false
        ))

        // Battle rounds (3-5 exchanges)
        val rounds = Random.nextInt(3, 6)
        for (round in 1..rounds) {
            // Local attacks (with 0.8-1.2 random factor)
            val localDamage = (local.effectiveAttack * Random.nextDouble(0.8, 1.2)).toInt()
            opponentHealth -= localDamage
            segments.add(BattleStorySegment(
                "${local.card.name} unleashes a devastating ${getAttackVerb()}! ${opponent.card.name} takes $localDamage damage!",
                isLocalAction = true,
                damageDealt = localDamage
            ))

            if (opponentHealth <= 0) break

            // Opponent attacks
            val opponentDamage = (opponent.effectiveAttack * Random.nextDouble(0.8, 1.2)).toInt()
            localHealth -= opponentDamage
            segments.add(BattleStorySegment(
                "${opponent.card.name} retaliates with a fierce ${getAttackVerb()}! ${local.card.name} suffers $opponentDamage damage!",
                isLocalAction = false,
                damageDealt = opponentDamage
            ))

            if (localHealth <= 0) break
        }

        // Determine outcome
        val isDraw = localHealth <= 0 && opponentHealth <= 0
        val localWins = !isDraw && localHealth > opponentHealth

        // Closing narration
        val closingText = when {
            isDraw -> "Both warriors fall simultaneously! A legendary draw that will be remembered!"
            localWins -> "${local.card.name} stands victorious! ${opponent.card.name} has been defeated!"
            else -> "${opponent.card.name} emerges triumphant! ${local.card.name} falls in battle!"
        }
        segments.add(BattleStorySegment(closingText, isLocalAction = localWins))

        val fullStory = segments.joinToString(" ") { it.text }

        val result = BattleResult(
            isDraw = isDraw,
            winnerIsLocal = if (isDraw) null else localWins,
            winnerCardName = when {
                isDraw -> null
                localWins -> local.card.name
                else -> opponent.card.name
            },
            loserCardName = when {
                isDraw -> null
                localWins -> opponent.card.name
                else -> local.card.name
            },
            localCardFinalHealth = maxOf(0, localHealth),
            opponentCardFinalHealth = maxOf(0, opponentHealth),
            battleStory = fullStory,
            cardWon = if (!isDraw && localWins) opponent.card else null
        )

        return Pair(segments, result)
    }

    private fun getAttackVerb(): String {
        val verbs = listOf(
            "strike", "blow", "assault", "barrage", "onslaught",
            "slam", "blast", "surge", "rampage", "combo"
        )
        return verbs.random()
    }

    // Store opponent's full card for potential transfer
    private var opponentFullCard: Card? = null

    private fun handleReceivedMessage(message: String) {
        val parts = message.split("|")
        when (parts[0]) {
            "CARDPREVIEW" -> {
                // CARDPREVIEW|id|name|rarity|imageUrl - no stats yet
                val id = parts[1].toLongOrNull() ?: 0
                val name = parts[2]
                val rarity = try { CardRarity.valueOf(parts[3]) } catch (e: Exception) { CardRarity.COMMON }
                val imageUrl = parts.getOrNull(4)?.replace("~", "|") ?: ""

                // Create preview card (stats hidden)
                val previewCard = Card(
                    id = id,
                    prompt = "",
                    imageUrl = imageUrl,
                    rarity = rarity,
                    name = name,
                    attack = 0, // Hidden
                    health = 0, // Hidden
                    biography = ""
                )
                _opponentCard.value = BattleCard(
                    card = previewCard,
                    effectiveAttack = 0, // Hidden
                    effectiveHealth = 0, // Hidden
                    currentHealth = 0
                )
                addMessage("Opponent selected their card")
            }
            "CARD" -> {
                // CARD|id|name|attack|health|rarity|prompt|imageUrl|biography - full stats
                val id = parts[1].toLongOrNull() ?: 0
                val name = parts[2]
                val attack = parts[3].toIntOrNull() ?: 50
                val health = parts[4].toIntOrNull() ?: 100
                val rarity = try { CardRarity.valueOf(parts[5]) } catch (e: Exception) { CardRarity.COMMON }
                val prompt = parts.getOrNull(6)?.replace("~", "|") ?: ""
                val imageUrl = parts.getOrNull(7)?.replace("~", "|") ?: ""
                val biography = parts.getOrNull(8)?.replace("~", "|") ?: ""

                // Create full card for potential transfer
                val opponentCardData = Card(
                    id = id,
                    prompt = prompt,
                    imageUrl = imageUrl,
                    rarity = rarity,
                    name = name,
                    attack = attack,
                    health = health,
                    biography = biography
                )
                opponentFullCard = opponentCardData
                _opponentCard.value = BattleCard(
                    card = opponentCardData,
                    effectiveAttack = attack,
                    effectiveHealth = health,
                    currentHealth = health
                )
                _statsRevealed.value = true
                addMessage("Opponent revealed: $name (ATK:$attack HP:$health)")
            }
            "READY" -> {
                opponentReady = parts[1] == "true"
                addMessage("Opponent is ready!")
                checkBothReady()
            }
            "STORY" -> {
                // Receive story segment from host
                // STORY|index|text|isLocalAction|damage
                _battleState.value = BattleState.BATTLE_IN_PROGRESS
                val index = parts[1].toIntOrNull() ?: 0
                val text = parts[2].replace("~", "|")
                // Flip isLocalAction for non-host (their local is host's opponent)
                val isLocalAction = parts[3] != "true"  // Inverted!
                val damage = parts[4].toIntOrNull()?.takeIf { it >= 0 }

                val segment = BattleStorySegment(text, isLocalAction, damage)
                val currentStory = _battleStory.value.toMutableList()
                // Ensure we add at correct index
                while (currentStory.size <= index) {
                    currentStory.add(segment)
                }
                if (currentStory.size > index) {
                    currentStory[index] = segment
                }
                _battleStory.value = currentStory
            }
            "RESULT" -> {
                // Non-host receives result
                val outcome = parts[1]

                val local = _localCard.value ?: return
                val opponent = _opponentCard.value ?: return

                val isDraw = outcome == "DRAW"
                val localWins = outcome == "OPPONENT" // Flip perspective for non-host

                val fullStory = _battleStory.value.joinToString(" ") { it.text }

                _battleResult.value = BattleResult(
                    isDraw = isDraw,
                    winnerIsLocal = if (isDraw) null else localWins,
                    winnerCardName = if (isDraw) null else if (localWins) local.card.name else opponent.card.name,
                    loserCardName = if (isDraw) null else if (localWins) opponent.card.name else local.card.name,
                    localCardFinalHealth = 0,
                    opponentCardFinalHealth = 0,
                    battleStory = fullStory,
                    cardWon = if (!isDraw && localWins) opponentFullCard else null
                )
                _battleState.value = BattleState.BATTLE_COMPLETE
            }
        }
    }

    private fun checkBothReady() {
        if (localReady && opponentReady && _localCard.value != null && _opponentCard.value != null) {
            _battleState.value = BattleState.READY_TO_BATTLE
            addMessage("Both players ready!")

            // Host executes battle
            if (isHost) {
                executeBattle()
            }
        }
    }

    private fun sendMessage(message: String) {
        currentEndpointId?.let { endpointId ->
            val payload = Payload.fromBytes(message.toByteArray(Charsets.UTF_8))
            connectionsClient.sendPayload(endpointId, payload)
            Log.d(TAG, "Sent: $message")
        }
    }

    private fun resetBattleState() {
        _localCard.value = null
        _opponentCard.value = null
        _battleStory.value = emptyList()
        _battleResult.value = null
        _messages.value = emptyList()
        localReady = false
        opponentReady = false
        localFullCard = null
        opponentFullCard = null
        _statsRevealed.value = false
    }

    fun stopAll() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        _connectionState.value = ConnectionState.Idle
        _battleState.value = BattleState.WAITING_FOR_OPPONENT
        _discoveredDevices.value = emptyList()
        currentEndpointId = null
        resetBattleState()
    }

    private fun addMessage(message: String) {
        val messages = _messages.value.toMutableList()
        messages.add(0, message)
        _messages.value = messages.take(20)
    }

    companion object {
        private const val TAG = "BattleManager"
    }
}
