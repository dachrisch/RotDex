package com.rotdex.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
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
                // Lobby phase
                connectionState is ConnectionState.Idle -> {
                    LobbySection(
                        playerName = playerName,
                        onPlayerNameChange = { viewModel.setPlayerName(it) },
                        onHostBattle = { viewModel.startAsHost() },
                        onJoinBattle = { viewModel.startAsClient() },
                        hasPermissions = hasPermissions
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
                        onCardSelect = { viewModel.selectCard(it) },
                        onReady = { viewModel.setReady() }
                    )
                }

                // Battle in progress or complete
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

            // Activity log
            if (messages.isNotEmpty()) {
                ActivityLogCard(messages = messages)
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
private fun LobbySection(
    playerName: String,
    onPlayerNameChange: (String) -> Unit,
    onHostBattle: () -> Unit,
    onJoinBattle: () -> Unit,
    hasPermissions: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "BATTLE ARENA",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Challenge another player to a card battle!",
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = playerName,
                onValueChange = onPlayerNameChange,
                label = { Text("Your Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onHostBattle,
                    enabled = hasPermissions,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("HOST BATTLE", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onJoinBattle,
                    enabled = hasPermissions,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("JOIN BATTLE", fontWeight = FontWeight.Bold)
                }
            }
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

                if (opponentCard != null) {
                    Text(
                        text = "Opponent has selected their card!",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Selected card preview
        if (selectedCard != null) {
            SelectedCardPreview(battleCard = selectedCard)
        }

        // Opponent card preview (image only, no stats)
        if (opponentCard != null) {
            OpponentCardPreview(battleCard = opponentCard)
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

        // Ready button
        Button(
            onClick = onReady,
            enabled = selectedCard != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("READY TO BATTLE!", fontWeight = FontWeight.Bold)
        }
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
private fun OpponentCardPreview(battleCard: BattleCard) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
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
                contentDescription = "Opponent's card",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "OPPONENT'S CARD",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = battleCard.card.rarity.displayName,
                    color = getRarityColor(battleCard.card.rarity)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("ATK: ???", fontWeight = FontWeight.Bold)
                Text("HP: ???", fontWeight = FontWeight.Bold)
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
private fun BattleSection(
    localCard: BattleCard?,
    opponentCard: BattleCard?,
    battleStory: List<BattleStorySegment>,
    currentStoryIndex: Int,
    battleResult: BattleResult?,
    cardTransferred: Boolean,
    onPlayAgain: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // VS Display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Local card
            localCard?.let { BattleCardDisplay(it, "YOU") }

            Text(
                text = "VS",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.error
            )

            // Opponent card
            opponentCard?.let { BattleCardDisplay(it, "FOE") }
        }

        // Battle story display
        if (battleStory.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "BATTLE LOG",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))

                    battleStory.take(currentStoryIndex + 1).forEach { segment ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically()
                        ) {
                            Text(
                                text = segment.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (segment.isLocalAction)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Result display
        battleResult?.let { result ->
            ResultCard(result = result, cardTransferred = cardTransferred, onPlayAgain = onPlayAgain)
        }
    }
}

@Composable
private fun BattleCardDisplay(battleCard: BattleCard, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.size(100.dp),
            colors = CardDefaults.cardColors(
                containerColor = getRarityColor(battleCard.card.rarity).copy(alpha = 0.3f)
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
            }
        }

        Text(
            text = battleCard.card.name.take(10),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "ATK:${battleCard.effectiveAttack}",
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = "HP:${battleCard.effectiveHealth}",
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun ResultCard(result: BattleResult, cardTransferred: Boolean, onPlayAgain: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Text(
                text = when {
                    result.isDraw -> "DRAW!"
                    result.winnerIsLocal == true -> "VICTORY!"
                    else -> "DEFEAT!"
                },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold
            )

            // Card transfer info
            when {
                result.isDraw -> {
                    Text(
                        text = "Both cards lost in the chaos!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                result.winnerIsLocal == true && result.cardWon != null -> {
                    Text(
                        text = "You claimed: ${result.cardWon.name}!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    if (cardTransferred) {
                        Text(
                            text = "Card added to your collection!",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                result.winnerIsLocal == false -> {
                    Text(
                        text = "Your card was claimed by the victor!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFF44336)
                    )
                }
            }

            Button(onClick = onPlayAgain) {
                Text("PLAY AGAIN", fontWeight = FontWeight.Bold)
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

@Composable
private fun ActivityLogCard(messages: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 150.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Activity Log",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            LazyColumn {
                items(messages) { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
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
