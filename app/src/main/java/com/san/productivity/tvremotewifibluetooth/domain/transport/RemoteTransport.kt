package com.san.productivity.tvremotewifibluetooth.domain.transport

import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionState
import com.san.productivity.tvremotewifibluetooth.data.model.RemoteCommand
import com.san.productivity.tvremotewifibluetooth.data.model.TvDevice
import kotlinx.coroutines.flow.StateFlow

interface RemoteTransport {
    val connectionState: StateFlow<ConnectionState>
    val currentDevice: StateFlow<TvDevice?>

    suspend fun connect(device: TvDevice): Result<Unit>
    suspend fun disconnect()
    suspend fun sendCommand(command: RemoteCommand): Result<Unit>
    suspend fun sendText(text: String): Result<Unit>
    suspend fun launchApp(appId: String): Result<Unit>
    suspend fun startVoiceHold(): Result<Unit>
    suspend fun stopVoiceHold(): Result<Unit>
}
