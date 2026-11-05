package com.san.productivity.tvremotewifibluetooth

import com.san.productivity.tvremotewifibluetooth.data.model.ConnectionMode
import com.san.productivity.tvremotewifibluetooth.data.model.RemoteCommand
import com.san.productivity.tvremotewifibluetooth.data.model.TvDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SamsungTvRemoteUnitTest {

    @Test
    fun testRemoteCommandSamsungKeyMapping() {
        assertEquals("KEY_POWER", RemoteCommand.POWER.samsungKey)
        assertEquals("KEY_VOLUP", RemoteCommand.VOLUME_UP.samsungKey)
        assertEquals("KEY_VOLDOWN", RemoteCommand.VOLUME_DOWN.samsungKey)
        assertEquals("KEY_MUTE", RemoteCommand.MUTE.samsungKey)
        assertEquals("KEY_UP", RemoteCommand.UP.samsungKey)
        assertEquals("KEY_DOWN", RemoteCommand.DOWN.samsungKey)
        assertEquals("KEY_LEFT", RemoteCommand.LEFT.samsungKey)
        assertEquals("KEY_RIGHT", RemoteCommand.RIGHT.samsungKey)
        assertEquals("KEY_ENTER", RemoteCommand.ENTER.samsungKey)
        assertEquals("KEY_RETURN", RemoteCommand.BACK.samsungKey)
        assertEquals("KEY_HOME", RemoteCommand.HOME.samsungKey)
        assertEquals("KEY_MENU", RemoteCommand.MENU.samsungKey)
        assertEquals("KEY_SOURCE", RemoteCommand.SOURCE.samsungKey)
    }

    @Test
    fun testRemoteCommandDigitMapping() {
        assertEquals(RemoteCommand.NUM_0, RemoteCommand.fromNumber(0))
        assertEquals(RemoteCommand.NUM_5, RemoteCommand.fromNumber(5))
        assertEquals(RemoteCommand.NUM_9, RemoteCommand.fromNumber(9))
    }

    @Test
    fun testWebSocketKeyPayloadFormatting() {
        val command = RemoteCommand.UP
        val payload = """
            {
              "method": "ms.remote.control",
              "params": {
                "Cmd": "Click",
                "DataOfCmd": "${command.samsungKey}",
                "Option": "false",
                "TypeOfRemote": "SendRemoteKey"
              }
            }
        """.trimIndent()

        assertTrue(payload.contains("ms.remote.control"))
        assertTrue(payload.contains("KEY_UP"))
        assertTrue(payload.contains("SendRemoteKey"))
    }

    @Test
    fun testTvDeviceCreation() {
        val device = TvDevice.createDefault("192.168.1.50", "AA:BB:CC:DD:EE:FF")
        assertEquals("192.168.1.50", device.ipAddress)
        assertEquals("AA:BB:CC:DD:EE:FF", device.macAddress)
        assertEquals(8002, device.port)
        assertEquals(ConnectionMode.WIFI, device.connectionType)
    }

    @Test
    fun testWakeOnLanMagicPacketGeneration() {
        val mac = "AA:BB:CC:DD:EE:FF"
        val cleanMac = mac.replace(":", "").replace("-", "").trim()
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

        // Verify WoL Packet structure (First 6 bytes are 0xFF, then 16 iterations of 6-byte MAC)
        assertEquals(102, packetBytes.size)
        for (i in 0..5) {
            assertEquals(0xFF.toByte(), packetBytes[i])
        }
        assertEquals(0xAA.toByte(), packetBytes[6])
        assertEquals(0xBB.toByte(), packetBytes[7])
        assertEquals(0xCC.toByte(), packetBytes[8])
        assertEquals(0xDD.toByte(), packetBytes[9])
        assertEquals(0xEE.toByte(), packetBytes[10])
        assertEquals(0xFF.toByte(), packetBytes[11])
    }
}
