package com.san.productivity.tvremotewifibluetooth.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionMode
import com.san.productivity.tvremotewifibluetooth.data.model.TvBrand
import com.san.productivity.tvremotewifibluetooth.data.model.TvDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tv_remote_settings")

class TvPreferencesDataStore(private val context: Context) {

    private object Keys {
        val SELECTED_TV_IP = stringPreferencesKey("selected_tv_ip")
        val SELECTED_TV_NAME = stringPreferencesKey("selected_tv_name")
        val SELECTED_TV_MAC = stringPreferencesKey("selected_tv_mac")
        val SELECTED_TV_PORT = intPreferencesKey("selected_tv_port")
        val SAVED_DEVICES_JSON = stringPreferencesKey("saved_devices_json")
        val TOKENS_JSON = stringPreferencesKey("tokens_json")
        val MACS_JSON = stringPreferencesKey("macs_json")
        val THEME_MODE = stringPreferencesKey("theme_mode") // SYSTEM, DARK, LIGHT
        val DEFAULT_CONNECTION_MODE = stringPreferencesKey("default_connection_mode") // WIFI, BLUETOOTH, IR
        val WIFI_ENABLED = booleanPreferencesKey("wifi_enabled")
        val BLUETOOTH_ENABLED = booleanPreferencesKey("bluetooth_enabled")
        val IR_ENABLED = booleanPreferencesKey("ir_enabled")
        val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val BUTTON_SOUND = booleanPreferencesKey("button_sound")
        val REPEAT_DELAY_MS = intPreferencesKey("repeat_delay_ms")
        val SAVED_BLUETOOTH_ADDRESS = stringPreferencesKey("saved_bluetooth_address")
        val SELECTED_TV_BRAND = stringPreferencesKey("selected_tv_brand")
    }

    val selectedTvBrand: Flow<TvBrand> = context.dataStore.data.map { prefs ->
        val brandStr = prefs[Keys.SELECTED_TV_BRAND] ?: "SAMSUNG"
        try {
            TvBrand.valueOf(brandStr)
        } catch (e: Exception) {
            TvBrand.SAMSUNG
        }
    }

