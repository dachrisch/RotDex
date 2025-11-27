package com.rotdex.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.rotdex.data.manager.ConnectionState
import com.rotdex.data.models.*
import com.rotdex.ui.viewmodel.BattleArenaViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleArenaScreen(
    onNavigateBack: () -> Unit,
    viewModel: BattleArenaViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val battleState by viewModel.battleState.collectAsState()
    val localCard by viewModel.localCard.collectAsState()
    val opponentCard by viewModel.opponentCard.collectAsState()
    val battleStory by viewModel.battleStory.collectAsState()
    val currentStoryIndex by viewModel.currentStoryIndex.collectAsState()
    val battleResult by viewModel.battleResult.collectAsState()
    val cardTransferred by viewModel.cardTransferred.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val playerCards by viewModel.playerCards.collectAsState()
    val playerName by viewModel.playerName.collectAsState()

    // Ready states for new components
    val localReady by viewModel.localReady.collectAsState()
    val opponentReady by viewModel.opponentReady.collectAsState()
    val canClickReady by viewModel.canClickReady.collectAsState()
    val opponentIsThinking by viewModel.opponentIsThinking.collectAsState()
    val shouldRevealCards by viewModel.shouldRevealCards.collectAsState()

    // Process card transfer when battle completes
    LaunchedEffect(battleResult) {
        if (battleResult != null && !cardTransferred) {
            viewModel.processCardTransfer()
        }
    }

    var hasPermissions by remember { mutableStateOf(false) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
    }

    // Request permissions on launch
    LaunchedEffect(Unit) {
        val permissions = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.NEARBY_WIFI_DEVICES
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                arrayOf(
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
            else -> {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
        }
        permissionLauncher.launch(permissions)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopAll() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Battle Arena") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopAll()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission warning
            if (!hasPermissions) {
                PermissionWarningCard()
            }

            when {
                // Lobby phase - Auto-discovery
                connectionState is ConnectionState.Idle ||
                connectionState is ConnectionState.AutoDiscovering -> {
                    // Auto-start discovery when permissions are ready
                    LaunchedEffect(hasPermissions) {
                        if (hasPermissions && playerName.isNotEmpty() && connectionState is ConnectionState.Idle) {
                            viewModel.startAutoDiscovery()
                        }
                    }

                    // Show discovered devices as bubbles
                    com.rotdex.ui.components.DiscoveryBubblesSection(
                        discoveredDevices = discoveredDevices,
                        onDeviceClick = { device ->
                            val endpointId = device.split("|").last()
                            viewModel.connectToDevice(endpointId)
                        }
                    )
                }

                // Waiting/Discovering
                connectionState is ConnectionState.Advertising ||
                connectionState is ConnectionState.Discovering ||
                connectionState is ConnectionState.Connecting -> {
                    WaitingSection(
                        connectionState = connectionState,
                        discoveredDevices = discoveredDevices,
                        onDeviceClick = { viewModel.connectToHost(it) },
                        onCancel = { viewModel.stopAll() }
                    )
                }

                // Connected - Card Selection
                connectionState is ConnectionState.Connected &&
                battleState == BattleState.CARD_SELECTION -> {
                    CardSelectionSection(
                        playerCards = playerCards,
                        selectedCard = localCard,
                        opponentCard = opponentCard,
                        localReady = localReady,
                        opponentReady = opponentReady,
                        canClickReady = canClickReady,
                        opponentIsThinking = opponentIsThinking,
                        shouldRevealCards = shouldRevealCards,
                        onCardSelect = { viewModel.selectCard(it) },
                        onReady = { viewModel.setReady() }
                    )
                }

                // Battle animation - full screen
                battleState == BattleState.BATTLE_ANIMATING &&
                localCard != null &&
                opponentCard != null &&
                battleStory.isNotEmpty() -> {
                    BattlePrimaryAnimationScreen(
                        localCard = localCard!!,
                        opponentCard = opponentCard!!,
                        battleStory = battleStory,
                        currentStoryIndex = currentStoryIndex,
                        onSkip = { viewModel.skipBattleAnimation() }
                    )
                }

                // Battle in progress or complete (legacy/fallback view)
                battleState == BattleState.READY_TO_BATTLE ||
                battleState == BattleState.BATTLE_IN_PROGRESS ||
                battleState == BattleState.BATTLE_COMPLETE -> {
                    BattleSection(
                        localCard = localCard,
                        opponentCard = opponentCard,
                        battleStory = battleStory,
                        currentStoryIndex = currentStoryIndex,
                        battleResult = battleResult,
                        cardTransferred = cardTransferred,
                        onPlayAgain = { viewModel.stopAll() }
                    )
                }

                // Disconnected
                battleState == BattleState.DISCONNECTED -> {
                    DisconnectedSection(onRetry = { viewModel.stopAll() })
                }
            }
        }
    }
}

