package com.san.productivity.tvremotewifibluetooth.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionMode
import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionState
import com.san.productivity.tvremotewifibluetooth.data.model.RemoteCommand
import com.san.productivity.tvremotewifibluetooth.data.model.TvDevice
import com.san.productivity.tvremotewifibluetooth.domain.transport.RemoteTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class BluetoothHidTransport(private val context: Context) : RemoteTransport {

    private val TAG = "BluetoothHidTransport"

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _currentDevice = MutableStateFlow<TvDevice?>(null)
    override val currentDevice: StateFlow<TvDevice?> = _currentDevice.asStateFlow()

    private val bluetoothManager: BluetoothManager? = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var hidDeviceProfile: Any? = null // BluetoothHidDevice on API 28+
    private var targetBluetoothDevice: BluetoothDevice? = null

    private val executor = Executors.newSingleThreadExecutor()

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            initProfileApi28()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun initProfileApi28() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
            bluetoothAdapter.getProfileProxy(
                context.applicationContext,
                object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                        if (profile == BluetoothProfile.HID_DEVICE && proxy is BluetoothHidDevice) {
                            Log.d(TAG, "BluetoothHidDevice profile connected")
                            hidDeviceProfile = proxy
                            registerAppApi28(proxy)
                        }
                    }

                    override fun onServiceDisconnected(profile: Int) {
                        if (profile == BluetoothProfile.HID_DEVICE) {
                            Log.d(TAG, "BluetoothHidDevice profile disconnected")
                            hidDeviceProfile = null
                            _connectionState.value = ConnectionState.Disconnected
                        }
                    }
                },
                BluetoothProfile.HID_DEVICE
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("MissingPermission")
    private fun registerAppApi28(proxy: BluetoothHidDevice) {
        if (!hasBluetoothConnectPermission()) return
        try {
            val sdpSettings = BluetoothHidDeviceAppSdpSettings(
                "Android TV Remote",
                "Samsung Remote Controller",
                "Android",
                BluetoothHidDevice.SUBCLASS1_COMBO,
                byteArrayOf(
                    // REPORT ID 1: Keyboard
                    0x05.toByte(), 0x01.toByte(), 0x09.toByte(), 0x06.toByte(), 0xa1.toByte(), 0x01.toByte(),
                    0x85.toByte(), 0x01.toByte(), 0x05.toByte(), 0x07.toByte(), 0x19.toByte(), 0xe0.toByte(),
                    0x29.toByte(), 0xe7.toByte(), 0x15.toByte(), 0x00.toByte(), 0x25.toByte(), 0x01.toByte(),
                    0x75.toByte(), 0x01.toByte(), 0x95.toByte(), 0x08.toByte(), 0x81.toByte(), 0x02.toByte(),
                    0x95.toByte(), 0x01.toByte(), 0x75.toByte(), 0x08.toByte(), 0x81.toByte(), 0x01.toByte(),
                    0x95.toByte(), 0x06.toByte(), 0x75.toByte(), 0x08.toByte(), 0x15.toByte(), 0x00.toByte(),
                    0x25.toByte(), 0x65.toByte(), 0x05.toByte(), 0x07.toByte(), 0x19.toByte(), 0x00.toByte(),
                    0x29.toByte(), 0x65.toByte(), 0x81.toByte(), 0x00.toByte(), 0xc0.toByte(),

                    // REPORT ID 2: Consumer Control (Media / Remote Keys)
                    0x05.toByte(), 0x0c.toByte(), 0x09.toByte(), 0x01.toByte(), 0xa1.toByte(), 0x01.toByte(),
                    0x85.toByte(), 0x02.toByte(), 0x15.toByte(), 0x00.toByte(), 0x26.toByte(), 0xff.toByte(), 0x03.toByte(),
                    0x19.toByte(), 0x00.toByte(), 0x2a.toByte(), 0xff.toByte(), 0x03.toByte(), 0x75.toByte(), 0x10.toByte(),
                    0x95.toByte(), 0x01.toByte(), 0x81.toByte(), 0x00.toByte(), 0xc0.toByte()
                )
            )

            val callback = object : BluetoothHidDevice.Callback() {
                override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
                    Log.d(TAG, "HID Device state changed: $state for ${device?.address}")
                    when (state) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            targetBluetoothDevice = device
                            val tvDevice = _currentDevice.value?.copy(
                                bluetoothAddress = device?.address ?: "",
                                name = device?.name ?: "Samsung TV (BT)"
                            ) ?: TvDevice(
                                id = device?.address ?: "BT",
                                name = device?.name ?: "Samsung TV (BT)",
                                connectionType = ConnectionMode.BLUETOOTH,
                                bluetoothAddress = device?.address ?: ""
                            )
                            _currentDevice.value = tvDevice
                            _connectionState.value = ConnectionState.Connected(
                                deviceName = tvDevice.displayName,
                                mode = ConnectionMode.BLUETOOTH
                            )
                        }
                        BluetoothProfile.STATE_CONNECTING -> {
                            _connectionState.value = ConnectionState.Connecting("Connecting over Bluetooth HID...")
                        }
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            _connectionState.value = ConnectionState.Disconnected
                        }
                    }
                }
            }

            proxy.registerApp(sdpSettings, null, null, executor, callback)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering HID app: ${e.message}", e)
        }
    }

    override suspend fun connect(device: TvDevice): Result<Unit> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            _connectionState.value = ConnectionState.Error("Bluetooth HID Emulation requires Android 9.0+ (API 28)")
            return Result.failure(UnsupportedOperationException("Requires API 28+"))
        }

        if (!hasBluetoothConnectPermission()) {
            _connectionState.value = ConnectionState.Error("Bluetooth Connect permission not granted")
            return Result.failure(SecurityException("Bluetooth CONNECT permission missing"))
        }

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _connectionState.value = ConnectionState.Error("Bluetooth is turned off")
            return Result.failure(IllegalStateException("Bluetooth disabled"))
        }

        val btAddress = device.bluetoothAddress.ifBlank { device.id }
        if (!BluetoothAdapter.checkBluetoothAddress(btAddress)) {
            _connectionState.value = ConnectionState.Error("Invalid Bluetooth address: $btAddress")
            return Result.failure(IllegalArgumentException("Invalid BT address"))
        }

        _currentDevice.value = device
        _connectionState.value = ConnectionState.Connecting("Connecting to Bluetooth device $btAddress...")

        return try {
            val btDevice = adapter.getRemoteDevice(btAddress)
            targetBluetoothDevice = btDevice

            val profile = hidDeviceProfile as? BluetoothHidDevice
            if (profile != null) {
                val connectedDevices = try {
                    profile.getDevicesMatchingConnectionStates(
                        intArrayOf(BluetoothProfile.STATE_CONNECTED, BluetoothProfile.STATE_CONNECTING)
                    )
                } catch (_: Exception) {
                    emptyList()
                }

                if (connectedDevices.contains(btDevice) || btDevice.bondState == BluetoothDevice.BOND_BONDED) {
                    _connectionState.value = ConnectionState.Connected(
                        deviceName = btDevice.name ?: device.name.ifBlank { "Samsung TV (BT)" },
                        mode = ConnectionMode.BLUETOOTH
                    )
                }

                profile.connect(btDevice)
                Result.success(Unit)
            } else {
                initProfileApi28()
                _connectionState.value = ConnectionState.Error("Bluetooth HID initializing. Please retry in a moment.")
                Result.failure(IllegalStateException("HID initializing"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Bluetooth connect failed: ${e.message}", e)
            _connectionState.value = ConnectionState.Error("Bluetooth error: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val profile = hidDeviceProfile as? BluetoothHidDevice
                val device = targetBluetoothDevice
                if (profile != null && device != null && hasBluetoothConnectPermission()) {
                    profile.disconnect(device)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error disconnecting BT HID: ${e.message}")
        }
        _connectionState.value = ConnectionState.Disconnected
        _currentDevice.value = null
    }

    @SuppressLint("MissingPermission")
    override suspend fun sendCommand(command: RemoteCommand): Result<Unit> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return@withContext Result.failure(UnsupportedOperationException("Bluetooth HID requires Android 9.0+"))
        }

        val profile = hidDeviceProfile as? BluetoothHidDevice
        val btAddress = _currentDevice.value?.bluetoothAddress?.ifBlank { _currentDevice.value?.id } ?: ""
        val device = targetBluetoothDevice ?: if (btAddress.isNotBlank()) bluetoothAdapter?.getRemoteDevice(btAddress) else null

        if (profile == null || device == null) {
            return@withContext Result.failure(IllegalStateException("Bluetooth HID not ready"))
        }

        return@withContext try {
            var sentSuccess = false

            val keyCode = mapCommandToKeyboardKeyCode(command)
            if (keyCode != 0.toByte()) {
                // Send 8-byte Standard HID Keyboard Report (Report ID 1)
                val pressReport = byteArrayOf(0x00, 0x00, keyCode, 0x00, 0x00, 0x00, 0x00, 0x00)
                val sentPress = profile.sendReport(device, 1, pressReport)

                kotlinx.coroutines.delay(25)

                val releaseReport = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
                val sentRelease = profile.sendReport(device, 1, releaseReport)

                kotlinx.coroutines.delay(15)

                if (sentPress && sentRelease) {
                    sentSuccess = true
                }
            }

            val consumerCode = mapCommandToConsumerCode(command)
            if (consumerCode != 0) {
                // Send 2-byte Consumer Control Report (Report ID 2)
                val low = (consumerCode and 0xFF).toByte()
                val high = ((consumerCode shr 8) and 0xFF).toByte()
                val pressReport = byteArrayOf(low, high)
                val sentPress = profile.sendReport(device, 2, pressReport)

                kotlinx.coroutines.delay(25)

                val releaseReport = byteArrayOf(0x00, 0x00)
                val sentRelease = profile.sendReport(device, 2, releaseReport)

                kotlinx.coroutines.delay(15)

                if (sentPress && sentRelease) {
                    sentSuccess = true
                }
            }

            if (sentSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(UnsupportedOperationException("Command ${command.name} failed over Bluetooth HID"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending BT HID report for ${command.name}: ${e.message}", e)
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun sendText(text: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return@withContext Result.failure(UnsupportedOperationException("Bluetooth HID requires Android 9.0+"))
        }

        val profile = hidDeviceProfile as? BluetoothHidDevice
        val device = targetBluetoothDevice
        if (profile == null || device == null || _connectionState.value !is ConnectionState.Connected) {
            return@withContext Result.failure(IllegalStateException("BT HID disconnected"))
        }

        return@withContext try {
            for (char in text) {
                val keyCode = charToHidKeyCode(char)
                if (keyCode != 0.toByte()) {
                    val press = byteArrayOf(0x00, 0x00, keyCode, 0x00, 0x00, 0x00, 0x00, 0x00)
                    profile.sendReport(device, 1, press)
                    kotlinx.coroutines.delay(40)
                    val release = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
                    profile.sendReport(device, 1, release)
                    kotlinx.coroutines.delay(30)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun launchApp(appId: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException("App launching is not supported over Bluetooth HID profile"))
    }

    @SuppressLint("MissingPermission")
    override suspend fun startVoiceHold(): Result<Unit> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return@withContext Result.failure(UnsupportedOperationException("Requires API 28+"))
        }
        val profile = hidDeviceProfile as? BluetoothHidDevice
        val device = targetBluetoothDevice
        if (profile == null || device == null) {
            return@withContext Result.failure(IllegalStateException("BT HID not ready"))
        }
        return@withContext try {
            val pressReport = byteArrayOf(0x21.toByte(), 0x02.toByte())
            val sent = profile.sendReport(device, 2, pressReport)
            if (sent) Result.success(Unit) else Result.failure(RuntimeException("Failed BT HID Voice press"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun stopVoiceHold(): Result<Unit> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return@withContext Result.failure(UnsupportedOperationException("Requires API 28+"))
        }
        val profile = hidDeviceProfile as? BluetoothHidDevice
        val device = targetBluetoothDevice
        if (profile == null || device == null) {
            return@withContext Result.failure(IllegalStateException("BT HID not ready"))
        }
        return@withContext try {
            val releaseReport = byteArrayOf(0x00, 0x00)
            val sent = profile.sendReport(device, 2, releaseReport)
            if (sent) Result.success(Unit) else Result.failure(RuntimeException("Failed BT HID Voice release"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapCommandToConsumerCode(command: RemoteCommand): Int {
        return when (command) {
            RemoteCommand.POWER, RemoteCommand.POWER_OFF -> 0x0030
            RemoteCommand.VOLUME_UP -> 0x00E9
            RemoteCommand.VOLUME_DOWN -> 0x00EA
            RemoteCommand.MUTE -> 0x00E2
            RemoteCommand.CHANNEL_UP -> 0x009C
            RemoteCommand.CHANNEL_DOWN -> 0x009D
            RemoteCommand.PLAY -> 0x00B0
            RemoteCommand.PAUSE -> 0x00B1
            RemoteCommand.STOP -> 0x00B7
            RemoteCommand.REWIND -> 0x00B4
            RemoteCommand.FAST_FORWARD -> 0x00B3
            RemoteCommand.HOME -> 0x0223
            RemoteCommand.BACK -> 0x0224
            RemoteCommand.MENU -> 0x0040
            RemoteCommand.SOURCE -> 0x01BB
            RemoteCommand.INFO -> 0x01BD
            RemoteCommand.GUIDE -> 0x008D
            RemoteCommand.EXIT -> 0x0226
            RemoteCommand.VOICE, RemoteCommand.SEARCH -> 0x0221
            else -> 0
        }
    }

    private fun mapCommandToKeyboardKeyCode(command: RemoteCommand): Byte {
        return when (command) {
            RemoteCommand.POWER, RemoteCommand.POWER_OFF -> 0x66.toByte() // Keyboard Power
            RemoteCommand.UP -> 0x52.toByte()
            RemoteCommand.DOWN -> 0x51.toByte()
            RemoteCommand.LEFT -> 0x50.toByte()
            RemoteCommand.RIGHT -> 0x4F.toByte()
            RemoteCommand.ENTER -> 0x28.toByte()
            RemoteCommand.BACK -> 0x29.toByte()
            RemoteCommand.HOME -> 0xE3.toByte() // Keyboard GUI/Meta (Home)
            RemoteCommand.VOLUME_UP -> 0x80.toByte() // Keyboard Vol Up
            RemoteCommand.VOLUME_DOWN -> 0x81.toByte() // Keyboard Vol Down
            RemoteCommand.MUTE -> 0x7F.toByte() // Keyboard Mute
            RemoteCommand.CHANNEL_UP -> 0x4B.toByte()
            RemoteCommand.CHANNEL_DOWN -> 0x4E.toByte()
            RemoteCommand.PREV_CHANNEL -> 0x2A.toByte()
            RemoteCommand.PLAY, RemoteCommand.PAUSE -> 0xE8.toByte()
            RemoteCommand.STOP -> 0xE9.toByte()
            RemoteCommand.REWIND -> 0xEA.toByte()
            RemoteCommand.FAST_FORWARD -> 0xEB.toByte()
            RemoteCommand.SOURCE -> 0x3A.toByte()
            RemoteCommand.MENU -> 0x65.toByte()
            RemoteCommand.INFO -> 0x3D.toByte()
            RemoteCommand.GUIDE -> 0x41.toByte()
            RemoteCommand.EXIT -> 0x29.toByte()
            RemoteCommand.SETTINGS, RemoteCommand.TOOLS -> 0x3E.toByte() // F5
            RemoteCommand.RED -> 0x3A.toByte()
            RemoteCommand.GREEN -> 0x3B.toByte()
            RemoteCommand.YELLOW -> 0x3C.toByte()
            RemoteCommand.BLUE -> 0x3D.toByte()
            RemoteCommand.NUM_0 -> 0x27.toByte()
            RemoteCommand.NUM_1 -> 0x1E.toByte()
            RemoteCommand.NUM_2 -> 0x1F.toByte()
            RemoteCommand.NUM_3 -> 0x20.toByte()
            RemoteCommand.NUM_4 -> 0x21.toByte()
            RemoteCommand.NUM_5 -> 0x22.toByte()
            RemoteCommand.NUM_6 -> 0x23.toByte()
            RemoteCommand.NUM_7 -> 0x24.toByte()
            RemoteCommand.NUM_8 -> 0x25.toByte()
            RemoteCommand.NUM_9 -> 0x26.toByte()
            else -> 0x00.toByte()
        }
    }

    private fun charToHidKeyCode(char: Char): Byte {
        return when (char.lowercaseChar()) {
            'a' -> 0x04.toByte()
            'b' -> 0x05.toByte()
            'c' -> 0x06.toByte()
            'd' -> 0x07.toByte()
            'e' -> 0x08.toByte()
            'f' -> 0x09.toByte()
            'g' -> 0x0A.toByte()
            'h' -> 0x0B.toByte()
            'i' -> 0x0C.toByte()
            'j' -> 0x0D.toByte()
            'k' -> 0x0E.toByte()
            'l' -> 0x0F.toByte()
            'm' -> 0x10.toByte()
            'n' -> 0x11.toByte()
            'o' -> 0x12.toByte()
            'p' -> 0x13.toByte()
            'q' -> 0x14.toByte()
            'r' -> 0x15.toByte()
            's' -> 0x16.toByte()
            't' -> 0x17.toByte()
            'u' -> 0x18.toByte()
            'v' -> 0x19.toByte()
            'w' -> 0x1A.toByte()
            'x' -> 0x1B.toByte()
            'y' -> 0x1C.toByte()
            'z' -> 0x1D.toByte()
            '1' -> 0x1E.toByte()
            '2' -> 0x1F.toByte()
            '3' -> 0x20.toByte()
            '4' -> 0x21.toByte()
            '5' -> 0x22.toByte()
            '6' -> 0x23.toByte()
            '7' -> 0x24.toByte()
            '8' -> 0x25.toByte()
            '9' -> 0x26.toByte()
            '0' -> 0x27.toByte()
            ' ' -> 0x2C.toByte()
            else -> 0x00.toByte()
        }
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
