package com.rotdex.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rotdex.data.models.*
import com.rotdex.ui.components.RotDexLogo
import com.rotdex.ui.viewmodel.DailyRewardsViewModel
import com.rotdex.ui.viewmodel.SpinState
import com.rotdex.ui.viewmodel.StreakState
import kotlin.math.cos
import kotlin.math.sin

/**
 * Main screen for daily rewards (spin wheel and streak)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyRewardsScreen(
    viewModel: DailyRewardsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val streakState by viewModel.streakState.collectAsState()
    val spinState by viewModel.spinState.collectAsState()
    val nextMilestone by viewModel.nextMilestone.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { RotDexLogo() },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Spin wheel
            SpinWheelCard(
                spinState = spinState,
                canSpin = userProfile?.let { !it.hasUsedSpinToday } ?: false,
                onSpin = { viewModel.performSpin() },
                onDismissResult = { viewModel.resetSpinState() }
            )
        }
    }
}

@Composable
fun UserStatsCard(profile: UserProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RewardStatItem(
                emoji = "⚡",
                label = "Energy",
                value = "${profile.currentEnergy}/${profile.maxEnergy}"
            )
            RewardStatItem(
                emoji = "🪙",
                label = "Coins",
                value = profile.brainrotCoins.toString()
            )
            RewardStatItem(
                emoji = "💎",
                label = "Gems",
                value = profile.gems.toString()
            )
        }
    }
}

@Composable
private fun RewardStatItem(emoji: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = emoji,
            fontSize = 32.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StreakCard(
    streakState: StreakState,
    currentStreak: Int,
    longestStreak: Int,
    nextMilestone: StreakMilestone?,
    onUseProtection: () -> Unit,
    onDeclineProtection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LOGIN STREAK 🔥",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StreakStatColumn("Current", currentStreak)
                StreakStatColumn("Best", longestStreak)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Streak state messages
            when (streakState) {
                is StreakState.Loading -> {
                    CircularProgressIndicator()
                }
                is StreakState.AlreadyCheckedIn -> {
                    Text(
                        text = "✓ Checked in today!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Green
                    )
                }
                is StreakState.StreakIncreased -> {
                    Text(
                        text = "Streak increased to ${streakState.newStreak} days! 🔥",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Green,
                        textAlign = TextAlign.Center
                    )
                }
                is StreakState.StreakBroken -> {
                    Text(
                        text = "Streak broken at ${streakState.previousStreak} days. Starting fresh!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
                is StreakState.ProtectionOffered -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Your ${streakState.currentStreak} day streak is about to break!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onUseProtection) {
                                Text("🛡️ USE PROTECTION", fontWeight = FontWeight.ExtraBold)
                            }
                            OutlinedButton(onClick = onDeclineProtection) {
                                Text("START FRESH", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                is StreakState.Error -> {
                    Text(
                        text = "Error: ${streakState.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Next milestone progress
            if (nextMilestone != null) {
                Spacer(modifier = Modifier.height(16.dp))
                val progress = currentStreak.toFloat() / nextMilestone.day
                Column {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Next: ${nextMilestone.displayName} (${nextMilestone.day - currentStreak} days)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun StreakStatColumn(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun SpinWheelCard(
    spinState: SpinState,
    canSpin: Boolean,
    onSpin: () -> Unit,
    onDismissResult: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "DAILY SPIN 🎰",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (spinState) {
                is SpinState.Idle -> {
                    SpinWheelDisplay()

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onSpin,
                        enabled = canSpin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = if (canSpin) "✨ LET'S GOOO!" else "❌ COME BACK TOMORROW!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                is SpinState.Spinning -> {
                    SpinningAnimation()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Spinning...",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                is SpinState.Result -> {
                    RewardResultDisplay(
                        reward = spinState.reward,
                        onDismiss = onDismissResult
                    )
                }

                is SpinState.AlreadySpun -> {
                    Text(
                        text = "✅",
                        fontSize = 100.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "ALREADY SPUN TODAY!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Come back tomorrow for more goodies",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }

                is SpinState.Error -> {
                    Text(
                        text = "⚠️",
                        fontSize = 64.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Error: ${spinState.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun SpinWheelDisplay() {
    Box(
        modifier = Modifier.size(250.dp),
        contentAlignment = Alignment.Center
    ) {
        val rewards = RewardConfig.SPIN_REWARDS
        val segmentAngle = 360f / rewards.size

        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2

            rewards.forEachIndexed { index, reward ->
                val startAngle = index * segmentAngle - 90f
                val color = getColorForRewardType(reward.type)

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = segmentAngle,
                    useCenter = true,
                    size = Size(radius * 2, radius * 2),
                    topLeft = Offset(
                        (size.width - radius * 2) / 2,
                        (size.height - radius * 2) / 2
                    )
                )

                // Draw border
                drawArc(
                    color = Color.White,
                    startAngle = startAngle,
                    sweepAngle = segmentAngle,
                    useCenter = true,
                    size = Size(radius * 2, radius * 2),
                    topLeft = Offset(
                        (size.width - radius * 2) / 2,
                        (size.height - radius * 2) / 2
                    ),
                    style = Stroke(width = 2f)
                )
            }
        }

        // Place emoji icons on each segment
        rewards.forEachIndexed { index, reward ->
            val angle = index * segmentAngle + segmentAngle / 2 - 90
            val radiusOffset = 80f
            val offsetX = (radiusOffset * cos(Math.toRadians(angle.toDouble()))).toFloat()
            val offsetY = (radiusOffset * sin(Math.toRadians(angle.toDouble()))).toFloat()

            Box(
                modifier = Modifier
                    .offset(x = offsetX.dp, y = offsetY.dp)
            ) {
                Text(
                    text = getEmojiForRewardType(reward.type),
                    fontSize = 24.sp
                )
            }
        }

        // Center indicator
        Text(
            text = "✨",
            fontSize = 48.sp
        )
    }
}

@Composable
fun SpinningAnimation() {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        rotation.animateTo(
            targetValue = 1800f + (0..360).random().toFloat(), // 5 rotations + random offset
            animationSpec = tween(
                durationMillis = 2500,
                easing = FastOutSlowInEasing
            )
        )
    }

    Box(
        modifier = Modifier
            .size(250.dp)
            .rotate(rotation.value)
    ) {
        SpinWheelDisplay()
    }
}

@Composable
fun RewardResultDisplay(reward: SpinReward, onDismiss: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = getEmojiForRewardType(reward.type),
            fontSize = 100.sp
        )

        Text(
            text = reward.displayName.uppercase(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = getColorForRewardType(reward.type),
            textAlign = TextAlign.Center
        )

        Text(
            text = reward.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("CLAIM IT!", fontWeight = FontWeight.ExtraBold)
        }
    }
}

fun getEmojiForRewardType(type: SpinRewardType): String {
    return when (type) {
        SpinRewardType.ENERGY -> "⚡"
        SpinRewardType.COINS -> "🪙"
        SpinRewardType.GEMS -> "💎"
        SpinRewardType.FREE_PACK -> "🎁"
        SpinRewardType.RARITY_BOOST -> "🚀"
        SpinRewardType.STREAK_PROTECTION -> "🛡️"
        SpinRewardType.JACKPOT -> "💰"
    }
}

@Composable
fun NextMilestoneCard(milestone: StreakMilestone) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Next Milestone",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = milestone.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = milestone.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = "Day ${milestone.day}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun MilestoneRewardDialog(milestone: StreakMilestone, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text(
                text = "🏆",
                fontSize = 64.sp
            )
        },
        title = {
            Text(
                text = "MILESTONE REACHED! 🎉",
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = milestone.displayName.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = milestone.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("LET'S GOOO!", fontWeight = FontWeight.ExtraBold)
            }
        }
    )
}

fun getColorForRewardType(type: SpinRewardType): Color {
    return when (type) {
        SpinRewardType.ENERGY -> Color(0xFFFF6B9D)
        SpinRewardType.COINS -> Color(0xFFFFD700)
        SpinRewardType.GEMS -> Color(0xFF4A90E2)
        SpinRewardType.FREE_PACK -> Color(0xFF9B59B6)
        SpinRewardType.RARITY_BOOST -> Color(0xFFFF6B6B)
        SpinRewardType.STREAK_PROTECTION -> Color(0xFF4ECDC4)
        SpinRewardType.JACKPOT -> Color(0xFFFF1744)
    }
}