@Composable
private fun PermissionWarningCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Please grant Bluetooth and Location permissions",
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WaitingSection(
    connectionState: ConnectionState,
    discoveredDevices: List<String>,
    onDeviceClick: (String) -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()

            Text(
                text = when (connectionState) {
                    is ConnectionState.Advertising -> "Waiting for opponent..."
                    is ConnectionState.Discovering -> "Scanning for battles..."
                    is ConnectionState.Connecting -> "Connecting..."
                    else -> "Please wait..."
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Show discovered devices
            if (discoveredDevices.isNotEmpty()) {
                Text("Available Battles:", fontWeight = FontWeight.Bold)
                discoveredDevices.forEach { device ->
                    val name = device.split("|").firstOrNull() ?: device
                    Button(
                        onClick = { onDeviceClick(device) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Join: $name")
                    }
                }
            }

            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun CardSelectionSection(
    playerCards: List<Card>,
    selectedCard: BattleCard?,
    opponentCard: BattleCard?,
    localReady: Boolean,
    opponentReady: Boolean,
    canClickReady: Boolean,
    opponentIsThinking: Boolean,
    shouldRevealCards: Boolean,
    onCardSelect: (Card) -> Unit,
    onReady: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SELECT YOUR CHAMPION",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Battle Ready Status
        com.rotdex.ui.components.BattleReadyStatus(
            localReady = localReady,
            opponentReady = opponentReady,
            opponentIsThinking = opponentIsThinking
        )

        // Selected card preview
        if (selectedCard != null) {
            SelectedCardPreview(battleCard = selectedCard)
        }

        // Opponent card preview - Use BlurredCardReveal
        opponentCard?.let { card ->
            com.rotdex.ui.components.BlurredCardReveal(
                battleCard = card,
                isRevealed = shouldRevealCards
            )
        }

        // Card selection grid
        Text(
            text = "Your Cards:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(playerCards) { card ->
                SelectableCardItem(
                    card = card,
                    isSelected = selectedCard?.card?.id == card.id,
                    onClick = { onCardSelect(card) }
                )
            }
        }

        // Ready button - Use ReadyButton
        com.rotdex.ui.components.ReadyButton(
            localCard = selectedCard,
            localReady = localReady,
            opponentReady = opponentReady,
            canClick = canClickReady,
            onReady = onReady
        )
    }
}

@Composable
private fun SelectedCardPreview(battleCard: BattleCard) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = getRarityColor(battleCard.card.rarity).copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(battleCard.card.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = battleCard.card.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = battleCard.card.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = battleCard.card.rarity.displayName,
                    color = getRarityColor(battleCard.card.rarity)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("ATK: ${battleCard.effectiveAttack}", fontWeight = FontWeight.Bold)
                Text("HP: ${battleCard.effectiveHealth}", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SelectableCardItem(
    card: Card,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(
                    3.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(card.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = card.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = card.name.take(12),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                text = "ATK:${card.attack} HP:${card.health}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
internal fun BattleSection(
    localCard: BattleCard?,
    opponentCard: BattleCard?,
    battleStory: List<BattleStorySegment>,
    currentStoryIndex: Int,
    battleResult: BattleResult?,
    cardTransferred: Boolean,
    onPlayAgain: () -> Unit
) {
    // Track which card is currently attacking for animation
    val currentSegment = battleStory.getOrNull(currentStoryIndex)
    val isLocalAttacking = currentSegment?.isLocalAction == true
    val isOpponentAttacking = currentSegment?.isLocalAction == false && currentSegment.damageDealt != null
    val isBattleActive = battleResult == null && battleStory.isNotEmpty()

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // VS Display with animated cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Local card with attack animation
            localCard?.let {
                AnimatedBattleCard(
                    battleCard = it,
                    label = "YOU",
                    isAttacking = isLocalAttacking && isBattleActive,
                    isTakingDamage = isOpponentAttacking && isBattleActive,
                    isWinner = battleResult?.winnerIsLocal == true,
                    isLoser = battleResult?.winnerIsLocal == false,
                    isDraw = battleResult?.isDraw == true
                )
            }

            // Animated VS text
            AnimatedVsText(isBattleActive = isBattleActive)

            // Opponent card with attack animation
            opponentCard?.let {
                AnimatedBattleCard(
                    battleCard = it,
                    label = "FOE",
                    isAttacking = isOpponentAttacking && isBattleActive,
                    isTakingDamage = isLocalAttacking && isBattleActive,
                    isWinner = battleResult?.winnerIsLocal == false,
                    isLoser = battleResult?.winnerIsLocal == true,
                    isDraw = battleResult?.isDraw == true
                )
            }
        }

        // Result display with animation
        battleResult?.let { result ->
            AnimatedResultCard(
                result = result,
                cardTransferred = cardTransferred,
                onPlayAgain = onPlayAgain
            )
        }
    }
}

@Composable
private fun AnimatedVsText(isBattleActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "vs")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isBattleActive) 1.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vsScale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vsRotation"
    )

    Text(
        text = "VS",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .scale(scale)
            .graphicsLayer {
                rotationZ = if (isBattleActive) rotation else 0f
            }
    )
}

@Composable
internal fun AnimatedBattleCard(
    battleCard: BattleCard,
    label: String,
    isAttacking: Boolean,
    isTakingDamage: Boolean,
    isWinner: Boolean,
    isLoser: Boolean,
    isDraw: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "battle")

    // Attack animation - lunge forward
    val attackOffset by animateFloatAsState(
        targetValue = if (isAttacking) 20f else 0f,
        animationSpec = tween(200, easing = EaseOutBack),
        label = "attackOffset"
    )

    // Damage animation - shake
    val shakeOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(50),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )

    // Damage flash
    var showDamageFlash by remember { mutableStateOf(false) }
    LaunchedEffect(isTakingDamage) {
        if (isTakingDamage) {
            showDamageFlash = true
            delay(300)
            showDamageFlash = false
        }
    }

    // Winner pulse animation
    val winnerScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "winnerScale"
    )

    // Loser fade
    val loserAlpha by animateFloatAsState(
        targetValue = if (isLoser) 0.4f else 1f,
        animationSpec = tween(1000),
        label = "loserAlpha"
    )

    val cardModifier = Modifier
        .graphicsLayer {
            translationX = when {
                isAttacking -> attackOffset * (if (label == "YOU") 1 else -1)
                isTakingDamage -> shakeOffset
                else -> 0f
            }
            scaleX = if (isWinner) winnerScale else 1f
            scaleY = if (isWinner) winnerScale else 1f
            alpha = loserAlpha
        }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = cardModifier
    ) {
        // Label with winner/loser indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            if (isWinner) {
                Text("👑", fontSize = 16.sp)
            } else if (isLoser) {
                Text("💀", fontSize = 16.sp)
            } else if (isDraw) {
                Text("🤝", fontSize = 16.sp)
            }
        }

        // Card with effects
        Box {
            Card(
                modifier = Modifier.size(110.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isWinner -> Color(0xFF4CAF50).copy(alpha = 0.4f)
                        isLoser -> Color(0xFFF44336).copy(alpha = 0.3f)
                        else -> getRarityColor(battleCard.card.rarity).copy(alpha = 0.3f)
                    }
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(battleCard.card.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = battleCard.card.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Damage flash overlay
                    if (showDamageFlash) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Red.copy(alpha = 0.5f))
                        )
                    }

                    // Winner glow
                    if (isWinner) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFFFD700).copy(alpha = 0.2f))
                        )
                    }
                }
            }

            // Health bar
            HealthBar(
                currentHealth = battleCard.currentHealth,
                maxHealth = battleCard.effectiveHealth,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(4.dp)
            )
        }

        Text(
            text = battleCard.card.name.take(10),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "⚔️${battleCard.effectiveAttack}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "❤️${battleCard.effectiveHealth}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HealthBar(
    currentHealth: Int,
    maxHealth: Int,
    modifier: Modifier = Modifier
) {
    val healthPercent = if (maxHealth > 0) currentHealth.toFloat() / maxHealth else 1f
    val healthColor = when {
        healthPercent > 0.5f -> Color(0xFF4CAF50)
        healthPercent > 0.25f -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Box(
        modifier = modifier
            .width(90.dp)
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Gray.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(healthPercent)
                .background(healthColor)
        )
    }
}

@Composable
private fun AnimatedResultCard(
    result: BattleResult,
    cardTransferred: Boolean,
    onPlayAgain: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(500)
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "resultScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = when {
                result.isDraw -> MaterialTheme.colorScheme.tertiaryContainer
                result.winnerIsLocal == true -> Color(0xFF4CAF50).copy(alpha = 0.3f)
                else -> Color(0xFFF44336).copy(alpha = 0.3f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Big result text with emoji
            Text(
                text = when {
                    result.isDraw -> "🤝 DRAW! 🤝"
                    result.winnerIsLocal == true -> "🏆 VICTORY! 🏆"
                    else -> "💀 DEFEAT! 💀"
                },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            // Card transfer info
            when {
                result.isDraw -> {
                    Text(
                        text = "Both cards lost in the chaos!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
                result.winnerIsLocal == true && result.cardWon != null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🎉 You claimed: ${result.cardWon.name}! 🎉",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50),
                            textAlign = TextAlign.Center
                        )
                        if (cardTransferred) {
                            Text(
                                text = "✅ Card added to your collection!",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                result.winnerIsLocal == false -> {
                    Text(
                        text = "😢 Your card was claimed by the victor!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFF44336),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onPlayAgain,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔄 PLAY AGAIN", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun DisconnectedSection(onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Opponent Disconnected",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Button(onClick = onRetry) {
                Text("Return to Lobby")
            }
        }
    }
}

/**
 * Typewriter text effect composable that displays text character by character
 * following the Single Responsibility Principle
 *
 * @param text The full text to display
 * @param isAnimating Whether to animate the text or show it immediately
 * @param delayPerCharMs Delay in milliseconds between each character (default: 30ms)
 * @param modifier Modifier for the text composable
 */
@Composable
fun TypewriterText(
    text: String,
    isAnimating: Boolean,
    delayPerCharMs: Long = 30L,
    modifier: Modifier = Modifier
) {
    var displayedText by remember(text, isAnimating) { mutableStateOf("") }

    LaunchedEffect(text, isAnimating) {
        if (!isAnimating) {
            // Show full text immediately when not animating
            displayedText = text
        } else {
            // Animate character by character
            displayedText = ""
            text.forEachIndexed { index, _ ->
                displayedText = text.substring(0, index + 1)
                if (index < text.length - 1) {
                    delay(delayPerCharMs)
                }
            }
        }
    }

    Text(
        text = displayedText,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier,
        textAlign = TextAlign.Center
    )
}

/**
 * Full-screen battle animation screen that shows the battle unfolding
 * with progressive story display, card animations, and battle progress
 *
 * Design follows:
 * - Single Responsibility: Focuses solely on battle animation presentation
 * - Open/Closed: Extensible through parameters without modification
 * - Dependency Inversion: Depends on abstractions (BattleCard, callbacks)
 *
 * @param localCard The player's battle card
 * @param opponentCard The opponent's battle card
 * @param battleStory List of story segments to display progressively
 * @param currentStoryIndex Current segment being displayed (0-based)
 * @param onSkip Callback when skip button is clicked
 */
@Composable
fun BattlePrimaryAnimationScreen(
    localCard: BattleCard,
    opponentCard: BattleCard,
    battleStory: List<BattleStorySegment>,
    currentStoryIndex: Int,
    onSkip: () -> Unit
) {
    val currentSegment = battleStory.getOrNull(currentStoryIndex)
    val totalSegments = battleStory.size
    val roundNumber = currentStoryIndex + 1

    // Determine which card is attacking/taking damage
    val isLocalAttacking = currentSegment?.isLocalAction == true && currentSegment.damageDealt != null
    val isOpponentAttacking = currentSegment?.isLocalAction == false && currentSegment.damageDealt != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top: Battle progress (no round numbers - continuous story flow)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⚔️ BATTLE IN PROGRESS ⚔️",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            // Progress bar (generic battle progress)
            LinearProgressIndicator(
                progress = { (currentStoryIndex.toFloat() / battleStory.size.coerceAtLeast(1).toFloat()) },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Middle: Battle cards facing off
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Local card
            AnimatedBattleCard(
                battleCard = localCard,
                label = "YOU",
                isAttacking = isLocalAttacking,
                isTakingDamage = isOpponentAttacking,
                isWinner = false,
                isLoser = false,
                isDraw = false
            )

            // VS text
            Text(
                text = "VS",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.error
            )

            // Opponent card
            AnimatedBattleCard(
                battleCard = opponentCard,
                label = "FOE",
                isAttacking = isOpponentAttacking,
                isTakingDamage = isLocalAttacking,
                isWinner = false,
                isLoser = false,
                isDraw = false
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom: Story text with typewriter effect
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Story segment with typewriter effect
                currentSegment?.let { segment ->
                    Box(
                        modifier = Modifier.weight(1f, fill = false),
                        contentAlignment = Alignment.Center
                    ) {
                        TypewriterText(
                            text = segment.text,
                            isAnimating = true,
                            delayPerCharMs = 30L,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Damage indicator
                    segment.damageDealt?.let { damage ->
                        Text(
                            text = "💥 $damage damage!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Skip button
        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "SKIP TO RESULTS ⏭️",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun getRarityColor(rarity: CardRarity): Color {
    return when (rarity) {
        CardRarity.COMMON -> Color(0xFF9E9E9E)
        CardRarity.RARE -> Color(0xFF2196F3)
        CardRarity.EPIC -> Color(0xFF9C27B0)
        CardRarity.LEGENDARY -> Color(0xFFFF9800)
    }
}
