package com.san.productivity.tvremotewifibluetooth.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import android.util.Log
import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionMode
import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionState
import com.san.productivity.tvremotewifibluetooth.data.model.RemoteCommand
import com.san.productivity.tvremotewifibluetooth.data.model.TvDevice
import com.san.productivity.tvremotewifibluetooth.domain.transport.RemoteTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class IrEngineMode(val displayName: String, val description: String) {
    UNIVERSAL_MULTI("Universal Multi-Pulse (All Phones & TVs)", "Sends microseconds, cycles & legacy codes"),
    STANDARD_MICROSECONDS("Standard Microseconds (Pixel/Samsung/Motorola)", "Standard Android AOSP timing"),
    OEM_CYCLES("OEM Cycles Mode (Xiaomi/Poco/Redmi/HTC)", "Converts microseconds to carrier cycles")
}

class IrRemoteTransport(private val context: Context) : RemoteTransport {

    private val TAG = "IrRemoteTransport"

    private val irManager: ConsumerIrManager? =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _currentDevice = MutableStateFlow<TvDevice?>(null)
    override val currentDevice: StateFlow<TvDevice?> = _currentDevice.asStateFlow()

    var engineMode: IrEngineMode = IrEngineMode.UNIVERSAL_MULTI

    fun hasIrEmitter(): Boolean {
        return irManager?.hasIrEmitter() == true
    }

    fun getOptimalFrequency(): Int {
        val manager = irManager ?: return 38000
        return try {
            val ranges = manager.carrierFrequencies
            if (ranges != null) {
                for (range in ranges) {
                    if (37900 in range.minFrequency..range.maxFrequency) return 37900
                    if (38000 in range.minFrequency..range.maxFrequency) return 38000
                    if (36000 in range.minFrequency..range.maxFrequency) return 36000
                }
                if (ranges.isNotEmpty()) ranges[0].minFrequency else 38000
            } else {
                38000
            }
        } catch (e: Exception) {
            38000
        }
    }

    override suspend fun connect(device: TvDevice): Result<Unit> {
        if (!hasIrEmitter()) {
            _connectionState.value = ConnectionState.Error("This phone does not have a built-in IR Blaster")
            return Result.failure(IllegalStateException("No IR emitter available"))
        }

        val target = device.copy(connectionType = ConnectionMode.IR)
        _currentDevice.value = target
        _connectionState.value = ConnectionState.Connected(
            deviceName = target.displayName.ifBlank { "Samsung TV (IR)" },
            mode = ConnectionMode.IR
        )
        return Result.success(Unit)
    }

    override suspend fun disconnect() {
        _connectionState.value = ConnectionState.Disconnected
        _currentDevice.value = null
    }

