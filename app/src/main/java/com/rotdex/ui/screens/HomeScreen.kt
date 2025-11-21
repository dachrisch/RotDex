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
import androidx.hilt.navigation.compose.hiltViewModel
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
    viewModel: DailyRewardsViewModel = hiltViewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { RotDexLogo() },
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

            // User Stats Card
            userProfile?.let { profile ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(icon = "⚡", label = "Energy", value = "${profile.currentEnergy}/${profile.maxEnergy}")
                            StatItem(icon = "🪙", label = "Coins", value = "${profile.brainrotCoins}")
                            StatItem(icon = "💎", label = "Gems", value = "${profile.gems}")
                        }
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(icon = "🔥", label = "Streak", value = "${profile.currentStreak}")
                            StatItem(icon = "🛡️", label = "Protections", value = "${profile.streakProtections}")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Buttons
            NavigationButton(
                icon = Icons.Default.CardGiftcard,
                emoji = "🎁",
                title = "FREE STUFF TIME",
                subtitle = "Spin & keep that streak alive 🔥",
                onClick = onNavigateToDailyRewards
            )

            NavigationButton(
                icon = Icons.Default.Collections,
                emoji = "📚",
                title = "YOUR DECK",
                subtitle = "Check out your collection of chaos",
                onClick = onNavigateToCollection
            )

            NavigationButton(
                icon = Icons.Default.Create,
                emoji = "✨",
                title = "COOK UP SOME HEAT",
                subtitle = "Generate new brainrot cards fr fr",
                onClick = onNavigateToCardCreate
            )

            NavigationButton(
                icon = Icons.Default.AutoFixHigh,
                emoji = "⚗️",
                title = "THE BLENDER",
                subtitle = "Mash cards together and see what happens",
                onClick = onNavigateToFusion
            )

            NavigationButton(
                icon = Icons.Default.EmojiEvents,
                emoji = "🏆",
                title = "ACHIEVEMENTS",
                subtitle = "Flex your progress and get rewards",
                onClick = onNavigateToAchievements
            )
        }
    }
}

@Composable
private fun StatItem(icon: String, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            fontSize = 24.sp
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    emoji: String,
    title: String,
    subtitle: String,
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
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Emoji icon
            Text(
                text = emoji,
                fontSize = 48.sp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
