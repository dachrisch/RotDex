package com.rotdex.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.rotdex.ui.components.RotDexLogo
import com.rotdex.ui.viewmodel.DailyRewardsViewModel

/**
 * Home screen with main navigation options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDailyRewards: () -> Unit,
    onNavigateToCollection: () -> Unit,
    onNavigateToCardCreate: () -> Unit,
    onNavigateToFusion: () -> Unit = {},
    onNavigateToAchievements: () -> Unit = {},
    onNavigateToConnectionTest: () -> Unit = {},
    onNavigateToBattleArena: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: DailyRewardsViewModel = hiltViewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    RotDexLogo(
                        userProfile = userProfile,
                        onAvatarClick = onNavigateToSettings
                    )
                },
                actions = {
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Section
            Text(
                text = "COLLECT THE CHAOS 💥",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Stats moved to app bar - removed card

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Buttons
            NavigationButton(
                icon = Icons.Default.CardGiftcard,
                emoji = "🎁",
                title = "FREE STUFF TIME",
                onClick = onNavigateToDailyRewards
            )

            NavigationButton(
                icon = Icons.Default.Collections,
                emoji = "📚",
                title = "YOUR DECK",
                onClick = onNavigateToCollection
            )

            NavigationButton(
                icon = Icons.Default.Create,
                emoji = "✨",
                title = "COOK UP SOME HEAT",
                onClick = onNavigateToCardCreate
            )

            NavigationButton(
                icon = Icons.Default.AutoFixHigh,
                emoji = "⚗️",
                title = "THE BLENDER",
                onClick = onNavigateToFusion
            )

            NavigationButton(
                icon = Icons.Default.EmojiEvents,
                emoji = "🏆",
                title = "ACHIEVEMENTS",
                onClick = onNavigateToAchievements
            )

            NavigationButton(
                icon = Icons.Default.LocalFireDepartment,
                emoji = "⚔️",
                title = "BATTLE ARENA",
                onClick = onNavigateToBattleArena
            )

            // Temporary connection test button
            NavigationButton(
                icon = Icons.Default.Bluetooth,
                emoji = "🧪",
                title = "CONNECTION TEST",
                onClick = onNavigateToConnectionTest
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    emoji: String,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Emoji icon
            Text(
                text = emoji,
                fontSize = 40.sp
            )
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
