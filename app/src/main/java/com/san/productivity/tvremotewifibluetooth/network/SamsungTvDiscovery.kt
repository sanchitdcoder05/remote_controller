package com.san.productivity.tvremotewifibluetooth.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.san.productivity.tvremotewifibluetooth.data.model.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class SamsungTvDiscovery(private val context: Context) {

    private val TAG = "SamsungTvDiscovery"

    private val _discoveredDevices = MutableStateFlow<List<TvDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<TvDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(1200, TimeUnit.MILLISECONDS)
        .readTimeout(1200, TimeUnit.MILLISECONDS)
        .build()

    suspend fun startDiscovery() = withContext(Dispatchers.IO) {
        if (_isScanning.value) return@withContext
        _isScanning.value = true
        _discoveredDevices.value = emptyList()

        val foundMap = ConcurrentHashMap<String, TvDevice>()

        coroutineScope {
            // Task 1: SSDP Multicast Search
            val ssdpJob = async {
                performSsdpDiscovery(foundMap)
            }

            // Task 2: Active Subnet HTTP Probe
            val subnetJob = async {
                performSubnetHttpProbe(foundMap)
            }

            ssdpJob.await()
            subnetJob.await()
        }

        _discoveredDevices.value = foundMap.values.toList().sortedBy { it.displayName }
        _isScanning.value = false
    }

    private suspend fun performSsdpDiscovery(foundMap: ConcurrentHashMap<String, TvDevice>) = withContext(Dispatchers.IO) {
        var multicastLock: WifiManager.MulticastLock? = null
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifiManager?.createMulticastLock("SamsungTvSsdpLock")
            multicastLock?.setReferenceCounted(true)
            multicastLock?.acquire()

            val ssdpQuery = "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 3\r\n" +
                    "ST: urn:samsung.com:device:RemoteControlReceiver:1\r\n\r\n"

            val socket = DatagramSocket()
            socket.soTimeout = 2500

            val queryBytes = ssdpQuery.toByteArray(Charsets.UTF_8)
            val group = InetAddress.getByName("239.255.255.250")
            val packet = DatagramPacket(queryBytes, queryBytes.size, group, 1900)
            socket.send(packet)

            val buffer = ByteArray(2048)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < 2500) {
                try {
                    val recvPacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(recvPacket)
                    val ip = recvPacket.address.hostAddress ?: continue
                    val response = String(recvPacket.data, 0, recvPacket.length, Charsets.UTF_8)
                    Log.d(TAG, "SSDP response from $ip:\n$response")

                    if (!foundMap.containsKey(ip)) {
                        probeTvInfo(ip)?.let { device ->
                            foundMap[ip] = device
                            _discoveredDevices.value = foundMap.values.toList()
                        }
                    }
                } catch (e: Exception) {
                    break // Timeout or socket closed
                }
            }
            socket.close()
        } catch (e: Exception) {
            Log.w(TAG, "SSDP Discovery warning: ${e.message}")
        } finally {
            try {
                if (multicastLock?.isHeld == true) {
                    multicastLock.release()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed releasing multicast lock: ${e.message}")
            }
        }
    }

    private suspend fun performSubnetHttpProbe(foundMap: ConcurrentHashMap<String, TvDevice>) = withContext(Dispatchers.IO) {
        val localIp = getLocalWifiIpAddress()
        if (localIp.isBlank()) return@withContext

        val prefix = localIp.substringBeforeLast(".") + "."
        val hostLastOctet = localIp.substringAfterLast(".").toIntOrNull() ?: 0

        val ipsToScan = (1..254).filter { it != hostLastOctet }.map { "$prefix$it" }

        // Scan in chunks of 25 to avoid socket exhaustion
        ipsToScan.chunked(25).forEach { chunk ->
            coroutineScope {
                chunk.map { ip ->
                    async {
                        if (!foundMap.containsKey(ip)) {
                            probeTvInfo(ip)?.let { device ->
                                foundMap[ip] = device
                                _discoveredDevices.value = foundMap.values.toList()
                            }
                        }
                    }
                }.awaitAll()
            }
        }
    }

    suspend fun probeTvInfo(ip: String): TvDevice? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("http://$ip:8001/api/v2/")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)

                val name = json.optString("name", "Samsung TV")
                val deviceObj = json.optJSONObject("device")
                val tvName = deviceObj?.optString("name") ?: name
                val modelName = deviceObj?.optString("modelName") ?: json.optString("model", "Samsung Smart TV")
                val wifiMac = deviceObj?.optString("wifiMac") ?: deviceObj?.optString("mac") ?: ""

                val device = TvDevice(
                    id = ip,
                    name = if (tvName.isNotBlank()) tvName else "Samsung Smart TV ($ip)",
                    ipAddress = ip,
                    macAddress = wifiMac,
                    model = modelName,
                    port = 8002
                )
                Log.d(TAG, "Found Samsung TV via HTTP probe: $device")
                return@withContext device
            }
        } catch (e: Exception) {
            // IP is not a Samsung TV or unreachable
        }
        return@withContext null
    }

    private fun getLocalWifiIpAddress(): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            @Suppress("DEPRECATION")
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                return String.format(
                    Locale.US,
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
            }

            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is InetAddress) {
                        val host = addr.hostAddress ?: ""
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Wifi IP: ${e.message}")
        }
        return ""
    }
}
