package com.san.productivity.tvremotewifibluetooth.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TroubleshootingScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Troubleshooting Guide") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Samsung TV (UA43AUE65AKXXL / Tizen 2022) Diagnostics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // ITEM 1: PAIRING PROMPT NOT SHOWING
            TroubleshootingCard(
                icon = Icons.Default.Lock,
                title = "1. TV Pairing Prompt Not Showing / Access Denied",
                problem = "The TV displays connection error or fails to prompt 'Allow [Android TV Remote]'.",
                solutions = listOf(
                    "On your Samsung TV, open: Settings > General & Privacy > External Device Manager > Device Connection Manager.",
                    "Check 'Access Notification' and set it to 'First Time Only' or 'Always On'.",
                    "Go to 'Device List' and delete any old or denied mobile devices.",
                    "Restart the app and tap Connect again. A popup will appear on your TV screen. Click ALLOW using your physical remote."
                )
            )

            // ITEM 2: SAME WI-FI & ROUTER ISOLATION
            TroubleshootingCard(
                icon = Icons.Default.Router,
                title = "2. TV Not Found on Network Scan",
                problem = "Network scan fails to detect TV IP address automatically.",
                solutions = listOf(
                    "Verify phone and Samsung TV are connected to the EXACT same Wi-Fi router & frequency (2.4GHz vs 5GHz).",
                    "Check Wi-Fi Router Settings: Disable 'AP Isolation', 'Client Isolation', or 'Guest Network Isolation' on your Wi-Fi router.",
                    "Ensure TV IP address in TV Network Status matches the IP entered in the app (e.g. 192.168.1.X).",
                    "Try Manual IP Entry on the Home screen to connect directly."
                )
            )

            // ITEM 3: WSS PORT 8002 VS WS PORT 8001
            TroubleshootingCard(
                icon = Icons.Default.Wifi,
                title = "3. WebSocket Protocol & Ports (8001 / 8002)",
                problem = "Connection hangs or drops immediately.",
                solutions = listOf(
                    "2016-2024 Samsung Tizen TVs (including UA43AUE65AKXXL) default to Port 8002 (WSS - Secure WebSocket with TLS).",
                    "Older Samsung TVs use Port 8001 (WS - Plaintext WebSocket).",
                    "The app automatically attempts Port 8002 first, and falls back to Port 8001 if Port 8002 is blocked.",
                    "Make sure your Wi-Fi router firewall is not blocking local TCP ports 8001 or 8002."
                )
            )

            // ITEM 4: POWER ON / WAKE-ON-LAN
            TroubleshootingCard(
                icon = Icons.Default.PowerSettingsNew,
                title = "4. Power-On (Wake-on-LAN) Requirements",
                problem = "Power button cannot turn ON TV when it is in Standby mode.",
                solutions = listOf(
                    "When Samsung TV is powered OFF (Standby), Wi-Fi connection shuts down after a few minutes unless Wake-on-LAN is enabled.",
                    "In TV Settings: Open Settings > General > Network > Expert Settings > Turn ON 'Power On with Mobile'.",
                    "Enter your TV's MAC Address in App Settings or TV details. Wake-on-LAN sends a UDP magic packet over your LAN to wake the TV.",
                    "If TV is connected via Ethernet cable instead of Wi-Fi, Wake-on-LAN is significantly more reliable."
                )
            )

            // ITEM 5: BLUETOOTH HID LIMITATIONS
            TroubleshootingCard(
                icon = Icons.Default.Bluetooth,
                title = "5. Bluetooth Remote Limitations & Setup",
                problem = "Some buttons (like Home or App Launch) don't work over Bluetooth.",
                solutions = listOf(
                    "Bluetooth mode emulates a standard Bluetooth HID Keyboard/Gamepad.",
                    "Supported over Bluetooth: D-Pad Up/Down/Left/Right, OK/Enter, Back (Esc), Vol +, Vol -, Mute, Media Play/Pause.",
                    "Unsupported over Bluetooth: Power-On from deep standby, Smart Apps deep-link launching, and direct channel shortcuts.",
                    "To pair Bluetooth: Put TV in Bluetooth pairing mode (Settings > Sound > Sound Output > Bluetooth Device List), then scan and pair in Bluetooth screen."
                )
            )
        }
    }
}

@Composable
fun TroubleshootingCard(
    icon: ImageVector,
    title: String,
    problem: String,
    solutions: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Issue: $problem",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )

            Text(
                text = "Solutions:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            solutions.forEachIndexed { index, solution ->
                Text(
                    text = "${index + 1}. $solution",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
