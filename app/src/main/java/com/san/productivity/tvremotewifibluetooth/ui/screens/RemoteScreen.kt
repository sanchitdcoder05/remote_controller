package com.san.productivity.tvremotewifibluetooth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionMode
import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionState
import com.san.productivity.tvremotewifibluetooth.data.model.RemoteCommand
import com.san.productivity.tvremotewifibluetooth.ui.components.DPad
import com.san.productivity.tvremotewifibluetooth.ui.components.RemoteButton
import com.san.productivity.tvremotewifibluetooth.viewmodel.RemoteViewModel
import kotlinx.coroutines.coroutineScope
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(
    viewModel: RemoteViewModel,
    onNavigateToDiscovery: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val currentDevice by viewModel.currentDevice.collectAsState()
    val savedDevices by viewModel.savedDevices.collectAsState()
    val lastMessage by viewModel.lastMessage.collectAsState()

    var showNumberPadDialog by remember { mutableStateOf(false) }
    var showTouchpadOverlay by remember { mutableStateOf(false) }
    var showTvSwitcherMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(lastMessage) {
        lastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearLastMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showTvSwitcherMenu = true }
                                .padding(vertical = 2.dp, horizontal = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentDevice?.displayName ?: "Samsung TV Remote",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Switch TV",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val (dotColor, statusText) = when (connectionState) {
                                    is ConnectionState.Connected -> Pair(Color(0xFF4CAF50), "Connected")
                                    is ConnectionState.Connecting -> Pair(Color(0xFFFFC107), "Connecting...")
                                    is ConnectionState.PairingRequested -> Pair(Color(0xFFFF9800), "Allow on TV screen")
                                    is ConnectionState.Error -> Pair(Color(0xFFF44336), "Error")
                                    else -> Pair(Color(0xFF9E9E9E), "Disconnected")
                                }
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(dotColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // MULTI-TV DEVICE SWITCHER DROPDOWN MENU
                        DropdownMenu(
                            expanded = showTvSwitcherMenu,
                            onDismissRequest = { showTvSwitcherMenu = false }
                        ) {
                            Text(
                                text = "Switch Connected TV",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                            HorizontalDivider()

                            if (savedDevices.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No saved TVs found", style = MaterialTheme.typography.bodySmall) },
                                    onClick = { showTvSwitcherMenu = false }
                                )
                            } else {
                                savedDevices.forEach { device ->
                                    val isSelected = currentDevice?.ipAddress == device.ipAddress || (currentDevice?.id == device.id && device.id.isNotBlank())
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Tv,
                                                    contentDescription = null,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = device.displayName,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Text(
                                                        text = if (device.ipAddress.isNotBlank()) device.ipAddress else device.bluetoothAddress,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        },
                                        trailingIcon = {
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectAndConnectDevice(device)
                                            showTvSwitcherMenu = false
                                        }
                                    )
                                }
                            }

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text("+ Pair / Add New TV", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showTvSwitcherMenu = false
                                    onNavigateToDiscovery()
                                }
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToDiscovery) {
                        Icon(Icons.Default.Tv, contentDescription = "Pair / Search TV")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            RemoteTabContent(
                viewModel = viewModel,
                onOpenTouchpad = { showTouchpadOverlay = true },
                onOpenNumberPad = { showNumberPadDialog = true }
            )
        }
    }

    // NUMBER PAD POPUP DIALOG
    if (showNumberPadDialog) {
        AlertDialog(
            onDismissRequest = { showNumberPadDialog = false },
            title = { Text("Number Pad") },
            text = { NumbersTabContent(viewModel) },
            confirmButton = {
                TextButton(onClick = { showNumberPadDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // FULLSCREEN OLED TOUCHPAD OVERLAY
    if (showTouchpadOverlay) {
        OledTouchpadDialog(
            onDismiss = { showTouchpadOverlay = false },
            onCommand = { viewModel.sendCommand(it) }
        )
    }
}

@Composable
fun RemoteTabContent(
    viewModel: RemoteViewModel,
    onOpenTouchpad: () -> Unit,
    onOpenNumberPad: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // WIDE ACTION BUTTONS ROW ABOVE D-PAD (MENU, GUIDE, TOUCH, EXIT)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RemoteButton(
                onClick = { viewModel.sendCommand(RemoteCommand.MENU) },
                icon = Icons.Default.Menu,
                text = "MENU",
                shape = RoundedCornerShape(14.dp),
                height = 54.dp,
                modifier = Modifier.weight(1f)
            )

            RemoteButton(
                onClick = { viewModel.sendCommand(RemoteCommand.GUIDE) },
                text = "GUIDE",
                shape = RoundedCornerShape(14.dp),
                height = 54.dp,
                modifier = Modifier.weight(1f)
            )

            RemoteButton(
                onClick = onOpenTouchpad,
                icon = Icons.Default.TouchApp,
                text = "TOUCH",
                shape = RoundedCornerShape(14.dp),
                height = 54.dp,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )

            RemoteButton(
                onClick = { viewModel.sendCommand(RemoteCommand.EXIT) },
                text = "EXIT",
                shape = RoundedCornerShape(14.dp),
                height = 54.dp,
                modifier = Modifier.weight(1f)
            )
        }

        // LOWER SECTION: VOLUME ROW ABOVE D-PAD & CLOSE BACK/HOME/123 BUTTONS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // DEDICATED VOLUME CONTROL ROW (VOL -, MUTE, VOL +)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RemoteButton(
                    onClick = { viewModel.sendCommand(RemoteCommand.VOLUME_DOWN) },
                    onLongPress = { viewModel.startRepeatCommand(RemoteCommand.VOLUME_DOWN) },
                    onRelease = { viewModel.stopRepeatCommand() },
                    icon = Icons.Default.VolumeDown,
                    text = "VOL -",
                    shape = RoundedCornerShape(14.dp),
                    height = 52.dp,
                    modifier = Modifier.weight(1f)
                )

                RemoteButton(
                    onClick = { viewModel.sendCommand(RemoteCommand.MUTE) },
                    icon = Icons.Default.VolumeMute,
                    text = "MUTE",
                    shape = RoundedCornerShape(14.dp),
                    height = 52.dp,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(0.9f)
                )

                RemoteButton(
                    onClick = { viewModel.sendCommand(RemoteCommand.VOLUME_UP) },
                    onLongPress = { viewModel.startRepeatCommand(RemoteCommand.VOLUME_UP) },
                    onRelease = { viewModel.stopRepeatCommand() },
                    icon = Icons.Default.VolumeUp,
                    text = "VOL +",
                    shape = RoundedCornerShape(14.dp),
                    height = 52.dp,
                    modifier = Modifier.weight(1f)
                )
            }

            DPad(
                onCommand = { viewModel.sendCommand(it) },
                onStartRepeat = { viewModel.startRepeatCommand(it) },
                onStopRepeat = { viewModel.stopRepeatCommand() }
            )

            // BACK, HOME, 123 NUMBERS BUTTONS (SUPER CLOSE TO D-PAD!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // BACK BUTTON (Left 33.3%)
                RemoteButton(
                    onClick = { viewModel.sendCommand(RemoteCommand.BACK) },
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    text = "BACK",
                    shape = RoundedCornerShape(16.dp),
                    height = 68.dp,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )

                // HOME BUTTON (Center 33.3%)
                RemoteButton(
                    onClick = { viewModel.sendCommand(RemoteCommand.HOME) },
                    icon = Icons.Default.Home,
                    text = "HOME",
                    shape = RoundedCornerShape(16.dp),
                    height = 68.dp,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )

                // 123 NUMBERS POP-UP BUTTON (Right 33.3%)
                RemoteButton(
                    onClick = onOpenNumberPad,
                    icon = Icons.Default.Apps,
                    text = "123",
                    shape = RoundedCornerShape(16.dp),
                    height = 68.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun NumbersTabContent(viewModel: RemoteViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Number Pad",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // 1, 2, 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RemoteButton(onClick = { viewModel.sendCommand(RemoteCommand.NUM_1) }, text = "1", size = 64.dp)
                    RemoteButton(onClick = { viewModel.sendCommand(RemoteCommand.NUM_2) }, text = "2", size = 64.dp)
                    RemoteButton(onClick = { viewModel.sendCommand(RemoteCommand.NUM_3) }, text = "3", size = 64.dp)
                }

                // 4, 5, 6
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RemoteButton(onClick = { viewModel.sendCommand(RemoteCommand.NUM_4) }, text = "4", size = 64.dp)
                    RemoteButton(onClick = { viewModel.sendCommand(RemoteCommand.NUM_5) }, text = "5", size = 64.dp)
                    RemoteButton(onClick = { viewModel.sendCommand(RemoteCommand.NUM_6) }, text = "6", size = 64.dp)
                }

                // 7, 8, 9
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RemoteButton(onClick = { viewModel.sendCommand(RemoteCommand.NUM_7) }, text = "7", size = 64.dp)
                    RemoteButton(onClick = { viewModel.sendCommand(RemoteCommand.NUM_8) }, text = "8", size = 64.dp)
                    RemoteButton(onClick = { viewModel.sendCommand(RemoteCommand.NUM_9) }, text = "9", size = 64.dp)
                }

                // PREV CH, 0, ENTER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RemoteButton(onClick = { viewModel.sendCommand(RemoteCommand.PREV_CHANNEL) }, text = "PREV", size = 64.dp)
                    RemoteButton(onClick = { viewModel.sendCommand(RemoteCommand.NUM_0) }, text = "0", size = 64.dp)
                    RemoteButton(onClick = { viewModel.sendCommand(RemoteCommand.ENTER) }, text = "OK", size = 64.dp, containerColor = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OledTouchpadDialog(
    onDismiss: () -> Unit,
    onCommand: (RemoteCommand) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.key) {
                            Key.VolumeUp -> {
                                onCommand(RemoteCommand.VOLUME_UP)
                                true
                            }
                            Key.VolumeDown -> {
                                onCommand(RemoteCommand.VOLUME_DOWN)
                                true
                            }
                            Key.VolumeMute -> {
                                onCommand(RemoteCommand.MUTE)
                                true
                            }
                            else -> false
                        }
                    } else false
                },
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(16.dp)
            ) {
                // TOP BAR: CLOSE BUTTON & TITLE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Touchpad",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "Swipe • Tap",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // WHOLE PITCH BLACK GESTURE TOUCHPAD SURFACE
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            coroutineScope {
                                awaitPointerEventScope {
                                    while (true) {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        var dragX = 0f
                                        var dragY = 0f
                                        var isDrag = false
                                        val startTime = System.currentTimeMillis()

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id }
                                            if (change == null || !change.pressed) {
                                                val duration = System.currentTimeMillis() - startTime
                                                if (!isDrag && duration < 300) {
                                                    onCommand(RemoteCommand.ENTER)
                                                }
                                                break
                                            }

                                            val dragAmount = change.positionChange()
                                            dragX += dragAmount.x
                                            dragY += dragAmount.y

                                            if (abs(dragX) > 30f || abs(dragY) > 30f) {
                                                isDrag = true
                                                change.consume()
                                                if (abs(dragX) > abs(dragY)) {
                                                    if (dragX > 0) onCommand(RemoteCommand.RIGHT) else onCommand(RemoteCommand.LEFT)
                                                } else {
                                                    if (dragY > 0) onCommand(RemoteCommand.DOWN) else onCommand(RemoteCommand.UP)
                                                }
                                                dragX = 0f
                                                dragY = 0f
                                            }
                                        }
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = Color.DarkGray,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Swipe to Navigate • Tap to Select",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SUBTLE DIVIDER BAR
                HorizontalDivider(
                    color = Color(0xFF222222),
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // BOTTOM CONTROL BAR DIVIDED BY 2 (LEFT: BACK, RIGHT: HOME) - LITTLE GRAY BUTTONS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT SIDE: BACK BUTTON (50%) - LITTLE GRAY
                    Button(
                        onClick = { onCommand(RemoteCommand.BACK) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF242424),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(24.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BACK", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }

                    // RIGHT SIDE: HOME BUTTON (50%) - LITTLE GRAY
                    Button(
                        onClick = { onCommand(RemoteCommand.HOME) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF242424),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(24.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("HOME", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