    val selectedTvIp: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.SELECTED_TV_IP] ?: ""
    }

    val selectedTvName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.SELECTED_TV_NAME] ?: "Samsung Smart TV"
    }

    val selectedTvMac: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.SELECTED_TV_MAC] ?: ""
    }

    val selectedTvPort: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.SELECTED_TV_PORT] ?: 8002
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE] ?: "DARK"
    }

    val defaultConnectionMode: Flow<ConnectionMode> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.DEFAULT_CONNECTION_MODE]) {
            "BLUETOOTH" -> ConnectionMode.BLUETOOTH
            "IR" -> ConnectionMode.IR
            else -> ConnectionMode.WIFI
        }
    }

    val wifiEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.WIFI_ENABLED] ?: true
    }

    val bluetoothEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.BLUETOOTH_ENABLED] ?: true
    }

    val irEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.IR_ENABLED] ?: true
    }

    val autoConnect: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_CONNECT] ?: true
    }

    val hapticFeedback: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HAPTIC_FEEDBACK] ?: true
    }

    val buttonSound: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.BUTTON_SOUND] ?: false
    }

    val repeatDelayMs: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.REPEAT_DELAY_MS] ?: 250
    }

    val savedBluetoothAddress: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.SAVED_BLUETOOTH_ADDRESS] ?: ""
    }

    val savedDevices: Flow<List<TvDevice>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[Keys.SAVED_DEVICES_JSON] ?: "[]"
        parseSavedDevices(jsonStr)
    }

    suspend fun saveSelectedDevice(device: TvDevice) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_TV_IP] = device.ipAddress
            prefs[Keys.SELECTED_TV_NAME] = device.name
            prefs[Keys.SELECTED_TV_MAC] = device.macAddress
            prefs[Keys.SELECTED_TV_PORT] = device.port
            if (device.bluetoothAddress.isNotBlank()) {
                prefs[Keys.SAVED_BLUETOOTH_ADDRESS] = device.bluetoothAddress
            }
        }
        addSavedDevice(device)
    }

    suspend fun addSavedDevice(device: TvDevice) {
        context.dataStore.edit { prefs ->
            val existingJson = prefs[Keys.SAVED_DEVICES_JSON] ?: "[]"
            val devices = parseSavedDevices(existingJson).toMutableList()
            val index = devices.indexOfFirst { existing ->
                if (device.ipAddress.isNotBlank() && existing.ipAddress.isNotBlank()) {
                    existing.ipAddress == device.ipAddress
                } else if (device.bluetoothAddress.isNotBlank() && existing.bluetoothAddress.isNotBlank()) {
                    existing.bluetoothAddress == device.bluetoothAddress
                } else {
                    existing.id == device.id && device.id.isNotBlank()
                }
            }
            val updatedDevice = device.copy(isSaved = true, lastConnected = System.currentTimeMillis())
            if (index >= 0) {
                devices[index] = updatedDevice
            } else {
                devices.add(updatedDevice)
            }
            prefs[Keys.SAVED_DEVICES_JSON] = serializeSavedDevices(devices)
        }
    }

    suspend fun removeSavedDevice(ipOrId: String) {
        if (ipOrId.isBlank()) return
        context.dataStore.edit { prefs ->
            val existingJson = prefs[Keys.SAVED_DEVICES_JSON] ?: "[]"
            val devices = parseSavedDevices(existingJson).filterNot { existing ->
                existing.ipAddress == ipOrId || existing.bluetoothAddress == ipOrId || existing.id == ipOrId
            }
            prefs[Keys.SAVED_DEVICES_JSON] = serializeSavedDevices(devices)
        }
    }

    suspend fun saveToken(ipAddress: String, token: String) {
        if (ipAddress.isBlank() || token.isBlank()) return
        context.dataStore.edit { prefs ->
            val tokensJson = prefs[Keys.TOKENS_JSON] ?: "{}"
            val jsonObj = JSONObject(tokensJson)
            jsonObj.put(ipAddress, token)
            prefs[Keys.TOKENS_JSON] = jsonObj.toString()
        }
    }

    suspend fun getToken(ipAddress: String): String? {
        if (ipAddress.isBlank()) return null
        val prefs = context.dataStore.data.first()
        val tokensJson = prefs[Keys.TOKENS_JSON] ?: "{}"
        val jsonObj = JSONObject(tokensJson)
        return if (jsonObj.has(ipAddress)) jsonObj.optString(ipAddress) else null
    }

    suspend fun saveMacAddress(ipAddress: String, macAddress: String) {
        if (ipAddress.isBlank() || macAddress.isBlank()) return
        context.dataStore.edit { prefs ->
            val macsJson = prefs[Keys.MACS_JSON] ?: "{}"
            val jsonObj = JSONObject(macsJson)
            jsonObj.put(ipAddress, macAddress)
            prefs[Keys.MACS_JSON] = jsonObj.toString()
            
            // Also update current selected TV mac if IP matches
            if (prefs[Keys.SELECTED_TV_IP] == ipAddress) {
                prefs[Keys.SELECTED_TV_MAC] = macAddress
            }
        }
    }

    suspend fun clearTokens() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.TOKENS_JSON)
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode
        }
    }

    suspend fun setDefaultConnectionMode(mode: ConnectionMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_CONNECTION_MODE] = mode.name
        }
    }

    suspend fun setSelectedTvBrand(brand: TvBrand) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_TV_BRAND] = brand.name
        }
    }

    suspend fun setWifiEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WIFI_ENABLED] = enabled
        }
    }

    suspend fun setBluetoothEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BLUETOOTH_ENABLED] = enabled
        }
    }

    suspend fun setIrEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IR_ENABLED] = enabled
        }
    }

    suspend fun setAutoConnect(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_CONNECT] = enabled
        }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAPTIC_FEEDBACK] = enabled
        }
    }

    suspend fun setButtonSound(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BUTTON_SOUND] = enabled
        }
    }

    suspend fun setRepeatDelayMs(delayMs: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REPEAT_DELAY_MS] = delayMs
        }
    }

    suspend fun setSavedBluetoothAddress(address: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SAVED_BLUETOOTH_ADDRESS] = address
        }
    }

    suspend fun clearAllData() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    private fun parseSavedDevices(jsonStr: String): List<TvDevice> {
        val list = mutableListOf<TvDevice>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    TvDevice(
                        id = obj.optString("id", obj.optString("ipAddress")),
                        name = obj.optString("name", "Samsung Smart TV"),
                        ipAddress = obj.optString("ipAddress", ""),
                        macAddress = obj.optString("macAddress", ""),
                        model = obj.optString("model", "Samsung Smart TV"),
                        port = obj.optInt("port", 8002),
                        token = obj.optString("token", null),
                        connectionType = when (obj.optString("connectionType")) {
                            "BLUETOOTH" -> ConnectionMode.BLUETOOTH
                            "IR" -> ConnectionMode.IR
                            else -> ConnectionMode.WIFI
                        },
                        isSaved = true,
                        lastConnected = obj.optLong("lastConnected", 0L),
                        bluetoothAddress = obj.optString("bluetoothAddress", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun serializeSavedDevices(devices: List<TvDevice>): String {
        val array = JSONArray()
        for (device in devices) {
            val obj = JSONObject()
            obj.put("id", device.id)
            obj.put("name", device.name)
            obj.put("ipAddress", device.ipAddress)
            obj.put("macAddress", device.macAddress)
            obj.put("model", device.model)
            obj.put("port", device.port)
            obj.put("token", device.token ?: "")
            obj.put("connectionType", device.connectionType.name)
            obj.put("isSaved", device.isSaved)
            obj.put("lastConnected", device.lastConnected)
            obj.put("bluetoothAddress", device.bluetoothAddress)
            array.put(obj)
        }
        return array.toString()
    }
}
