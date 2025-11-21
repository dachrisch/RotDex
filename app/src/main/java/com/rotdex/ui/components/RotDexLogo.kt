package com.rotdex.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable RotDex logo component for consistent branding
 */
@Composable
fun RotDexLogo(
    modifier: Modifier = Modifier,
    showEmoji: Boolean = true,
    fontSize: Int = 20
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showEmoji) {
            Text(
                text = "🧠",
                fontSize = fontSize.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = "RotDex",
            fontSize = fontSize.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
