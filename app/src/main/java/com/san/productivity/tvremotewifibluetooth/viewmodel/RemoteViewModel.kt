package com.san.productivity.tvremotewifibluetooth.viewmodel

import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionState
import com.san.productivity.tvremotewifibluetooth.data.model.RemoteCommand
import com.san.productivity.tvremotewifibluetooth.data.model.TvDevice
import com.san.productivity.tvremotewifibluetooth.data.repository.SamsungTvRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class RemoteTab {
    REMOTE,
    NUMBERS,
    TOUCHPAD,
    KEYBOARD_APPS
}

class RemoteViewModel(
    private val context: Context,
    private val repository: SamsungTvRepository
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = repository.connectionState
    val currentDevice: StateFlow<TvDevice?> = repository.currentDevice
    val savedDevices: StateFlow<List<TvDevice>> = repository.savedDevices

    val hapticFeedbackEnabled: StateFlow<Boolean> = repository.preferencesDataStore.hapticFeedback
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _selectedTab = MutableStateFlow(RemoteTab.REMOTE)
    val selectedTab: StateFlow<RemoteTab> = _selectedTab.asStateFlow()

    private val _textInput = MutableStateFlow("")
    val textInput: StateFlow<String> = _textInput.asStateFlow()

    private val _lastMessage = MutableStateFlow<String?>(null)
    val lastMessage: StateFlow<String?> = _lastMessage.asStateFlow()

    private val commandMutex = Mutex()
    private var repeatJob: Job? = null

    init {
        viewModelScope.launch {
            repository.savedDevices.collect { savedList ->
                if (repository.connectionState.value is ConnectionState.Disconnected && savedList.isNotEmpty()) {
                    val lastDevice = savedList.maxByOrNull { it.lastConnected } ?: savedList.first()
                    repository.connectToTv(lastDevice)
                }
            }
        }
    }

    fun selectTab(tab: RemoteTab) {
        _selectedTab.value = tab
    }

    fun selectAndConnectDevice(device: TvDevice) {
        triggerHaptic()
        viewModelScope.launch {
            repository.connectToTv(device)
        }
    }

    fun setTextInput(text: String) {
        _textInput.value = text
    }

    fun sendCommand(command: RemoteCommand) {
        triggerHaptic()
        viewModelScope.launch {
            commandMutex.withLock {
                val result = repository.sendCommand(command)
                if (result.isFailure) {
                    _lastMessage.value = "Error: ${result.exceptionOrNull()?.localizedMessage}"
                }
            }
        }
    }

    fun startRepeatCommand(command: RemoteCommand) {
        triggerHaptic()
        repeatJob?.cancel()
        repeatJob = viewModelScope.launch {
            repository.sendCommand(command)
            delay(400) // Initial delay before repeating
            while (true) {
                triggerHaptic()
                repository.sendCommand(command)
                delay(150) // Repeat interval
            }
        }
    }

    fun stopRepeatCommand() {
        repeatJob?.cancel()
        repeatJob = null
    }

    fun startVoiceHold() {
        triggerHaptic()
        viewModelScope.launch {
            repository.startVoiceHold()
        }
    }

    fun stopVoiceHold() {
        triggerHaptic()
        viewModelScope.launch {
            repository.stopVoiceHold()
        }
    }

    fun sendTextDirect(text: String) {
        if (text.isNotBlank()) {
            triggerHaptic()
            viewModelScope.launch {
                val result = repository.sendText(text)
                if (result.isFailure) {
                    _lastMessage.value = "Voice search failed: ${result.exceptionOrNull()?.localizedMessage}"
                }
            }
        }
    }

    fun sendText() {
        val text = _textInput.value
        if (text.isNotBlank()) {
            triggerHaptic()
            viewModelScope.launch {
                val result = repository.sendText(text)
                if (result.isSuccess) {
                    _textInput.value = ""
                } else {
                    _lastMessage.value = "Text send failed: ${result.exceptionOrNull()?.localizedMessage}"
                }
            }
        }
    }

    fun launchApp(appId: String) {
        triggerHaptic()
        viewModelScope.launch {
            val result = repository.launchApp(appId)
            if (result.isFailure) {
                _lastMessage.value = "App launch failed: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun clearLastMessage() {
        _lastMessage.value = null
    }

    private fun triggerHaptic() {
        if (!hapticFeedbackEnabled.value) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(20)
                }
            }
        } catch (e: Exception) {
            // Ignore haptic error if permission/hardware missing
        }
    }

    class Factory(
        private val context: Context,
        private val repository: SamsungTvRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RemoteViewModel(context, repository) as T
        }
    }
}
