package com.san.productivity.tvremotewifibluetooth.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.san.productivity.tvremotewifibluetooth.data.model.RemoteCommand

@Composable
fun VolumeChannelControl(
    onCommand: (RemoteCommand) -> Unit,
    onStartRepeat: (RemoteCommand) -> Unit,
    onStopRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // VOLUME ROCKER CARD
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.padding(4.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                RemoteButton(
                    onClick = { onCommand(RemoteCommand.VOLUME_UP) },
                    onLongPress = { onStartRepeat(RemoteCommand.VOLUME_UP) },
                    onRelease = onStopRepeat,
                    icon = Icons.Default.Add,
                    text = "VOL+",
                    shape = RoundedCornerShape(16.dp),
                    size = 52.dp
                )

                Text(
                    text = "VOL",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                RemoteButton(
                    onClick = { onCommand(RemoteCommand.MUTE) },
                    icon = Icons.AutoMirrored.Filled.VolumeMute,
                    text = "MUTE",
                    shape = RoundedCornerShape(12.dp),
                    size = 44.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )

                RemoteButton(
                    onClick = { onCommand(RemoteCommand.VOLUME_DOWN) },
                    onLongPress = { onStartRepeat(RemoteCommand.VOLUME_DOWN) },
                    onRelease = onStopRepeat,
                    icon = Icons.Default.Remove,
                    text = "VOL-",
                    shape = RoundedCornerShape(16.dp),
                    size = 52.dp
                )
            }
        }

        // CHANNEL ROCKER CARD
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.padding(4.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                RemoteButton(
                    onClick = { onCommand(RemoteCommand.CHANNEL_UP) },
                    onLongPress = { onStartRepeat(RemoteCommand.CHANNEL_UP) },
                    onRelease = onStopRepeat,
                    icon = Icons.Default.KeyboardArrowUp,
                    text = "CH+",
                    shape = RoundedCornerShape(16.dp),
                    size = 52.dp
                )

                Text(
                    text = "CH",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                RemoteButton(
                    onClick = { onCommand(RemoteCommand.PREV_CHANNEL) },
                    text = "PREV",
                    shape = RoundedCornerShape(12.dp),
                    size = 44.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )

                RemoteButton(
                    onClick = { onCommand(RemoteCommand.CHANNEL_DOWN) },
                    onLongPress = { onStartRepeat(RemoteCommand.CHANNEL_DOWN) },
                    onRelease = onStopRepeat,
                    icon = Icons.Default.KeyboardArrowDown,
                    text = "CH-",
                    shape = RoundedCornerShape(16.dp),
                    size = 52.dp
                )
            }
        }
    }
}
