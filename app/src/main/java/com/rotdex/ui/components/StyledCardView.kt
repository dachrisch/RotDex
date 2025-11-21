package com.rotdex.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.rotdex.R
import com.rotdex.data.models.Card
import com.rotdex.data.models.CardRarity
import com.rotdex.ui.theme.getColor
import java.io.File

/**
 * Display mode for the styled card view
 */
enum class CardDisplayMode {
    THUMBNAIL,  // Compact view for grids
    FULL        // Expanded view with all details including biography
}

/**
 * Reusable styled card UI component with RPG game aesthetics
 *
 * Features:
 * - Ornamental border matching card rarity
 * - Medieval-style nameplate at bottom
 * - HP and Attack stat indicators
 * - Biography display in full mode
 * - Gradient effects and shadows
 *
 * @param card The card to display
 * @param displayMode Whether to show as thumbnail or full view
 * @param modifier Optional modifier for the card
 * @param onClick Optional click handler
 */
@Composable
fun StyledCardView(
    card: Card,
    displayMode: CardDisplayMode = CardDisplayMode.THUMBNAIL,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val rarityColor = card.rarity.getColor()
    val borderWidth = if (displayMode == CardDisplayMode.FULL) 4.dp else 2.dp

    Card(
        modifier = modifier
            .then(
                // In thumbnail mode, maintain aspect ratio to prevent stretching
                if (displayMode == CardDisplayMode.THUMBNAIL) {
                    Modifier.aspectRatio(0.75f) // Portrait card ratio (3:4)
                } else {
                    Modifier
                }
            )
            .shadow(
                elevation = if (displayMode == CardDisplayMode.FULL) 16.dp else 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = rarityColor.copy(alpha = 0.5f)
            ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(borderWidth, rarityColor),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = { onClick?.invoke() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Card image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(card.imageUrl))
                    .crossfade(true)
                    .build(),
                contentDescription = card.name.ifEmpty { card.prompt },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.card_placeholder),
                error = painterResource(R.drawable.card_error)
            )

            // Gradient overlay at top and bottom for better text visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.6f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            // Stat indicators
            StatIndicators(
                card = card,
                rarityColor = rarityColor,
                displayMode = displayMode
            )

            // Nameplate at bottom
            Nameplate(
                card = card,
                rarityColor = rarityColor,
                displayMode = displayMode
            )
        }

        // Biography (only in full mode)
        if (displayMode == CardDisplayMode.FULL && card.biography.isNotEmpty()) {
            BiographySection(card.biography, rarityColor)
        }
    }
}

/**
 * Format stat values for display, abbreviating large numbers
 */
private fun formatStatValue(value: Int): String {
    return when {
        value >= 1000000 -> "${value / 1000000}M"
        value >= 1000 -> "${value / 1000}K"
        else -> value.toString()
    }
}

/**
 * HP and Attack stat indicators displayed as badges
 */
@Composable
private fun BoxScope.StatIndicators(
    card: Card,
    rarityColor: Color,
    displayMode: CardDisplayMode
) {
    val iconSize = if (displayMode == CardDisplayMode.FULL) 20.dp else 16.dp
    val fontSize = if (displayMode == CardDisplayMode.FULL) 14.sp else 11.sp
    val padding = if (displayMode == CardDisplayMode.FULL) 8.dp else 6.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .padding(padding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // HP (Health) indicator - top left
        StatBadge(
            icon = Icons.Default.Favorite,
            value = formatStatValue(card.health),
            backgroundColor = Color(0xFFE74C3C).copy(alpha = 0.9f),
            iconSize = iconSize,
            fontSize = fontSize,
            accessibilityLabel = "Health: ${card.health}"
        )

        Spacer(modifier = Modifier.weight(1f))

        // Attack indicator - top right
        StatBadge(
            icon = null,  // Using text symbol for sword
            value = formatStatValue(card.attack),
            backgroundColor = Color(0xFFF39C12).copy(alpha = 0.9f),
            iconSize = iconSize,
            fontSize = fontSize,
            prefix = "⚔",
            accessibilityLabel = "Attack: ${card.attack}"
        )
    }
}

