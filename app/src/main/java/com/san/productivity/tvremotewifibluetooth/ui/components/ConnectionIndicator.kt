package com.san.productivity.tvremotewifibluetooth.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionMode
import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionState
import com.san.productivity.tvremotewifibluetooth.data.model.TvDevice

@Composable
fun ConnectionIndicator(
    connectionState: ConnectionState,
    currentDevice: TvDevice?,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPowerOnClick: (() -> Unit)? = null
) {
    val (statusColor, statusText) = when (connectionState) {
        is ConnectionState.Connected -> Pair(Color(0xFF4CAF50), "Connected to ${connectionState.deviceName}")
        is ConnectionState.Connecting -> Pair(Color(0xFFFFC107), connectionState.message)
        is ConnectionState.PairingRequested -> Pair(Color(0xFFFF9800), connectionState.message)
        is ConnectionState.Reconnecting -> Pair(Color(0xFFFF9800), connectionState.message)
        is ConnectionState.Error -> Pair(Color(0xFFF44336), connectionState.message)
        is ConnectionState.Discovering -> Pair(Color(0xFF2196F3), "Discovering Samsung TVs...")
        is ConnectionState.Disconnected -> Pair(Color(0xFF9E9E9E), "Disconnected")
    }

    val animatedIndicatorColor by animateColorAsState(targetValue = statusColor, label = "statusColor")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(animatedIndicatorColor)
                    )

                    Icon(
                        imageVector = if (currentDevice?.connectionType == ConnectionMode.BLUETOOTH) Icons.Default.Bluetooth else Icons.Default.Wifi,
                        contentDescription = "Connection Type",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )

                    Column {
                        Text(
                            text = currentDevice?.displayName ?: "No TV Selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (currentDevice != null && currentDevice.ipAddress.isNotBlank()) {
                            Text(
                                text = "IP: ${currentDevice.ipAddress} ${if (currentDevice.macAddress.isNotBlank()) "• MAC: ${currentDevice.macAddress}" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (connectionState is ConnectionState.Connecting || connectionState is ConnectionState.Reconnecting) {
                    IconButton(onClick = onConnectClick) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refreshing"
                        )
                    }
                }
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = animatedIndicatorColor,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (connectionState) {
                    is ConnectionState.Connected -> {
                        OutlinedButton(
                            onClick = onDisconnectClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Disconnect")
                        }
                    }
                    else -> {
                        Button(
                            onClick = onConnectClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Connect")
                        }

                        if (onPowerOnClick != null && currentDevice?.macAddress?.isNotBlank() == true) {
                            OutlinedButton(
                                onClick = onPowerOnClick,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Power On (WoL)")
                            }
                        }
                    }
                }
            }
        }
    }
}
