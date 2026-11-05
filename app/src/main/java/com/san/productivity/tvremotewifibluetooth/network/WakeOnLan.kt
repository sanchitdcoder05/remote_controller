package com.san.productivity.tvremotewifibluetooth.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object WakeOnLan {

    private const val TAG = "WakeOnLan"

    suspend fun sendMagicPacket(macAddress: String, targetIp: String = "255.255.255.255"): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanMac = macAddress.replace(":", "").replace("-", "").trim()
        if (cleanMac.length != 12) {
            return@withContext Result.failure(IllegalArgumentException("Invalid MAC address length: $macAddress"))
        }

        return@withContext try {
            val macBytes = ByteArray(6)
            for (i in 0..5) {
                macBytes[i] = cleanMac.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }

            val packetBytes = ByteArray(6 + 16 * 6)
            for (i in 0..5) {
                packetBytes[i] = 0xFF.toByte()
            }
            for (i in 0 until 16) {
                System.arraycopy(macBytes, 0, packetBytes, 6 + i * 6, 6)
            }

            val address = InetAddress.getByName(targetIp.ifBlank { "255.255.255.255" })

            // Send on port 9 and port 7
            for (port in listOf(9, 7)) {
                val socket = DatagramSocket()
                socket.broadcast = true
                val packet = DatagramPacket(packetBytes, packetBytes.size, address, port)
                socket.send(packet)
                socket.close()
                Log.d(TAG, "Sent Wake-on-LAN magic packet to $targetIp ($cleanMac) on port $port")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending WoL packet: ${e.message}", e)
            Result.failure(e)
        }
    }
}
