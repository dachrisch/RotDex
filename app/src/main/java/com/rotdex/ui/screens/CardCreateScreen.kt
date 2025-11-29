package com.rotdex.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.rotdex.data.models.GameConfig
import com.rotdex.ui.components.CardDisplayMode
import com.rotdex.ui.components.FallingIconData
import com.rotdex.ui.components.FallingIconsContainer
import com.rotdex.ui.components.RotDexLogo
import com.rotdex.ui.components.StyledCardView
import com.rotdex.ui.utils.ActionVerbs
import com.rotdex.ui.viewmodel.CardCreateViewModel
import com.rotdex.ui.viewmodel.CardGenerationState
import java.io.File
import java.util.UUID
import kotlinx.coroutines.delay

/**
 * Screen for creating/generating new cards
 * Shows energy cost and handles insufficient energy gracefully
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardCreateScreen(
    viewModel: CardCreateViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToCollection: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val generationState by viewModel.cardGenerationState.collectAsState()
    var promptText by remember { mutableStateOf("") }

    // State for falling icon animations
    var fallingIcons by remember { mutableStateOf<List<FallingIconData>>(emptyList()) }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { RotDexLogo() },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
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
        // Show only generation animation when generating
        if (generationState is CardGenerationState.Generating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                FullScreenGeneratingAnimation()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Energy Status Card
                userProfile?.let { profile ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (profile.currentEnergy >= GameConfig.CARD_GENERATION_ENERGY_COST) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Your Energy",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "⚡",
                                        fontSize = 24.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${profile.currentEnergy}/${profile.maxEnergy}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Cost per card",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "⚡",
                                        fontSize = 24.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${GameConfig.CARD_GENERATION_ENERGY_COST}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Prompt Input Section
                Text(
                    text = "WHAT CARD U COOKIN? ✨",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )

                // Calculate extra coin cost
                val charCount = promptText.length
                val freeCharLimit = 20
                val extraChars = (charCount - freeCharLimit).coerceAtLeast(0)
                val coinCost = (extraChars + 9) / 10 // Ceiling division: 1 coin per 10 chars

                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Drop your idea here") },
                    placeholder = { Text("e.g., sigma wizard with drip") },
                    minLines = 3,
                    maxLines = 5,
                    enabled = generationState !is CardGenerationState.Generating,
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$charCount chars",
                                color = if (charCount > freeCharLimit) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )

                            if (charCount > freeCharLimit) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🪙 +$coinCost",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                )

                // Visual coin cost card (when over limit)
                if (coinCost > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Extra spice 🌶️",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "$extraChars chars over free limit ($freeCharLimit free)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🪙",
                                    fontSize = 24.sp
                                )
                                Text(
                                    text = "$coinCost",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Generate Button
                Button(
                    onClick = {
                        // Trigger falling energy icon animation
                        fallingIcons = fallingIcons + FallingIconData(
                            id = UUID.randomUUID().toString(),
                            icon = "⚡",
                            amount = -GameConfig.CARD_GENERATION_ENERGY_COST,
                            startOffset = 0.dp,
                            startX = null
                        )

                        // Trigger falling coin icon animation for long prompts
                        if (coinCost > 0) {
                            fallingIcons = fallingIcons + FallingIconData(
                                id = UUID.randomUUID().toString(),
                                icon = "🪙",
                                amount = -coinCost,
                                startOffset = 2.dp,
                                startX = (-40).dp
                            )
                        }

                        viewModel.generateCard(promptText, coinCost)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = viewModel.hasEnoughEnergy() &&
                             promptText.isNotBlank() &&
                             generationState !is CardGenerationState.Generating &&
                             (coinCost == 0 || (userProfile?.brainrotCoins ?: 0) >= coinCost)
                ) {
                    Text(
                        text = if (coinCost > 0) {
                            "⚡ LET'S GOOOO! (-${GameConfig.CARD_GENERATION_ENERGY_COST} ⚡ -$coinCost 🪙)"
                        } else {
                            "⚡ LET'S GOOOO! (-${GameConfig.CARD_GENERATION_ENERGY_COST} ⚡)"
                        },
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // State Messages
                when (val state = generationState) {
                    is CardGenerationState.InsufficientEnergy -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "⚠️ Not Enough Energy",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "You need ${state.required} energy but only have ${state.current}",
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "Energy regenerates every 4 hours",
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    is CardGenerationState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "❌ Error",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = state.message,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                                Button(
                                    onClick = { viewModel.resetState() }
                                ) {
                                    Text("Try Again")
                                }
                            }
                        }
                    }

                    else -> { /* Idle - no extra message */ }
                }

                // Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "💡 Tips",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Be specific and creative with your prompts",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "• Energy regenerates automatically over time",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "• Higher rarity cards are more valuable",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }

    // Full-screen card reveal overlay
    if (generationState is CardGenerationState.Success) {
        FullScreenCardReveal(
            card = (generationState as CardGenerationState.Success).card,
            onViewCollection = onNavigateToCollection,
            onCreateAnother = {
                promptText = ""
                viewModel.resetState()
            }
        )
    }

    // Falling icon animations
    FallingIconsContainer(
        animations = fallingIcons,
        onAnimationComplete = { id ->
            fallingIcons = fallingIcons.filter { it.id != id }
        },
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 16.dp, top = 16.dp)
    )
    }
}

