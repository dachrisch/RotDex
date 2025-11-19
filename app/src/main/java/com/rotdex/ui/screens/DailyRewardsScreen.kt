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
import androidx.hilt.navigation.compose.hiltViewModel
import com.rotdex.data.models.*
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
                title = { Text("Daily Rewards") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
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
            // User stats
            userProfile?.let { profile ->
                UserStatsCard(profile)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Streak display
            StreakCard(
                streakState = streakState,
                currentStreak = userProfile?.currentStreak ?: 0,
                longestStreak = userProfile?.longestStreak ?: 0,
                nextMilestone = nextMilestone,
                onUseProtection = { viewModel.useStreakProtection() },
                onDeclineProtection = { viewModel.declineProtection() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Spin wheel
            SpinWheelCard(
                spinState = spinState,
                canSpin = userProfile?.let { !it.hasUsedSpinToday } ?: false,
                onSpin = { viewModel.performSpin() },
                onDismissResult = { viewModel.resetSpinState() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Upcoming milestones
            if (nextMilestone != null) {
                NextMilestoneCard(milestone = nextMilestone!!)
            }
        }
    }

    // Milestone reward dialog
    if (streakState is StreakState.StreakIncreased) {
        val state = streakState as StreakState.StreakIncreased
        if (state.milestone != null) {
            MilestoneRewardDialog(
                milestone = state.milestone,
                onDismiss = { /* Keep showing until user navigates away */ }
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
            StatItem(
                icon = Icons.Default.Favorite,
                label = "Energy",
                value = "${profile.currentEnergy}/${profile.maxEnergy}",
                color = Color(0xFFFF6B9D)
            )
            StatItem(
                icon = Icons.Default.Star,
                label = "Coins",
                value = profile.brainrotCoins.toString(),
                color = Color(0xFFFFD700)
            )
            StatItem(
                icon = Icons.Default.Star,
                label = "Gems",
                value = profile.gems.toString(),
                color = Color(0xFF4A90E2)
            )
        }
    }
}

@Composable
fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                text = "Login Streak",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
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
                                Icon(Icons.Default.Star, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Use Protection")
                            }
                            OutlinedButton(onClick = onDeclineProtection) {
                                Text("Start Fresh")
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
                text = "Daily Spin",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
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
                        Icon(Icons.Default.Star, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (canSpin) "SPIN NOW!" else "Come back tomorrow!",
                            style = MaterialTheme.typography.titleMedium
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
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = Color.Green
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Already spun today!",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Come back tomorrow for another spin",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }

                is SpinState.Error -> {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Error: ${spinState.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
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
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rewards = RewardConfig.SPIN_REWARDS
            val segmentAngle = 360f / rewards.size
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

        // Center indicator
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Spin indicator",
            modifier = Modifier.size(48.dp),
            tint = Color.Yellow
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
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = getColorForRewardType(reward.type)
        )

        Text(
            text = reward.displayName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = getColorForRewardType(reward.type)
        )

        Text(
            text = reward.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Claim Reward")
        }
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
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFFFD700)
            )
        },
        title = {
            Text(
                text = "Milestone Reached!",
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = milestone.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = milestone.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Awesome!")
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
