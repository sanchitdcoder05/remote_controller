package com.san.productivity.tvremotewifibluetooth.network

import android.util.Base64
import android.util.Log
import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionMode
import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionState
import com.san.productivity.tvremotewifibluetooth.data.model.RemoteCommand
import com.san.productivity.tvremotewifibluetooth.data.model.TvDevice
import com.san.productivity.tvremotewifibluetooth.domain.transport.RemoteTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class SamsungTvWebSocketClient(
    private val onTokenReceived: (ip: String, token: String) -> Unit = { _, _ -> }
) : RemoteTransport, WebSocketListener() {

    private val TAG = "SamsungTvWSClient"
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _currentDevice = MutableStateFlow<TvDevice?>(null)
    override val currentDevice: StateFlow<TvDevice?> = _currentDevice.asStateFlow()

    private var webSocket: WebSocket? = null
    private var okHttpClient: OkHttpClient? = null

    private var reconnectJob: Job? = null
    private var isManualDisconnect = false

    private fun createUnsafeOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier(HostnameVerifier { _, _ -> true })
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .pingInterval(10, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun connect(device: TvDevice): Result<Unit> {
        if (device.ipAddress.isBlank()) {
            _connectionState.value = ConnectionState.Error("IP Address cannot be empty")
            return Result.failure(IllegalArgumentException("IP Address is blank"))
        }

        disconnectInternal(resetState = false)
        isManualDisconnect = false
        _currentDevice.value = device
        _connectionState.value = ConnectionState.Connecting("Connecting to ${device.displayName}...")

        return try {
            val appNameBase64 = Base64.encodeToString(
                "AndroidRemote".toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )

            val port = if (device.port == 8001 || device.port == 8002) device.port else 8002
            val scheme = if (port == 8002) "wss" else "ws"

            val urlBuilder = StringBuilder("$scheme://${device.ipAddress}:$port/api/v2/channels/samsung.remote.control?name=$appNameBase64")
            
            if (!device.token.isNullOrBlankToken()) {
                urlBuilder.append("&token=").append(device.token)
            }

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .addHeader("Origin", "http://${device.ipAddress}")
                .addHeader("User-Agent", "Mozilla/5.0 (Android)")
                .build()

            okHttpClient = createUnsafeOkHttpClient()
            webSocket = okHttpClient?.newWebSocket(request, this)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed: ${e.message}", e)
            _connectionState.value = ConnectionState.Error("Failed to initiate connection: ${e.message}")
            Result.failure(e)
        }
    }

    private fun String?.isNullOrBlankToken(): Boolean {
        return this.isNullOrBlank() || this == "null"
    }

    override suspend fun disconnect() {
        isManualDisconnect = true
        reconnectJob?.cancel()
        disconnectInternal(resetState = true)
    }

    private fun disconnectInternal(resetState: Boolean) {
        try {
            webSocket?.close(1000, "App disconnect")
            webSocket = null
        } catch (e: Exception) {
            Log.w(TAG, "Error closing websocket: ${e.message}")
        }
        if (resetState) {
            _connectionState.value = ConnectionState.Disconnected
            _currentDevice.value = null
        }
    }

    override suspend fun sendCommand(command: RemoteCommand): Result<Unit> {
        val currentWs = webSocket
        if (currentWs == null) {
            return Result.failure(IllegalStateException("Not connected to TV"))
        }

        return try {
            if (command == RemoteCommand.VOICE) {
                // Dual dispatch: Send primary KEY_VOICE click, then fallback KEY_SEARCH / KEY_MIC for Tizen 2020-2024+
                currentWs.send(buildKeyPayload("KEY_VOICE"))
                scope.launch {
                    delay(150)
                    currentWs.send(buildKeyPayload("KEY_SEARCH"))
                    delay(150)
                    currentWs.send(buildKeyPayload("KEY_MIC"))
                }
                Result.success(Unit)
            } else {
                val payload = buildKeyPayload(command.samsungKey)
                val sent = currentWs.send(payload)
                if (sent) {
                    if (_connectionState.value !is ConnectionState.Connected) {
                        val device = _currentDevice.value
                        if (device != null) {
                            _connectionState.value = ConnectionState.Connected(
                                deviceName = device.displayName,
                                mode = ConnectionMode.WIFI
                            )
                        }
                    }
                    Result.success(Unit)
                } else {
                    Result.failure(RuntimeException("Failed to queue websocket message"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending command ${command.name}: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun sendText(text: String): Result<Unit> {
        val currentWs = webSocket
        if (currentWs == null) {
            return Result.failure(IllegalStateException("Not connected to TV"))
        }

        return try {
            val encodedText = Base64.encodeToString(text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val json = JSONObject().apply {
                put("method", "ms.remote.control")
                put("params", JSONObject().apply {
                    put("Cmd", encodedText)
                    put("DataOfCmd", "base64")
                    put("Option", false)
                    put("TypeOfRemote", "SendInputString")
                })
            }
            val sent = currentWs.send(json.toString())
            if (sent) Result.success(Unit) else Result.failure(RuntimeException("Failed to queue text input"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun launchApp(appId: String): Result<Unit> {
        val currentWs = webSocket
        if (currentWs == null) {
            return Result.failure(IllegalStateException("Not connected to TV"))
        }

        return try {
            val json = JSONObject().apply {
                put("method", "ms.channel.emit")
                put("params", JSONObject().apply {
                    put("event", "ed.apps.launch")
                    put("to", "host")
                    put("data", JSONObject().apply {
                        put("action_type", "DEEP_LINK")
                        put("appId", appId)
                    })
                })
            }
            val sent = currentWs.send(json.toString())
            if (sent) Result.success(Unit) else Result.failure(RuntimeException("Failed to launch app"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun startVoiceHold(): Result<Unit> {
        val currentWs = webSocket
        if (currentWs == null) {
            return Result.failure(IllegalStateException("Not connected to TV"))
        }
        return try {
            val payload = buildKeyPayload("KEY_VOICE", cmd = "Press")
            val sent = currentWs.send(payload)
            if (sent) Result.success(Unit) else Result.failure(RuntimeException("Failed to send Press payload"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun stopVoiceHold(): Result<Unit> {
        val currentWs = webSocket
        if (currentWs == null) {
            return Result.failure(IllegalStateException("Not connected to TV"))
        }
        return try {
            val payload = buildKeyPayload("KEY_VOICE", cmd = "Release")
            val sent = currentWs.send(payload)
            if (sent) Result.success(Unit) else Result.failure(RuntimeException("Failed to send Release payload"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildKeyPayload(samsungKey: String, cmd: String = "Click"): String {
        return JSONObject().apply {
            put("method", "ms.remote.control")
            put("params", JSONObject().apply {
                put("Cmd", cmd)
                put("DataOfCmd", samsungKey)
                put("Option", false)
                put("TypeOfRemote", "SendRemoteKey")
            })
        }.toString()
    }

    // WebSocketListener overrides

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d(TAG, "WebSocket connection opened: ${response.message}")
        if (webSocket != this.webSocket) {
            Log.d(TAG, "Ignoring onOpen from stale WebSocket")
            return
        }
        val device = _currentDevice.value
        if (device != null) {
            if (!device.token.isNullOrBlankToken()) {
                _connectionState.value = ConnectionState.Connected(
                    deviceName = device.displayName,
                    mode = ConnectionMode.WIFI
                )
            } else {
                _connectionState.value = ConnectionState.PairingRequested("Check TV screen and click Allow if prompted")
            }
        }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d(TAG, "Received WS Message: $text")
        if (webSocket != this.webSocket) {
            Log.d(TAG, "Ignoring onMessage from stale WebSocket")
            return
        }
        try {
            val json = JSONObject(text)
            val event = json.optString("event")
            if (event == "ms.channel.connect" || event == "ms.channel.ready" || json.has("data")) {
                val data = json.optJSONObject("data")
                val token = data?.optString("token")
                val device = _currentDevice.value

                if (!token.isNullOrEmpty() && token != "null") {
                    Log.d(TAG, "Received TV Auth Token: $token")
                    if (device != null) {
                        onTokenReceived(device.ipAddress, token)
                        _currentDevice.value = device.copy(token = token)
                    }
                }

                if (device != null) {
                    _connectionState.value = ConnectionState.Connected(
                        deviceName = device.displayName,
                        mode = ConnectionMode.WIFI
                    )
                }
            } else if (event == "ms.channel.unauthorized") {
                _connectionState.value = ConnectionState.Error(
                    "Connection refused by TV. Please allow permission on your Samsung TV screen."
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing WS message: ${e.message}")
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "WebSocket closing: $code / $reason")
        if (webSocket != this.webSocket) return
        webSocket.close(1000, null)
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "WebSocket closed: $code / $reason")
        if (webSocket != this.webSocket) {
            Log.d(TAG, "Ignoring onClosed from stale WebSocket")
            return
        }
        this.webSocket = null
        if (!isManualDisconnect) {
            _connectionState.value = ConnectionState.Disconnected
            attemptReconnect()
        } else {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e(TAG, "WebSocket failure: ${t.message}", t)
        if (webSocket != this.webSocket) {
            Log.d(TAG, "Ignoring onFailure from stale WebSocket")
            return
        }
        this.webSocket = null
        if (isManualDisconnect) return

        val device = _currentDevice.value
        val port = device?.port ?: 8002

        // If port 8002 failed and we haven't tried port 8001, retry with 8001
        if (port == 8002 && device != null && (_connectionState.value is ConnectionState.Connecting || _connectionState.value is ConnectionState.PairingRequested)) {
            Log.d(TAG, "Port 8002 failed, trying fallback to port 8001 (ws)")
            scope.launch {
                delay(500)
                connect(device.copy(port = 8001))
            }
            return
        }

        _connectionState.value = ConnectionState.Error(
            "Connection failed: ${t.localizedMessage ?: "TV unreachable"}. Ensure TV is ON and on same Wi-Fi."
        )
    }

    private fun attemptReconnect() {
        val device = _currentDevice.value ?: return
        if (isManualDisconnect) return

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            for (attempt in 1..3) {
                if (isManualDisconnect) break
                _connectionState.value = ConnectionState.Reconnecting(attempt, "Reconnecting (Attempt $attempt/3)...")
                delay(3000L * attempt)
                val result = connect(device)
                if (result.isSuccess) {
                    break
                }
            }
        }
    }
}
