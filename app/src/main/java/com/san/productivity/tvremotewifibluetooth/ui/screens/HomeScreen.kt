package com.san.productivity.tvremotewifibluetooth.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionMode
import com.san.productivity.tvremotewifibluetooth.data.model.TvDevice
import com.san.productivity.tvremotewifibluetooth.ui.components.ConnectionIndicator
import com.san.productivity.tvremotewifibluetooth.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToRemote: () -> Unit,
    onNavigateToDiscovery: () -> Unit,
    onNavigateToBluetooth: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val currentDevice by viewModel.currentDevice.collectAsState()
    val savedDevices by viewModel.savedDevices.collectAsState()
    val connectionMode by viewModel.connectionMode.collectAsState()
    val showManualDialog by viewModel.showManualIpDialog.collectAsState()
    val manualIpInput by viewModel.manualIpInput.collectAsState()
    val manualMacInput by viewModel.manualMacInput.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP HEADER BAR
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Samsung TV Remote",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Model: UA43AUE65AKXXL / Tizen TV",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }

        // CONNECTION MODE SWITCHER (Wi-Fi vs Bluetooth)
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = connectionMode == ConnectionMode.WIFI,
                    onClick = { viewModel.setConnectionMode(ConnectionMode.WIFI) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Wi-Fi Mode")
                }

                SegmentedButton(
                    selected = connectionMode == ConnectionMode.BLUETOOTH,
                    onClick = {
                        viewModel.setConnectionMode(ConnectionMode.BLUETOOTH)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bluetooth Mode")
                }
            }
        }

        // CONNECTION STATUS CARD
        item {
            ConnectionIndicator(
                connectionState = connectionState,
                currentDevice = currentDevice,
                onConnectClick = {
                    val target = currentDevice ?: savedDevices.firstOrNull() ?: TvDevice.createDefault("192.168.1.100")
                    viewModel.connectToDevice(target)
                },
                onDisconnectClick = { viewModel.disconnect() },
                onPowerOnClick = {
                    currentDevice?.let { viewModel.powerOnTv(it) }
                }
            )
        }

        // OPEN REMOTE CONTROL CARD
        item {
            Card(
                onClick = onNavigateToRemote,
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Open Virtual Remote",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Full D-Pad, Volume, Playback & Touchpad",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // DISCOVERY AND MANUAL ADD ACTION ROW
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (connectionMode == ConnectionMode.BLUETOOTH) {
                            onNavigateToBluetooth()
                        } else {
                            onNavigateToDiscovery()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (connectionMode == ConnectionMode.BLUETOOTH) Icons.Default.Bluetooth else Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (connectionMode == ConnectionMode.BLUETOOTH) "BT Devices" else "Scan Network")
                }

                OutlinedButton(
                    onClick = { viewModel.setShowManualIpDialog(true) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Manual IP Entry")
                }
            }
        }

        // SAVED TVS HEADER
        item {
            Text(
                text = "Saved TVs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (savedDevices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved TVs yet. Scan network or add manual IP to connect.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(savedDevices) { device ->
                SavedTvCard(
                    device = device,
                    onConnect = { viewModel.connectToDevice(device) },
                    onDelete = { viewModel.removeSavedDevice(device.ipAddress) },
                    onPowerOn = { viewModel.powerOnTv(device) }
                )
            }
        }
    }

    // MANUAL IP DIALOG
    if (showManualDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowManualIpDialog(false) },
            title = { Text("Manual TV Connection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your Samsung TV's IP address (e.g. 192.168.1.150)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = manualIpInput,
                        onValueChange = { viewModel.setManualIpInput(it) },
                        label = { Text("TV IP Address") },
                        placeholder = { Text("192.168.1.X") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = manualMacInput,
                        onValueChange = { viewModel.setManualMacInput(it) },
                        label = { Text("TV MAC Address (Optional for Power-On)") },
                        placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.connectManualIp() }) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowManualIpDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SavedTvCard(
    device: TvDevice,
    onConnect: () -> Unit,
    onDelete: () -> Unit,
    onPowerOn: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "IP: ${device.ipAddress} • Port: ${device.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (device.macAddress.isNotBlank()) {
                    Text(
                        text = "MAC: ${device.macAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (device.macAddress.isNotBlank()) {
                    IconButton(onClick = onPowerOn) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Power On via WoL",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Connect")
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Saved TV",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
