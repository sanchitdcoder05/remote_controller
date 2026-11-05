package com.san.productivity.tvremotewifibluetooth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionMode
import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionState
import com.san.productivity.tvremotewifibluetooth.data.model.TvDevice
import com.san.productivity.tvremotewifibluetooth.data.repository.SamsungTvRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BluetoothViewModel(private val repository: SamsungTvRepository) : ViewModel() {

    val isBluetoothEnabled: StateFlow<Boolean> = repository.bluetoothManagerController.isBluetoothEnabled
    val pairedDevices: StateFlow<List<TvDevice>> = repository.bluetoothManagerController.pairedDevices
    val scannedDevices: StateFlow<List<TvDevice>> = repository.bluetoothManagerController.scannedDevices
    val isScanning: StateFlow<Boolean> = repository.bluetoothManagerController.isScanning

    val connectionState: StateFlow<ConnectionState> = repository.connectionState
    val currentDevice: StateFlow<TvDevice?> = repository.currentDevice

    fun loadPairedDevices() {
        repository.bluetoothManagerController.loadPairedDevices()
    }

    fun startScan() {
        repository.bluetoothManagerController.startScan()
    }

    fun stopScan() {
        repository.bluetoothManagerController.stopScan()
    }

    fun connectBluetoothTv(device: TvDevice) {
        viewModelScope.launch {
            repository.setConnectionMode(ConnectionMode.BLUETOOTH)
            repository.connectToTv(device)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            repository.disconnect()
        }
    }

    fun hasPermissions(): Boolean {
        return repository.bluetoothManagerController.hasRequiredPermissions()
    }

    class Factory(private val repository: SamsungTvRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BluetoothViewModel(repository) as T
        }
    }
}