/**
 * Individual stat badge with icon and value
 */
@Composable
private fun StatBadge(
    icon: ImageVector?,
    value: String,
    backgroundColor: Color,
    iconSize: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    prefix: String? = null,
    accessibilityLabel: String? = null
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.then(
            if (accessibilityLabel != null) {
                Modifier.semantics {
                    contentDescription = accessibilityLabel
                    role = Role.Image
                }
            } else {
                Modifier
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (prefix != null) {
                Text(
                    text = prefix,
                    fontSize = fontSize,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = value,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Medieval-style nameplate banner at the bottom
 */
@Composable
private fun BoxScope.Nameplate(
    card: Card,
    rarityColor: Color,
    displayMode: CardDisplayMode
) {
    val nameplateHeight = if (displayMode == CardDisplayMode.FULL) 60.dp else 40.dp
    val fontSize = if (displayMode == CardDisplayMode.FULL) 18.sp else 14.sp
    val rarityFontSize = if (displayMode == CardDisplayMode.FULL) 12.sp else 10.sp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .height(nameplateHeight)
    ) {
        // Nameplate background with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(nameplateHeight)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            rarityColor.copy(alpha = 0.95f),
                            rarityColor.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Decorative border line at top of nameplate
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.3f))
        )

        // Card name and rarity
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = card.name.ifEmpty { card.prompt },
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = if (displayMode == CardDisplayMode.FULL) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            if (displayMode == CardDisplayMode.FULL) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = card.rarity.displayName.uppercase(),
                    fontSize = rarityFontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                    letterSpacing = 1.sp
                )
            }
        }

        // Rarity tag at bottom-left corner (THUMBNAIL mode only)
        if (displayMode == CardDisplayMode.THUMBNAIL) {
            Surface(
                color = Color.White.copy(alpha = 0.95f),
                shape = RoundedCornerShape(topEnd = 8.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
            ) {
                Text(
                    text = card.rarity.displayName.first().toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = rarityColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Biography section displayed below the card in full mode
 */
@Composable
private fun BiographySection(
    biography: String,
    rarityColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Section header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .background(rarityColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "BIOGRAPHY",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = rarityColor,
                letterSpacing = 1.sp,
                modifier = Modifier.semantics { heading() }
            )
        }

        // Biography text
        Text(
            text = biography,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            lineHeight = 20.sp,
            maxLines = 10,
            overflow = TextOverflow.Ellipsis
        )
    }
}


/**
 * Preview for thumbnail mode
 */
@Preview(showBackground = true, widthDp = 180, heightDp = 250)
@Composable
private fun StyledCardViewThumbnailPreview() {
    MaterialTheme {
        StyledCardView(
            card = Card(
                id = 1,
                prompt = "A mystical dragon guardian",
                imageUrl = "",
                rarity = CardRarity.LEGENDARY,
                name = "Dragon Guardian",
                health = 150,
                attack = 85,
                biography = "An ancient dragon that protects the sacred temple"
            ),
            displayMode = CardDisplayMode.THUMBNAIL,
            modifier = Modifier.size(width = 180.dp, height = 250.dp)
        )
    }
}

/**
 * Preview for full mode
 */
@Preview(showBackground = true, widthDp = 320, heightDp = 500)
@Composable
private fun StyledCardViewFullPreview() {
    MaterialTheme {
        StyledCardView(
            card = Card(
                id = 2,
                prompt = "A powerful wizard",
                imageUrl = "",
                rarity = CardRarity.EPIC,
                name = "Archmage Merlin",
                health = 100,
                attack = 120,
                biography = "The most powerful wizard in the realm, master of ancient spells and keeper of forbidden knowledge. His magical prowess is matched only by his wisdom."
            ),
            displayMode = CardDisplayMode.FULL,
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
        )
    }
}

/**
 * Preview for COMMON rarity
 */
@Preview(showBackground = true, widthDp = 180, heightDp = 250)
@Composable
private fun StyledCardViewCommonPreview() {
    MaterialTheme {
        StyledCardView(
            card = Card(
                id = 3,
                prompt = "A simple warrior",
                imageUrl = "",
                rarity = CardRarity.COMMON,
                name = "Town Guard",
                health = 50,
                attack = 30,
                biography = "A regular guard protecting the town"
            ),
            displayMode = CardDisplayMode.THUMBNAIL,
            modifier = Modifier.size(width = 180.dp, height = 250.dp)
        )
    }
}

/**
 * Preview for RARE rarity
 */
@Preview(showBackground = true, widthDp = 180, heightDp = 250)
@Composable
private fun StyledCardViewRarePreview() {
    MaterialTheme {
        StyledCardView(
            card = Card(
                id = 4,
                prompt = "A skilled knight",
                imageUrl = "",
                rarity = CardRarity.RARE,
                name = "Royal Knight",
                health = 80,
                attack = 60,
                biography = "An elite knight serving the royal family"
            ),
            displayMode = CardDisplayMode.THUMBNAIL,
            modifier = Modifier.size(width = 180.dp, height = 250.dp)
        )
    }
}

/**
 * Preview for long name edge case
 */
@Preview(showBackground = true, widthDp = 180, heightDp = 250)
@Composable
private fun StyledCardViewLongNamePreview() {
    MaterialTheme {
        StyledCardView(
            card = Card(
                id = 5,
                prompt = "A character with a very long name",
                imageUrl = "",
                rarity = CardRarity.LEGENDARY,
                name = "Supreme Grand Master of the Ancient Order of Mystic Warriors",
                health = 200,
                attack = 150,
                biography = "A legendary hero with an incredibly long title"
            ),
            displayMode = CardDisplayMode.THUMBNAIL,
            modifier = Modifier.size(width = 180.dp, height = 250.dp)
        )
    }
}

/**
 * Preview for empty biography
 */
@Preview(showBackground = true, widthDp = 320, heightDp = 500)
@Composable
private fun StyledCardViewEmptyBiographyPreview() {
    MaterialTheme {
        StyledCardView(
            card = Card(
                id = 6,
                prompt = "A mysterious character",
                imageUrl = "",
                rarity = CardRarity.EPIC,
                name = "Shadow Assassin",
                health = 90,
                attack = 110,
                biography = ""
            ),
            displayMode = CardDisplayMode.FULL,
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
        )
    }
}

/**
 * Preview for high stat values (>999)
 */
@Preview(showBackground = true, widthDp = 180, heightDp = 250)
@Composable
private fun StyledCardViewHighStatsPreview() {
    MaterialTheme {
        StyledCardView(
            card = Card(
                id = 7,
                prompt = "An overpowered character",
                imageUrl = "",
                rarity = CardRarity.LEGENDARY,
                name = "Ultimate Champion",
                health = 1500,
                attack = 2000,
                biography = "A champion with incredible power"
            ),
            displayMode = CardDisplayMode.THUMBNAIL,
            modifier = Modifier.size(width = 180.dp, height = 250.dp)
        )
    }
}

/**
 * Preview for dark mode
 */
@Preview(
    showBackground = true,
    widthDp = 180,
    heightDp = 250,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun StyledCardViewDarkModePreview() {
    MaterialTheme {
        StyledCardView(
            card = Card(
                id = 8,
                prompt = "A dark knight",
                imageUrl = "",
                rarity = CardRarity.EPIC,
                name = "Dark Knight",
                health = 120,
                attack = 95,
                biography = "A knight who fights in the shadows"
            ),
            displayMode = CardDisplayMode.THUMBNAIL,
            modifier = Modifier.size(width = 180.dp, height = 250.dp)
        )
    }
}
