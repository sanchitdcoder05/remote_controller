package com.san.productivity.tvremotewifibluetooth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.san.productivity.tvremotewifibluetooth.data.model.RemoteCommand

@Composable
fun DPad(
    onCommand: (RemoteCommand) -> Unit,
    onStartRepeat: (RemoteCommand) -> Unit,
    onStopRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dpadShape = RoundedCornerShape(24.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(dpadShape),
        shape = dpadShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP ROW: UP (EXACT END-TO-END REGION)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onCommand(RemoteCommand.UP)
                                    tryAwaitRelease()
                                    onStopRepeat()
                                },
                                onLongPress = { onStartRepeat(RemoteCommand.UP) }
                            )
                        }
                )
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onCommand(RemoteCommand.UP)
                                    tryAwaitRelease()
                                    onStopRepeat()
                                },
                                onLongPress = { onStartRepeat(RemoteCommand.UP) }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Up",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(52.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onCommand(RemoteCommand.UP)
                                    tryAwaitRelease()
                                    onStopRepeat()
                                },
                                onLongPress = { onStartRepeat(RemoteCommand.UP) }
                            )
                        }
                )
            }

            // MIDDLE ROW: LEFT | OK | RIGHT (EXACT END-TO-END REGION)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
            ) {
                // LEFT BOX
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onCommand(RemoteCommand.LEFT)
                                    tryAwaitRelease()
                                    onStopRepeat()
                                },
                                onLongPress = { onStartRepeat(RemoteCommand.LEFT) }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Left",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(52.dp)
                    )
                }

                // CENTER OK BOX (PERFECT CIRCLE)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onCommand(RemoteCommand.ENTER) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "OK",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // RIGHT BOX
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onCommand(RemoteCommand.RIGHT)
                                    tryAwaitRelease()
                                    onStopRepeat()
                                },
                                onLongPress = { onStartRepeat(RemoteCommand.RIGHT) }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Right",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            // BOTTOM ROW: DOWN (EXACT END-TO-END REGION)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onCommand(RemoteCommand.DOWN)
                                    tryAwaitRelease()
                                    onStopRepeat()
                                },
                                onLongPress = { onStartRepeat(RemoteCommand.DOWN) }
                            )
                        }
                )
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onCommand(RemoteCommand.DOWN)
                                    tryAwaitRelease()
                                    onStopRepeat()
                                },
                                onLongPress = { onStartRepeat(RemoteCommand.DOWN) }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Down",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(52.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onCommand(RemoteCommand.DOWN)
                                    tryAwaitRelease()
                                    onStopRepeat()
                                },
                                onLongPress = { onStartRepeat(RemoteCommand.DOWN) }
                            )
                        }
                )
            }
        }
    }
}
