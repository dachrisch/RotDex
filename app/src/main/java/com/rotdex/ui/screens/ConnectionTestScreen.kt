package com.rotdex.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotdex.data.manager.ConnectionState
import com.rotdex.data.manager.ConnectionTestManager
import com.rotdex.ui.components.RotDexLogo

/**
 * Test screen to verify Nearby Connections API works
 * THIS IS TEMPORARY - just for testing connectivity!
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionTestScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { ConnectionTestManager(context) }

    val connectionState by manager.connectionState.collectAsState()
    val discoveredDevices by manager.discoveredDevices.collectAsState()
    val messages by manager.messages.collectAsState()

    var playerName by remember { mutableStateOf("Player${(1000..9999).random()}") }
    var testMessage by remember { mutableStateOf("Hello from RotDex!") }
    var hasPermissions by remember { mutableStateOf(false) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
    }

    // Request permissions on launch
    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        permissionLauncher.launch(permissions)
    }

    DisposableEffect(Unit) {
        onDispose {
            manager.stopAll()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🧪 Connection Test") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission status
            if (!hasPermissions) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠️ Permissions Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please grant Bluetooth and Location permissions to test connectivity",
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Player name input
            OutlinedTextField(
                value = playerName,
                onValueChange = { playerName = it },
                label = { Text("Your Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = connectionState is ConnectionState.Idle
            )

            // Status card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Status:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = when (connectionState) {
                            is ConnectionState.Idle -> "Idle"
                            is ConnectionState.Advertising -> "📡 Advertising..."
                            is ConnectionState.Discovering -> "🔍 Discovering..."
                            is ConnectionState.Connecting -> "🤝 Connecting..."
                            is ConnectionState.ConnectionInitiated -> "⏳ Connection Initiated"
                            is ConnectionState.Connected -> "✅ CONNECTED!"
                            is ConnectionState.Disconnected -> "🔌 Disconnected"
                            is ConnectionState.Error -> "❌ Error"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (connectionState is ConnectionState.Error) {
                        Text(
                            text = (connectionState as ConnectionState.Error).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Divider()

            // Host section
            Text(
                text = "DEVICE 1: HOST",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Button(
                onClick = { manager.startAdvertising(playerName) },
                modifier = Modifier.fillMaxWidth(),
                enabled = connectionState is ConnectionState.Idle && hasPermissions
            ) {
                Text("🔥 START HOSTING", fontWeight = FontWeight.Bold)
            }

            Divider()

            // Client section
            Text(
                text = "DEVICE 2: JOIN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.secondary
            )

            Button(
                onClick = { manager.startDiscovery(playerName) },
                modifier = Modifier.fillMaxWidth(),
                enabled = connectionState is ConnectionState.Idle && hasPermissions
            ) {
                Text("🔍 SCAN FOR HOSTS", fontWeight = FontWeight.Bold)
            }

            // Discovered devices
            if (discoveredDevices.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Found Hosts:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        discoveredDevices.forEach { device ->
                            val parts = device.split("(")
                            val name = parts[0].trim()
                            val endpointId = parts.getOrNull(1)?.removeSuffix(")")?.trim() ?: ""

                            Button(
                                onClick = {
                                    if (endpointId.isNotEmpty()) {
                                        manager.connectToEndpoint(endpointId, playerName)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Connect to: $name")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            Divider()

            // Message testing (only when connected)
            if (connectionState is ConnectionState.Connected) {
                Text(
                    text = "TEST MESSAGING",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )

                OutlinedTextField(
                    value = testMessage,
                    onValueChange = { testMessage = it },
                    label = { Text("Test Message") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        manager.sendMessage(testMessage)
                        testMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📤 SEND MESSAGE", fontWeight = FontWeight.Bold)
                }
            }

            // Disconnect button
            if (connectionState !is ConnectionState.Idle) {
                OutlinedButton(
                    onClick = { manager.stopAll() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("STOP & RESET", fontWeight = FontWeight.Bold)
                }
            }

            Divider()

            // Message log
            if (messages.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Activity Log:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 250.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(messages) { message ->
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
