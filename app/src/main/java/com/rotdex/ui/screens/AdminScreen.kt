package com.rotdex.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rotdex.data.models.CardRarity
import com.rotdex.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val message by viewModel.message.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current stats
            userProfile?.let { profile ->
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
                            text = "Current Resources",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatDisplay("Energy", "${profile.currentEnergy}/${profile.maxEnergy}")
                            StatDisplay("Coins", "${profile.brainrotCoins}")
                            StatDisplay("Gems", "${profile.gems}")
                        }
                    }
                }
            }

            // Message display
            if (message.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Add Resources",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            // Add Energy
            AdminButton(
                icon = Icons.Default.FlashOn,
                title = "Add Energy (+5)",
                onClick = { viewModel.addEnergy(5) }
            )

            // Add Coins
            AdminButton(
                icon = Icons.Default.MonetizationOn,
                title = "Add Coins (+100)",
                onClick = { viewModel.addCoins(100) }
            )

            // Add Gems
            AdminButton(
                icon = Icons.Default.Diamond,
                title = "Add Gems (+10)",
                onClick = { viewModel.addGems(10) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Add Test Cards",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            // Add Test Card
            AdminButton(
                icon = Icons.Default.AddCard,
                title = "Add Random Test Card",
                onClick = { viewModel.addTestCard(context) }
            )

            // Add Legendary Test Card
            AdminButton(
                icon = Icons.Default.Stars,
                title = "Add Legendary Test Card",
                onClick = { viewModel.addTestCard(context, CardRarity.LEGENDARY) }
            )
        }
    }
}

@Composable
private fun StatDisplay(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AdminButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
