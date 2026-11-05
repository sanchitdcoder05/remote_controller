package com.san.productivity.tvremotewifibluetooth.data.model

data class TvDevice(
    val id: String, // IP address or Bluetooth MAC address
    val name: String,
    val ipAddress: String = "",
    val macAddress: String = "",
    val model: String = "Samsung Smart TV",
    val port: Int = 8002,
    val token: String? = null,
    val connectionType: ConnectionMode = ConnectionMode.WIFI,
    val isSaved: Boolean = false,
    val lastConnected: Long = 0L,
    val bluetoothAddress: String = ""
) {
    val displayName: String
        get() = name.ifBlank { "Samsung TV ($ipAddress)" }

    companion object {
        fun createDefault(ip: String, mac: String = ""): TvDevice {
            return TvDevice(
                id = ip,
                name = "Samsung Smart TV",
                ipAddress = ip,
                macAddress = mac,
                model = "UA43AUE65AKXXL / Tizen TV",
                port = 8002,
                connectionType = ConnectionMode.WIFI
            )
        }
    }
}
