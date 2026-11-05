package com.san.productivity.tvremotewifibluetooth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionMode
import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionState
import com.san.productivity.tvremotewifibluetooth.data.model.TvDevice
import com.san.productivity.tvremotewifibluetooth.data.repository.SamsungTvRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: SamsungTvRepository) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = repository.connectionState
    val currentDevice: StateFlow<TvDevice?> = repository.currentDevice
    val savedDevices: StateFlow<List<TvDevice>> = repository.savedDevices
    val connectionMode: StateFlow<ConnectionMode> = repository.connectionMode

    private val _manualIpInput = MutableStateFlow("")
    val manualIpInput: StateFlow<String> = _manualIpInput.asStateFlow()

    private val _manualMacInput = MutableStateFlow("")
    val manualMacInput: StateFlow<String> = _manualMacInput.asStateFlow()

    private val _showManualIpDialog = MutableStateFlow(false)
    val showManualIpDialog: StateFlow<Boolean> = _showManualIpDialog.asStateFlow()

    fun setManualIpInput(ip: String) {
        _manualIpInput.value = ip
    }

    fun setManualMacInput(mac: String) {
        _manualMacInput.value = mac
    }

    fun setShowManualIpDialog(show: Boolean) {
        _showManualIpDialog.value = show
    }

    fun setConnectionMode(mode: ConnectionMode) {
        viewModelScope.launch {
            repository.setConnectionMode(mode)
        }
    }

    fun connectToDevice(device: TvDevice) {
        viewModelScope.launch {
            repository.connectToTv(device)
        }
    }

    fun connectManualIp() {
        val ip = _manualIpInput.value.trim()
        val mac = _manualMacInput.value.trim()
        if (ip.isNotBlank()) {
            val device = TvDevice.createDefault(ip, mac)
            viewModelScope.launch {
                repository.connectToTv(device)
                _showManualIpDialog.value = false
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            repository.disconnect()
        }
    }

    fun removeSavedDevice(ipOrId: String) {
        viewModelScope.launch {
            repository.removeSavedDevice(ipOrId)
        }
    }

    fun powerOnTv(device: TvDevice) {
        viewModelScope.launch {
            repository.powerOnTv(device.macAddress, device.ipAddress)
        }
    }

    class Factory(private val repository: SamsungTvRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