    override suspend fun sendCommand(command: RemoteCommand): Result<Unit> {
        val manager = irManager
        if (manager == null || !manager.hasIrEmitter()) {
            return Result.failure(IllegalStateException("IR Blaster not supported on this device"))
        }

        val keyByte = getSamsungKeyByte(command)
        if (keyByte == 0) {
            return Result.failure(UnsupportedOperationException("Command ${command.name} has no IR code"))
        }

        return try {
            val frequency = getOptimalFrequency()
            val invKey = (keyByte.inv() and 0xFF)

            // Bytes: [CustomerLow, CustomerHigh, Key, InvKey]
            val bytesSmart = byteArrayOf(0x07.toByte(), 0x07.toByte(), keyByte.toByte(), invKey.toByte())
            val bytesLegacy = byteArrayOf(0x00.toByte(), 0xE0.toByte(), keyByte.toByte(), invKey.toByte())

            when (engineMode) {
                IrEngineMode.STANDARD_MICROSECONDS -> {
                    val pattern = generateNecPattern(bytesSmart, leaderPulse = 4500, useCycles = false)
                    manager.transmit(frequency, pattern)
                }
                IrEngineMode.OEM_CYCLES -> {
                    val pattern = generateNecPattern(bytesSmart, leaderPulse = 4500, useCycles = true, frequency = frequency)
                    manager.transmit(frequency, pattern)
                }
                IrEngineMode.UNIVERSAL_MULTI -> {
                    // Transmit 1: Smart TV Microseconds (0x0707, 4500us)
                    val p1 = generateNecPattern(bytesSmart, leaderPulse = 4500, useCycles = false)
                    manager.transmit(frequency, p1)

                    // Transmit 2: Legacy Samsung Microseconds (0x00E0, 9000us)
                    try {
                        kotlinx.coroutines.delay(40)
                        val p2 = generateNecPattern(bytesLegacy, leaderPulse = 9000, useCycles = false)
                        manager.transmit(frequency, p2)
                    } catch (_: Exception) {}

                    // Transmit 3: OEM Cycles Pattern (for Xiaomi/Poco/HTC driver HALs)
                    try {
                        kotlinx.coroutines.delay(40)
                        val p3 = generateNecPattern(bytesSmart, leaderPulse = 4500, useCycles = true, frequency = frequency)
                        manager.transmit(frequency, p3)
                    } catch (_: Exception) {}
                }
            }

            if (_connectionState.value !is ConnectionState.Connected) {
                _connectionState.value = ConnectionState.Connected(
                    deviceName = _currentDevice.value?.displayName ?: "Samsung TV (IR)",
                    mode = ConnectionMode.IR
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error transmitting IR pattern: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun sendText(text: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException("Text typing is not supported over Infrared"))
    }

    override suspend fun launchApp(appId: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException("App launching is not supported over Infrared"))
    }

    override suspend fun startVoiceHold(): Result<Unit> {
        return sendCommand(RemoteCommand.VOICE)
    }

    override suspend fun stopVoiceHold(): Result<Unit> {
        return Result.success(Unit)
    }

    private fun generateNecPattern(
        bytes: ByteArray,
        leaderPulse: Int = 4500,
        leaderSpace: Int = 4500,
        useCycles: Boolean = false,
        frequency: Int = 38000
    ): IntArray {
        val patternUs = ArrayList<Int>()

        // Leader Pulse & Space
        patternUs.add(leaderPulse)
        patternUs.add(leaderSpace)

        // 4 Bytes LSB-First (Bit 0 to Bit 7)
        for (b in bytes) {
            val intVal = b.toInt() and 0xFF
            for (bitIdx in 0..7) {
                val bit = (intVal shr bitIdx) and 1
                patternUs.add(560)
                if (bit == 1) {
                    patternUs.add(1690)
                } else {
                    patternUs.add(560)
                }
            }
        }

        // Stop bit
        patternUs.add(560)

        if (!useCycles) {
            return patternUs.toIntArray()
        }

        // Convert microseconds to carrier cycle counts for OEM IR HALs
        val patternCycles = IntArray(patternUs.size)
        for (i in patternUs.indices) {
            patternCycles[i] = ((patternUs[i].toLong() * frequency + 500000L) / 1000000L).toInt()
        }
        return patternCycles
    }

    private fun getSamsungKeyByte(command: RemoteCommand): Int {
        return when (command) {
            RemoteCommand.POWER, RemoteCommand.POWER_OFF -> 0x02
            RemoteCommand.SOURCE -> 0x01
            RemoteCommand.MENU -> 0x1A
            RemoteCommand.GUIDE -> 0x4F
            RemoteCommand.INFO -> 0x1F
            RemoteCommand.EXIT -> 0x2B
            RemoteCommand.UP -> 0x60
            RemoteCommand.DOWN -> 0x61
            RemoteCommand.LEFT -> 0x65
            RemoteCommand.RIGHT -> 0x62
            RemoteCommand.ENTER -> 0x68
            RemoteCommand.BACK -> 0x58
            RemoteCommand.HOME -> 0x79
            RemoteCommand.VOLUME_UP -> 0x07
            RemoteCommand.VOLUME_DOWN -> 0x0B
            RemoteCommand.MUTE -> 0x0F
            RemoteCommand.CHANNEL_UP -> 0x12
            RemoteCommand.CHANNEL_DOWN -> 0x10
            RemoteCommand.PREV_CHANNEL -> 0x13
            RemoteCommand.PLAY -> 0x47
            RemoteCommand.PAUSE -> 0x4A
            RemoteCommand.STOP -> 0x46
            RemoteCommand.REWIND -> 0x45
            RemoteCommand.FAST_FORWARD -> 0x48
            RemoteCommand.RED -> 0x6C
            RemoteCommand.GREEN -> 0x6D
            RemoteCommand.YELLOW -> 0x6E
            RemoteCommand.BLUE -> 0x6F
            RemoteCommand.NUM_0 -> 0x11
            RemoteCommand.NUM_1 -> 0x04
            RemoteCommand.NUM_2 -> 0x05
            RemoteCommand.NUM_3 -> 0x06
            RemoteCommand.NUM_4 -> 0x08
            RemoteCommand.NUM_5 -> 0x09
            RemoteCommand.NUM_6 -> 0x0A
            RemoteCommand.NUM_7 -> 0x0C
            RemoteCommand.NUM_8 -> 0x0D
            RemoteCommand.NUM_9 -> 0x0E
            else -> 0x00
        }
    }
}