/**
 * Full-screen animation displayed while card is being generated
 * Takes up entire content area, centered and prominent
 */
@Composable
fun FullScreenGeneratingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "generating")

    // Rotating animation for outer ring
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulsing animation for size
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Shimmer alpha animation
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Animated status messages using playful action verbs
    var messageIndex by remember { mutableIntStateOf(0) }
    val messages = remember { ActionVerbs.getGenerationVerbCycle(6) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1500) // Faster cycle for more chaos
            messageIndex = (messageIndex + 1) % messages.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated generation visual - larger than inline version
        Box(
            modifier = Modifier.size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer rotating ring
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .rotate(rotation)
                    .border(
                        width = 6.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.tertiary,
                                MaterialTheme.colorScheme.primary
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Pulsing center
            Box(
                modifier = Modifier
                    .size(140.dp * scale)
                    .alpha(alpha)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier
                        .size(70.dp)
                        .rotate(-rotation),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Orbiting sparkles
            for (i in 0..2) {
                val angle = rotation + (i * 120f)
                val offsetX = (125 * kotlin.math.cos(Math.toRadians(angle.toDouble()))).toFloat()
                val offsetY = (125 * kotlin.math.sin(Math.toRadians(angle.toDouble()))).toFloat()
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier
                        .offset(x = offsetX.dp, y = offsetY.dp)
                        .size(32.dp)
                        .alpha(alpha),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Animated status text
        AnimatedContent(
            targetState = messages[messageIndex],
            transitionSpec = {
                fadeIn(animationSpec = tween(500)) + slideInVertically { it / 2 } togetherWith
                        fadeOut(animationSpec = tween(500)) + slideOutVertically { -it / 2 }
            },
            label = "message"
        ) { message ->
            Text(
                text = message,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

    }
}

/**
 * Engaging animation displayed while card is being generated
 * Shows inline as a card (not fullscreen)
 */
@Composable
fun GeneratingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "generating")

    // Rotating animation for outer ring
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulsing animation for size
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Shimmer alpha animation
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Animated status messages using playful action verbs
    var messageIndex by remember { mutableIntStateOf(0) }
    val messages = remember { ActionVerbs.getGenerationVerbCycle(6) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1500) // Faster cycle for more chaos
            messageIndex = (messageIndex + 1) % messages.size
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Animated generation visual
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer rotating ring
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .rotate(rotation)
                        .border(
                            width = 4.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.tertiary,
                                    MaterialTheme.colorScheme.primary
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Pulsing center
                Box(
                    modifier = Modifier
                        .size(90.dp * scale)
                        .alpha(alpha)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier
                            .size(45.dp)
                            .rotate(-rotation),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // Orbiting sparkles
                for (i in 0..2) {
                    val angle = rotation + (i * 120f)
                    val offsetX = (75 * kotlin.math.cos(Math.toRadians(angle.toDouble()))).toFloat()
                    val offsetY = (75 * kotlin.math.sin(Math.toRadians(angle.toDouble()))).toFloat()
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier
                            .offset(x = offsetX.dp, y = offsetY.dp)
                            .size(24.dp)
                            .alpha(alpha),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Animated status text
            AnimatedContent(
                targetState = messages[messageIndex],
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) + slideInVertically { it / 2 } togetherWith
                            fadeOut(animationSpec = tween(500)) + slideOutVertically { -it / 2 }
                },
                label = "message"
            ) { message ->
                Text(
                    text = message,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Card reveal animation with splash effect
 */
@Composable
fun AnimatedCardReveal(
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100) // Small delay before animation starts
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(600, easing = FastOutSlowInEasing)
        ) + scaleIn(
            initialScale = 0.3f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + slideInVertically(
            initialOffsetY = { -it / 3 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    ) {
        content()
    }
}

/**
 * Full-screen card reveal splash
 */
@Composable
fun FullScreenCardReveal(
    card: com.rotdex.data.models.Card,
    onViewCollection: () -> Unit,
    onCreateAnother: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(800, easing = FastOutSlowInEasing)
        ) + scaleIn(
            initialScale = 0.5f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
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
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Success message
                Text(
                    text = "YOOO IT'S READY! ✨",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )

                // Card display using StyledCardView
                StyledCardView(
                    card = card,
                    displayMode = CardDisplayMode.FULL,
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(0.9f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCreateAnother,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("COOK ANOTHER", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onViewCollection,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SEE MY DECK", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
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
