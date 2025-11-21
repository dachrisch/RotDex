package com.rotdex.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.rotdex.R
import com.rotdex.data.models.*
import com.rotdex.ui.components.CardDisplayMode
import com.rotdex.ui.components.FallingIconData
import com.rotdex.ui.components.FallingIconsContainer
import com.rotdex.ui.components.RotDexLogo
import com.rotdex.ui.components.StyledCardView
import com.rotdex.ui.theme.getColor
import com.rotdex.ui.utils.ActionVerbs
import com.rotdex.ui.viewmodel.FusionState
import com.rotdex.ui.viewmodel.FusionViewModel
import java.io.File
import java.util.UUID
import kotlinx.coroutines.delay

/**
 * Main fusion screen for combining cards
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FusionScreen(
    viewModel: FusionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToCollection: () -> Unit
) {
    val allCards by viewModel.allCards.collectAsState()
    val selectedCards by viewModel.selectedCards.collectAsState()
    val fusionState by viewModel.fusionState.collectAsState()
    val validation by viewModel.validation.collectAsState()
    val matchingRecipe by viewModel.matchingRecipe.collectAsState()
    val publicRecipes by viewModel.publicRecipes.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val fusionCost by viewModel.fusionCost.collectAsState()

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (fusionState) {
                is FusionState.Idle -> {
                    FusionMainContent(
                        allCards = allCards,
                        selectedCards = selectedCards,
                        validation = validation,
                        matchingRecipe = matchingRecipe,
                        publicRecipes = publicRecipes,
                        fusionCost = fusionCost,
                        onCardClick = { viewModel.toggleCardSelection(it) },
                        onFuse = {
                            // Trigger falling coin icon animation
                            fallingIcons = fallingIcons + FallingIconData(
                                id = UUID.randomUUID().toString(),
                                icon = "🪙",
                                startOffset = 0.dp
                            )
                            viewModel.performFusion()
                        },
                        onClearSelection = { viewModel.clearSelection() },
                        onRecipeSelect = { viewModel.selectCardsForRecipe(it) }
                    )
                }

                is FusionState.Fusing -> {
                    FusionAnimation()
                }

                is FusionState.Result -> {
                    FusionResultScreen(
                        result = (fusionState as FusionState.Result).fusionResult,
                        onFuseAgain = { viewModel.resetFusionState() },
                        onViewCollection = onNavigateToCollection
                    )
                }

                is FusionState.Failed -> {
                    FusionFailedScreen(
                        onTryAgain = { viewModel.resetFusionState() }
                    )
                }

                is FusionState.Error -> {
                    ErrorScreen(
                        message = (fusionState as FusionState.Error).message,
                        onDismiss = { viewModel.resetFusionState() }
                    )
                }
            }
        }
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
 * Main content for fusion screen
 */
@Composable
private fun FusionMainContent(
    allCards: List<Card>,
    selectedCards: List<Card>,
    validation: FusionValidation?,
    matchingRecipe: FusionRecipe?,
    publicRecipes: List<FusionRecipe>,
    fusionCost: Int,
    onCardClick: (Card) -> Unit,
    onFuse: () -> Unit,
    onClearSelection: () -> Unit,
    onRecipeSelect: (FusionRecipe) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Selected cards display
        SelectedCardsDisplay(
            selectedCards = selectedCards,
            validation = validation,
            matchingRecipe = matchingRecipe,
            fusionCost = fusionCost,
            onFuse = onFuse,
            onClear = onClearSelection
        )

        // Recipe suggestions
        if (publicRecipes.isNotEmpty()) {
            RecipeSuggestions(
                recipes = publicRecipes,
                onRecipeSelect = onRecipeSelect
            )
        }

        // Card selection grid
        Text(
            text = "Select Cards to Fuse (${selectedCards.size}/${FusionRules.MAX_FUSION_CARDS})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (allCards.isEmpty()) {
            EmptyCardMessage()
        } else {
            CardSelectionGrid(
                cards = allCards,
                selectedCards = selectedCards,
                onCardClick = onCardClick
            )
        }
    }
}

/**
 * Display selected cards and fusion button
 */
