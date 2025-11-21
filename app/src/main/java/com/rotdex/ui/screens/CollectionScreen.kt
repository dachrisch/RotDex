package com.rotdex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.rotdex.data.models.Card
import com.rotdex.data.models.CardRarity
import com.rotdex.ui.components.CardDisplayMode
import com.rotdex.ui.components.RotDexLogo
import com.rotdex.ui.components.StyledCardView
import com.rotdex.ui.theme.getColor
import com.rotdex.ui.viewmodel.CollectionViewModel
import com.rotdex.ui.viewmodel.SortOrder
import com.rotdex.utils.DateUtils
import java.io.File

/**
 * Screen displaying the user's card collection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    viewModel: CollectionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val cards by viewModel.cards.collectAsState()
    val selectedRarity by viewModel.selectedRarity.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var selectedCard by remember { mutableStateOf<Card?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { RotDexLogo() },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    // Stats
                    userProfile?.let { profile ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            CompactStatItem(icon = "⚡", value = "${profile.currentEnergy}")
                            CompactStatItem(icon = "🪙", value = "${profile.brainrotCoins}")
                            CompactStatItem(icon = "💎", value = "${profile.gems}")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Collection stats
            CollectionStatsCard(
                stats = stats,
                selectedRarity = selectedRarity,
                onRarityClick = { rarity ->
                    // Toggle: if already selected, clear filter; otherwise set filter
                    if (selectedRarity == rarity) {
                        viewModel.filterByRarity(null)
                    } else {
                        viewModel.filterByRarity(rarity)
                    }
                }
            )

            // Cards grid
            if (cards.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "🎴",
                            fontSize = 64.sp
                        )
                        Text(
                            text = if (selectedRarity != null) {
                                "No ${selectedRarity?.displayName} cards yet"
                            } else {
                                "No cards yet"
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Start creating cards to build your collection!",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cards) { card ->
                        StyledCardView(
                            card = card,
                            displayMode = CardDisplayMode.THUMBNAIL,
                            onClick = { selectedCard = card }
                        )
                    }
                }
            }
        }
    }

    // Fullscreen card viewer
    selectedCard?.let { card ->
        FullscreenCardView(
            card = card,
            onDismiss = { selectedCard = null }
        )
    }
}

/**
 * Collection statistics card with rarity-colored icons
 */
@Composable
fun CollectionStatsCard(
    stats: com.rotdex.ui.viewmodel.CollectionStats,
    selectedRarity: CardRarity?,
    onRarityClick: (CardRarity) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RarityStatBadge(
                rarity = CardRarity.COMMON,
                count = stats.commonCount,
                isSelected = selectedRarity == CardRarity.COMMON,
                onClick = { onRarityClick(CardRarity.COMMON) }
            )
            RarityStatBadge(
                rarity = CardRarity.RARE,
                count = stats.rareCount,
                isSelected = selectedRarity == CardRarity.RARE,
                onClick = { onRarityClick(CardRarity.RARE) }
            )
            RarityStatBadge(
                rarity = CardRarity.EPIC,
                count = stats.epicCount,
                isSelected = selectedRarity == CardRarity.EPIC,
                onClick = { onRarityClick(CardRarity.EPIC) }
            )
            RarityStatBadge(
                rarity = CardRarity.LEGENDARY,
                count = stats.legendaryCount,
                isSelected = selectedRarity == CardRarity.LEGENDARY,
                onClick = { onRarityClick(CardRarity.LEGENDARY) }
            )
        }
    }
}

/**
 * Rarity badge with colored circle and count
 */
@Composable
fun RarityStatBadge(
    rarity: CardRarity,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(if (isSelected) 64.dp else 56.dp)
            .clip(RoundedCornerShape(if (isSelected) 32.dp else 28.dp))
            .clickable(onClick = onClick)
            .background(
                color = rarity.getColor(),
                shape = RoundedCornerShape(if (isSelected) 32.dp else 28.dp)
            )
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(32.dp)
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Text(
            text = count.toString(),
            fontSize = if (isSelected) 22.sp else 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}

/**
 * Fullscreen card viewer
 */
@Composable
fun FullscreenCardView(
    card: Card,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Styled card view in full mode
                StyledCardView(
                    card = card,
                    displayMode = CardDisplayMode.FULL,
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(0.9f)
                )

                // Created date
                Text(
                    text = "Created ${DateUtils.formatTimestamp(card.createdAt)}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CompactStatItem(icon: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 16.sp
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
