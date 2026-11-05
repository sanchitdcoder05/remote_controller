package com.san.productivity.tvremotewifibluetooth.data.repository

import android.content.Context
import android.util.Log
import com.san.productivity.tvremotewifibluetooth.bluetooth.BluetoothHidTransport
import com.san.productivity.tvremotewifibluetooth.bluetooth.BluetoothManagerController
import com.san.productivity.tvremotewifibluetooth.data.local.TvPreferencesDataStore
import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionMode
import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionState
import com.san.productivity.tvremotewifibluetooth.data.model.RemoteCommand
import com.san.productivity.tvremotewifibluetooth.data.model.TvDevice
import com.san.productivity.tvremotewifibluetooth.domain.transport.RemoteTransport
import com.san.productivity.tvremotewifibluetooth.ir.IrRemoteTransport
import com.san.productivity.tvremotewifibluetooth.network.SamsungTvDiscovery
import com.san.productivity.tvremotewifibluetooth.network.SamsungTvWebSocketClient
import com.san.productivity.tvremotewifibluetooth.network.WakeOnLan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SamsungTvRepository(
    private val context: Context,
    val preferencesDataStore: TvPreferencesDataStore
) {

    private val TAG = "SamsungTvRepository"
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    val savedDevices: StateFlow<List<TvDevice>> = preferencesDataStore.savedDevices
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val wifiTransport = SamsungTvWebSocketClient(
        onTokenReceived = { ip, token ->
            scope.launch {
                preferencesDataStore.saveToken(ip, token)
            }
        }
    )

    val bluetoothTransport = BluetoothHidTransport(context)
    val irTransport = IrRemoteTransport(context)
    val bluetoothManagerController = BluetoothManagerController(context)
    val discovery = SamsungTvDiscovery(context)

    private val _connectionMode = MutableStateFlow(ConnectionMode.WIFI)
    val connectionMode: StateFlow<ConnectionMode> = _connectionMode.asStateFlow()

    private val _activeTransport = MutableStateFlow<RemoteTransport>(wifiTransport)

    val connectionState: StateFlow<ConnectionState> = _activeTransport
        .flatMapLatest { it.connectionState }
        .stateIn(scope, SharingStarted.Eagerly, ConnectionState.Disconnected)

    val currentDevice: StateFlow<TvDevice?> = _activeTransport
        .flatMapLatest { it.currentDevice }
        .stateIn(scope, SharingStarted.Eagerly, null)

    init {
        scope.launch {
            preferencesDataStore.defaultConnectionMode.collect { mode ->
                _connectionMode.value = mode
                _activeTransport.value = when (mode) {
                    ConnectionMode.BLUETOOTH -> bluetoothTransport
                    ConnectionMode.IR -> irTransport
                    else -> wifiTransport
                }
            }
        }
    }

    suspend fun setConnectionMode(mode: ConnectionMode) {
        if (_connectionMode.value != mode) {
            disconnect()
            _connectionMode.value = mode
            _activeTransport.value = when (mode) {
                ConnectionMode.BLUETOOTH -> bluetoothTransport
                ConnectionMode.IR -> irTransport
                else -> wifiTransport
            }
            preferencesDataStore.setDefaultConnectionMode(mode)
        }
    }

    suspend fun connectToTv(device: TvDevice): Result<Unit> {
        val savedToken = if (device.token.isNullOrEmpty() && device.connectionType == ConnectionMode.WIFI) {
            preferencesDataStore.getToken(device.ipAddress)
        } else {
            device.token
        }
        val targetDevice = device.copy(token = savedToken)

        preferencesDataStore.saveSelectedDevice(targetDevice)
        val transport = when (targetDevice.connectionType) {
            ConnectionMode.BLUETOOTH -> bluetoothTransport
            ConnectionMode.IR -> irTransport
            else -> wifiTransport
        }
        _activeTransport.value = transport
        return transport.connect(targetDevice)
    }

    suspend fun disconnect() {
        _activeTransport.value.disconnect()
    }

    suspend fun sendCommand(command: RemoteCommand): Result<Unit> {
        // Special case: If user clicks POWER on a disconnected TV, attempt Wake-On-LAN first
        if (command == RemoteCommand.POWER && connectionState.value !is ConnectionState.Connected) {
            val device = currentDevice.value
            val mac = device?.macAddress ?: ""
            val ip = device?.ipAddress ?: ""
            if (mac.isNotBlank()) {
                Log.d(TAG, "Attempting Wake-on-LAN for $ip ($mac)...")
                WakeOnLan.sendMagicPacket(mac, ip)
            }
        }
        return _activeTransport.value.sendCommand(command)
    }

    suspend fun sendText(text: String): Result<Unit> {
        return _activeTransport.value.sendText(text)
    }

    suspend fun launchApp(appId: String): Result<Unit> {
        return _activeTransport.value.launchApp(appId)
    }

    suspend fun startVoiceHold(): Result<Unit> {
        return _activeTransport.value.startVoiceHold()
    }

    suspend fun stopVoiceHold(): Result<Unit> {
        return _activeTransport.value.stopVoiceHold()
    }

    suspend fun powerOnTv(macAddress: String, ipAddress: String): Result<Unit> {
        if (macAddress.isBlank()) {
            return Result.failure(IllegalArgumentException("MAC address is required for Wake-on-LAN"))
        }
        preferencesDataStore.saveMacAddress(ipAddress, macAddress)
        return WakeOnLan.sendMagicPacket(macAddress, ipAddress)
    }

    suspend fun startWifiDiscovery() {
        discovery.startDiscovery()
    }

    suspend fun saveDevice(device: TvDevice) {
        preferencesDataStore.addSavedDevice(device)
    }

    suspend fun removeSavedDevice(ipOrId: String) {
        preferencesDataStore.removeSavedDevice(ipOrId)
    }
}