@Composable
private fun SelectedCardsDisplay(
    selectedCards: List<Card>,
    validation: FusionValidation?,
    matchingRecipe: FusionRecipe?,
    fusionCost: Int,
    onFuse: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "THE BLENDER 🌀",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )

            // Selected cards slots
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(FusionRules.MAX_FUSION_CARDS) { index ->
                    FusionCardSlot(
                        card = selectedCards.getOrNull(index),
                        index = index
                    )
                }
            }

            // Matching recipe indicator
            matchingRecipe?.let {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Recipe",
                            tint = Color(0xFFFFD700)
                        )
                        Text(
                            text = "${it.name} RECIPE UNLOCKED!",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Validation display
            validation?.let {
                when (it) {
                    is FusionValidation.Valid -> {
                        Text(
                            text = "VIBE CHECK: ${(it.successRate * 100).toInt()}% 💯",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    is FusionValidation.Error -> {
                        Text(
                            text = it.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Clear button - icon only
                OutlinedButton(
                    onClick = onClear,
                    enabled = selectedCards.isNotEmpty(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(24.dp))
                }

                // Fuse button
                Button(
                    onClick = onFuse,
                    modifier = Modifier.weight(1f),
                    enabled = validation is FusionValidation.Valid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("⚗️ MASH 'EM UP! (-$fusionCost 🪙)", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

/**
 * Individual card slot for fusion
 */
@Composable
private fun FusionCardSlot(
    card: Card?,
    index: Int
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                color = if (card != null) {
                    card.rarity.getColor().copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 2.dp,
                color = if (card != null) {
                    card.rarity.getColor()
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (card != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(card.imageUrl))
                    .crossfade(true)
                    .build(),
                contentDescription = card.prompt,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.card_placeholder),
                error = painterResource(R.drawable.card_error)
            )
        } else {
            Text(
                text = "${index + 1}",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Recipe suggestions
 */
@Composable
private fun RecipeSuggestions(
    recipes: List<FusionRecipe>,
    onRecipeSelect: (FusionRecipe) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Recipes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(recipes) { recipe ->
                RecipeCard(recipe = recipe, onClick = { onRecipeSelect(recipe) })
            }
        }
    }
}

/**
 * Individual recipe card
 */
@Composable
private fun RecipeCard(
    recipe: FusionRecipe,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, recipe.guaranteedRarity.getColor())
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = recipe.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = recipe.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 2
            )
            Text(
                text = "→ ${recipe.guaranteedRarity.displayName}",
                style = MaterialTheme.typography.labelSmall,
                color = recipe.guaranteedRarity.getColor(),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Card selection grid
 */
@Composable
private fun CardSelectionGrid(
    cards: List<Card>,
    selectedCards: List<Card>,
    onCardClick: (Card) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(cards) { card ->
            SelectableCardItem(
                card = card,
                isSelected = selectedCards.contains(card),
                onClick = { onCardClick(card) }
            )
        }
    }
}

/**
 * Selectable card item
 */
@Composable
private fun SelectableCardItem(
    card: Card,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.95f else 1f,
        label = "scale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(0.7f)
            .scale(scale)
    ) {
        StyledCardView(
            card = card,
            displayMode = CardDisplayMode.THUMBNAIL,
            onClick = onClick,
            modifier = Modifier.fillMaxSize()
        )

        // Selection overlay and checkmark
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(card.rarity.getColor().copy(alpha = 0.3f))
            )
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = card.rarity.getColor(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

/**
 * Empty card message
 */
@Composable
private fun EmptyCardMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "No cards available for fusion",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = "Generate some cards first!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}


/**
 * Fusion animation screen with text-based status updates
 */
@Composable
private fun FusionAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "fusion")

    // Rotating animation for icon
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulsing animation
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Alpha animation for shimmer
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Animated fusion verbs
    var messageIndex by remember { mutableIntStateOf(0) }
    val messages = remember { ActionVerbs.getFusionVerbCycle(6) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            messageIndex = (messageIndex + 1) % messages.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated fusion icon
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer rotating border
                Box(
                    modifier = Modifier
                        .size(180.dp)
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

                // Pulsing center icon
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = "Fusing",
                    modifier = Modifier
                        .size(100.dp)
                        .scale(scale)
                        .alpha(alpha)
                        .rotate(-rotation),
                    tint = MaterialTheme.colorScheme.primary
                )

                // Orbiting sparkles
                for (i in 0..2) {
                    val angle = rotation + (i * 120f)
                    val offsetX = (90 * kotlin.math.cos(Math.toRadians(angle.toDouble()))).toFloat()
                    val offsetY = (90 * kotlin.math.sin(Math.toRadians(angle.toDouble()))).toFloat()
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier
                            .offset(x = offsetX.dp, y = offsetY.dp)
                            .size(28.dp)
                            .alpha(alpha),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Animated status text with transition
            Text(
                text = messages[messageIndex].uppercase(),
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(alpha)
            )
        }
    }
}

/**
 * Fusion result screen
 */
@Composable
private fun FusionResultScreen(
    result: FusionResult,
    onFuseAgain: () -> Unit,
    onViewCollection: () -> Unit
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
                text = "YOOO IT WORKED! ✨💥",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )

            // Recipe discovery (if any)
            result.recipeDiscovered?.let {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFD700).copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎉 NEW RECIPE UNLOCKED!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = it.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Result card display
            StyledCardView(
                card = result.resultCard,
                displayMode = CardDisplayMode.FULL,
                onClick = { },
                modifier = Modifier.fillMaxWidth(0.9f)
            )

            // Bonus info
            result.bonusApplied?.let {
                Text(
                    text = "✨ Recipe Bonus: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onFuseAgain,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("MASH AGAIN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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

/**
 * Fusion failed screen - shown when fusion attempt failed due to chance
 */
@Composable
private fun FusionFailedScreen(
    onTryAgain: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.tertiaryContainer
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Failed message
            Text(
                text = "💀 RIP FUSION 💀",
                fontSize = 52.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Bro the vibe was off...\nYour cards are safe but coins = gone",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Try again button
            Button(
                onClick = onTryAgain,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "RUN IT BACK",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

/**
 * Error screen
 */
@Composable
private fun ErrorScreen(
    message: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )

                Text(
                    text = "Error",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Button(onClick = onDismiss) {
                    Text("OK")
                }
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
