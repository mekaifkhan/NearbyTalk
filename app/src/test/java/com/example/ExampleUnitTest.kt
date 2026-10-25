package com.example

import com.example.protocol.NearbyPacket
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testFloorRequestPacketSerialization() {
        val original = NearbyPacket.FloorRequest(
            senderId = "DEV-101",
            senderName = "Alpha Operator",
            channel = "Tactical"
        )
        val serialized = NearbyPacket.serialize(original)
        val deserialized = NearbyPacket.deserialize(serialized)

        assertTrue(deserialized is NearbyPacket.FloorRequest)
        val result = deserialized as NearbyPacket.FloorRequest
        assertEquals(original.senderId, result.senderId)
        assertEquals(original.senderName, result.senderName)
        assertEquals(original.channel, result.channel)
    }

    @Test
    fun testAudioDataPacketSerialization() {
        val testPcm = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val original = NearbyPacket.AudioData(
            senderId = "DEV-999",
            sequenceNumber = 42,
            pcmData = testPcm
        )
        val serialized = NearbyPacket.serialize(original)
        val deserialized = NearbyPacket.deserialize(serialized)

        assertTrue(deserialized is NearbyPacket.AudioData)
        val result = deserialized as NearbyPacket.AudioData
        assertEquals(original.senderId, result.senderId)
        assertEquals(original.sequenceNumber, result.sequenceNumber)
        assertArrayEquals(original.pcmData, result.pcmData)
    }

    @Test
    fun testFloorReleasePacketSerialization() {
        val original = NearbyPacket.FloorRelease(
            senderId = "DEV-333",
            channel = "General"
        )
        val serialized = NearbyPacket.serialize(original)
        val deserialized = NearbyPacket.deserialize(serialized)

        assertTrue(deserialized is NearbyPacket.FloorRelease)
        val result = deserialized as NearbyPacket.FloorRelease
        assertEquals(original.senderId, result.senderId)
        assertEquals(original.channel, result.channel)
    }
}
