package com.san.productivity.tvremotewifibluetooth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionMode
import com.san.productivity.tvremotewifibluetooth.data.model.TvDevice
import com.san.productivity.tvremotewifibluetooth.data.repository.SamsungTvRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DeviceDiscoveryViewModel(private val repository: SamsungTvRepository) : ViewModel() {

    val isScanning: StateFlow<Boolean> = repository.discovery.isScanning
    val savedDevices: StateFlow<List<TvDevice>> = repository.savedDevices
    val hasIrEmitter: Boolean = repository.irTransport.hasIrEmitter()

    fun setupIrDevice() {
        viewModelScope.launch {
            val irDevice = TvDevice(
                id = "IR_SAMSUNG_TV",
                name = "Samsung TV (IR Blaster)",
                connectionType = ConnectionMode.IR,
                model = "Infrared Controlled TV"
            )
            repository.connectToTv(irDevice)
            repository.setConnectionMode(ConnectionMode.IR)
        }
    }

    val discoveredDevices: StateFlow<List<TvDevice>> = combine(
        repository.discovery.discoveredDevices,
        repository.bluetoothManagerController.pairedDevices,
        repository.bluetoothManagerController.scannedDevices
    ) { wifiList, btPaired, btScanned ->
        val combined = mutableListOf<TvDevice>()
        combined.addAll(wifiList)

        btPaired.forEach { btDev ->
            if (combined.none { (it.bluetoothAddress.isNotBlank() && it.bluetoothAddress == btDev.bluetoothAddress) || (it.id == btDev.id) }) {
                combined.add(btDev)
            }
        }

        btScanned.forEach { btDev ->
            if (combined.none { (it.bluetoothAddress.isNotBlank() && it.bluetoothAddress == btDev.bluetoothAddress) || (it.id == btDev.id) }) {
                combined.add(btDev)
            }
        }

        combined
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        startScan()
    }

    fun startScan() {
        viewModelScope.launch {
            repository.startWifiDiscovery()
            repository.bluetoothManagerController.loadPairedDevices()
            repository.bluetoothManagerController.startScan()
        }
    }

    fun connectToDevice(device: TvDevice) {
        viewModelScope.launch {
            repository.connectToTv(device)
        }
    }

    fun saveDevice(device: TvDevice) {
        viewModelScope.launch {
            repository.saveDevice(device)
        }
    }

    fun removeSavedDevice(ipOrId: String) {
        viewModelScope.launch {
            repository.removeSavedDevice(ipOrId)
        }
    }

    class Factory(private val repository: SamsungTvRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DeviceDiscoveryViewModel(repository) as T
        }
    }
}
