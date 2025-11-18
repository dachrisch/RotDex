package com.rotdex.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rotdex.data.models.*
import com.rotdex.ui.viewmodel.FusionState
import com.rotdex.ui.viewmodel.FusionViewModel

/**
 * Main fusion screen for combining cards
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FusionScreen(
    viewModel: FusionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val allCards by viewModel.allCards.collectAsState()
    val selectedCards by viewModel.selectedCards.collectAsState()
    val fusionState by viewModel.fusionState.collectAsState()
    val validation by viewModel.validation.collectAsState()
    val matchingRecipe by viewModel.matchingRecipe.collectAsState()
    val publicRecipes by viewModel.publicRecipes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card Fusion ⚗️") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                        onCardClick = { viewModel.toggleCardSelection(it) },
                        onFuse = { viewModel.performFusion() },
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
                        onDismiss = { viewModel.resetFusionState() }
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
                text = "Fusion Chamber",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
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
                            text = "${it.name} Recipe!",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Validation display
            validation?.let {
                when (it) {
                    is FusionValidation.Valid -> {
                        Text(
                            text = "Success Rate: ${(it.successRate * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
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
                // Clear button
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f),
                    enabled = selectedCards.isNotEmpty()
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }

                // Fuse button
                Button(
                    onClick = onFuse,
                    modifier = Modifier.weight(2f),
                    enabled = validation is FusionValidation.Valid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("FUSE CARDS", fontWeight = FontWeight.Bold)
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
            .size(60.dp)
            .background(
                color = if (card != null) {
                    getRarityColor(card.rarity).copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (card != null) {
            Text(
                text = card.rarity.displayName.first().toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = getRarityColor(card.rarity)
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
        border = BorderStroke(1.dp, getRarityColor(recipe.guaranteedRarity))
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
                color = getRarityColor(recipe.guaranteedRarity),
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
        targetValue = if (isSelected) 0.9f else 1f,
        label = "scale"
    )

    Card(
        modifier = Modifier
            .aspectRatio(0.7f)
            .scale(scale)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                getRarityColor(card.rarity).copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 3.dp else 1.dp,
            color = if (isSelected) {
                getRarityColor(card.rarity)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            }
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                // Rarity indicator
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = getRarityColor(card.rarity),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = card.rarity.displayName.first().toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Card prompt (truncated)
                Text(
                    text = card.prompt.take(30),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    fontSize = 10.sp
                )
            }

            // Selection checkmark
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = getRarityColor(card.rarity),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                )
            }
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
 * Get color for card rarity
 */
private fun getRarityColor(rarity: CardRarity): Color {
    return when (rarity) {
        CardRarity.COMMON -> Color(0xFF9E9E9E)      // Gray
        CardRarity.RARE -> Color(0xFF2196F3)        // Blue
        CardRarity.EPIC -> Color(0xFF9C27B0)        // Purple
        CardRarity.LEGENDARY -> Color(0xFFFFD700)   // Gold
    }
}

/**
 * Fusion animation screen
 */
@Composable
private fun FusionAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "fusion")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoFixHigh,
                contentDescription = "Fusing",
                modifier = Modifier
                    .size(120.dp)
                    .rotate(rotation)
                    .scale(scale),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Fusing Cards...",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
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
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Success/Failure indicator
                Icon(
                    imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = if (result.success) "Success" else "Failed",
                    modifier = Modifier.size(80.dp),
                    tint = if (result.success) Color(0xFF4CAF50) else Color(0xFFF44336)
                )

                Text(
                    text = if (result.success) "Fusion Successful!" else "Fusion Failed",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (result.success) Color(0xFF4CAF50) else Color(0xFFF44336)
                )

                // Result card info
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = getRarityColor(result.resultCard.rarity).copy(alpha = 0.2f)
                    ),
                    border = BorderStroke(2.dp, getRarityColor(result.resultCard.rarity))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = result.resultCard.rarity.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = getRarityColor(result.resultCard.rarity)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = result.resultCard.prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Bonus info
                result.bonusApplied?.let {
                    Text(
                        text = "✨ Recipe Bonus: $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFFD700)
                    )
                }

                // Recipe discovery
                result.recipeDiscovered?.let {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFD700).copy(alpha = 0.2f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🎉 New Recipe Discovered!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = it.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue")
                }
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
