package com.san.productivity.tvremotewifibluetooth.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionMode
import com.san.productivity.tvremotewifibluetooth.data.model.TvDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BluetoothManagerController(private val context: Context) {

    private val TAG = "BluetoothMgrCtrl"

    private val bluetoothManager: BluetoothManager? = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<TvDevice>>(emptyList())
    val pairedDevices: StateFlow<List<TvDevice>> = _pairedDevices.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<TvDevice>>(emptyList())
    val scannedDevices: StateFlow<List<TvDevice>> = _scannedDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    if (device != null && hasBluetoothConnectPermission()) {
                        val name = device.name ?: "Unknown Device (${device.address})"
                        val isTv = name.contains("TV", ignoreCase = true) || name.contains("Samsung", ignoreCase = true)
                        if (isTv) {
                            val tvDevice = TvDevice(
                                id = device.address,
                                name = name,
                                connectionType = ConnectionMode.BLUETOOTH,
                                bluetoothAddress = device.address,
                                model = "Samsung Bluetooth Device"
                            )
                            val current = _scannedDevices.value.toMutableList()
                            if (current.none { it.bluetoothAddress == device.address }) {
                                current.add(tvDevice)
                                _scannedDevices.value = current
                            }
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    _isScanning.value = true
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    _isBluetoothEnabled.value = (state == BluetoothAdapter.STATE_ON)
                }
            }
        }
    }

    init {
        loadPairedDevices()
        registerReceiver()
    }

    private fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        try {
            context.registerReceiver(receiver, filter)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering BT receiver: ${e.message}")
        }
    }

    fun unregisterReceiver() {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            // Receiver already unregistered
        }
    }

    @SuppressLint("MissingPermission")
    fun loadPairedDevices() {
        if (!hasBluetoothConnectPermission() || bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _pairedDevices.value = emptyList()
            return
        }

        try {
            val bonded = bluetoothAdapter.bondedDevices ?: emptySet()
            val tvList = bonded.map { device ->
                TvDevice(
                    id = device.address,
                    name = device.name ?: "Samsung TV (${device.address})",
                    connectionType = ConnectionMode.BLUETOOTH,
                    bluetoothAddress = device.address,
                    model = "Paired Bluetooth Device"
                )
            }
            _pairedDevices.value = tvList
        } catch (e: Exception) {
            Log.e(TAG, "Error getting paired BT devices: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!hasBluetoothScanPermission() || bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            return
        }

        try {
            _scannedDevices.value = emptyList()
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            bluetoothAdapter.startDiscovery()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting BT scan: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (hasBluetoothScanPermission() && bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter.cancelDiscovery()
        }
        _isScanning.value = false
    }

    fun hasRequiredPermissions(): Boolean {
        return hasBluetoothConnectPermission() && hasBluetoothScanPermission()
    }

    fun hasBluetoothConnectPermission(): Boolean {
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

    fun hasBluetoothScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
