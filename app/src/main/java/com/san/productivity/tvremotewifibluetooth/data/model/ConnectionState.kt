package com.san.productivity.tvremotewifibluetooth.data.model

sealed interface ConnectionState {
    object Disconnected : ConnectionState
    object Discovering : ConnectionState
    data class Connecting(val message: String = "Connecting to TV...") : ConnectionState
    data class PairingRequested(val message: String = "Check your TV screen and allow pairing prompt") : ConnectionState
    data class Connected(val deviceName: String, val mode: ConnectionMode) : ConnectionState
    data class Reconnecting(val attempt: Int, val message: String = "Reconnecting...") : ConnectionState
    data class Error(val message: String, val canRetry: Boolean = true) : ConnectionState
}
