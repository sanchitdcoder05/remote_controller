package com.san.productivity.tvremotewifibluetooth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.san.productivity.tvremotewifibluetooth.data.local.TvPreferencesDataStore
import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionMode
import com.san.productivity.tvremotewifibluetooth.data.model.TvBrand
import com.san.productivity.tvremotewifibluetooth.data.model.TvDevice
import com.san.productivity.tvremotewifibluetooth.data.repository.SamsungTvRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SamsungTvRepository) : ViewModel() {

    private val prefs: TvPreferencesDataStore = repository.preferencesDataStore

    val themeMode: StateFlow<String> = prefs.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, "DARK")

    val defaultConnectionMode: StateFlow<ConnectionMode> = prefs.defaultConnectionMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionMode.WIFI)

    val selectedTvBrand: StateFlow<TvBrand> = prefs.selectedTvBrand
        .stateIn(viewModelScope, SharingStarted.Eagerly, TvBrand.SAMSUNG)

    val wifiEnabled: StateFlow<Boolean> = prefs.wifiEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val bluetoothEnabled: StateFlow<Boolean> = prefs.bluetoothEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val irEnabled: StateFlow<Boolean> = prefs.irEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val hasIrEmitter: Boolean = repository.irTransport.hasIrEmitter()

    val hapticFeedback: StateFlow<Boolean> = prefs.hapticFeedback
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val autoConnect: StateFlow<Boolean> = prefs.autoConnect
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val repeatDelayMs: StateFlow<Int> = prefs.repeatDelayMs
        .stateIn(viewModelScope, SharingStarted.Eagerly, 250)

    val selectedTvMac: StateFlow<String> = prefs.selectedTvMac
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val selectedTvIp: StateFlow<String> = prefs.selectedTvIp
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val savedDevices: StateFlow<List<TvDevice>> = repository.savedDevices

    fun removeSavedDevice(ipOrId: String) {
        viewModelScope.launch {
            repository.removeSavedDevice(ipOrId)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            prefs.setThemeMode(mode)
        }
    }

    fun setDefaultConnectionMode(mode: ConnectionMode) {
        viewModelScope.launch {
            repository.setConnectionMode(mode)
        }
    }

    fun setSelectedTvBrand(brand: TvBrand) {
        viewModelScope.launch {
            prefs.setSelectedTvBrand(brand)
        }
    }

    fun setWifiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setWifiEnabled(enabled)
        }
    }

    fun setBluetoothEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setBluetoothEnabled(enabled)
        }
    }

    fun setIrEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setIrEnabled(enabled)
        }
    }

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

    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setHapticFeedback(enabled)
        }
    }

    fun setAutoConnect(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setAutoConnect(enabled)
        }
    }

    fun setRepeatDelayMs(delayMs: Int) {
        viewModelScope.launch {
            prefs.setRepeatDelayMs(delayMs)
        }
    }

    fun saveMacAddress(mac: String) {
        viewModelScope.launch {
            val currentIp = selectedTvIp.value
            if (currentIp.isNotBlank()) {
                prefs.saveMacAddress(currentIp, mac)
            }
        }
    }

    fun clearSavedTokens() {
        viewModelScope.launch {
            prefs.clearTokens()
        }
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            repository.disconnect()
            prefs.clearAllData()
        }
    }

    class Factory(private val repository: SamsungTvRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository) as T
        }
    }
}
