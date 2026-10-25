package com.example.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

sealed class NearbyPacket {
    abstract val type: Byte

    data class FloorRequest(
        val senderId: String,
        val senderName: String,
        val channel: String
    ) : NearbyPacket() {
        override val type: Byte = TYPE_FLOOR_REQUEST
    }

    data class FloorRelease(
        val senderId: String,
        val channel: String
    ) : NearbyPacket() {
        override val type: Byte = TYPE_FLOOR_RELEASE
    }

    data class AudioData(
        val senderId: String,
        val sequenceNumber: Int,
        val pcmData: ByteArray
    ) : NearbyPacket() {
        override val type: Byte = TYPE_AUDIO

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as AudioData
            return senderId == other.senderId &&
                    sequenceNumber == other.sequenceNumber &&
                    pcmData.contentEquals(other.pcmData)
        }

        override fun hashCode(): Int {
            var result = senderId.hashCode()
            result = 31 * result + sequenceNumber
            result = 31 * result + pcmData.contentHashCode()
            return result
        }
    }

    data class FloorBusy(
        val currentSpeakerId: String,
        val currentSpeakerName: String
    ) : NearbyPacket() {
        override val type: Byte = TYPE_FLOOR_BUSY
    }

    data class Ping(
        val senderId: String,
        val senderName: String,
        val channel: String
    ) : NearbyPacket() {
        override val type: Byte = TYPE_PING
    }

    companion object {
        const val TYPE_FLOOR_REQUEST: Byte = 0x01
        const val TYPE_FLOOR_RELEASE: Byte = 0x02
        const val TYPE_AUDIO: Byte = 0x03
        const val TYPE_FLOOR_BUSY: Byte = 0x04
        const val TYPE_PING: Byte = 0x05

        fun serialize(packet: NearbyPacket): ByteArray {
            return when (packet) {
                is FloorRequest -> {
                    val idBytes = packet.senderId.toByteArray(Charsets.UTF_8)
                    val nameBytes = packet.senderName.toByteArray(Charsets.UTF_8)
                    val chBytes = packet.channel.toByteArray(Charsets.UTF_8)

                    val buffer = ByteBuffer.allocate(1 + 1 + idBytes.size + 1 + nameBytes.size + 1 + chBytes.size)
                    buffer.put(TYPE_FLOOR_REQUEST)
                    buffer.put(idBytes.size.toByte())
                    buffer.put(idBytes)
                    buffer.put(nameBytes.size.toByte())
                    buffer.put(nameBytes)
                    buffer.put(chBytes.size.toByte())
                    buffer.put(chBytes)
                    buffer.array()
                }

                is FloorRelease -> {
                    val idBytes = packet.senderId.toByteArray(Charsets.UTF_8)
                    val chBytes = packet.channel.toByteArray(Charsets.UTF_8)
                    val buffer = ByteBuffer.allocate(1 + 1 + idBytes.size + 1 + chBytes.size)
                    buffer.put(TYPE_FLOOR_RELEASE)
                    buffer.put(idBytes.size.toByte())
                    buffer.put(idBytes)
                    buffer.put(chBytes.size.toByte())
                    buffer.put(chBytes)
                    buffer.array()
                }

                is AudioData -> {
                    val idBytes = packet.senderId.toByteArray(Charsets.UTF_8)
                    val buffer = ByteBuffer.allocate(1 + 1 + idBytes.size + 4 + packet.pcmData.size)
                        .order(ByteOrder.BIG_ENDIAN)
                    buffer.put(TYPE_AUDIO)
                    buffer.put(idBytes.size.toByte())
                    buffer.put(idBytes)
                    buffer.putInt(packet.sequenceNumber)
                    buffer.put(packet.pcmData)
                    buffer.array()
                }

                is FloorBusy -> {
                    val idBytes = packet.currentSpeakerId.toByteArray(Charsets.UTF_8)
                    val nameBytes = packet.currentSpeakerName.toByteArray(Charsets.UTF_8)
                    val buffer = ByteBuffer.allocate(1 + 1 + idBytes.size + 1 + nameBytes.size)
                    buffer.put(TYPE_FLOOR_BUSY)
                    buffer.put(idBytes.size.toByte())
                    buffer.put(idBytes)
                    buffer.put(nameBytes.size.toByte())
                    buffer.put(nameBytes)
                    buffer.array()
                }

                is Ping -> {
                    val idBytes = packet.senderId.toByteArray(Charsets.UTF_8)
                    val nameBytes = packet.senderName.toByteArray(Charsets.UTF_8)
                    val chBytes = packet.channel.toByteArray(Charsets.UTF_8)
                    val buffer = ByteBuffer.allocate(1 + 1 + idBytes.size + 1 + nameBytes.size + 1 + chBytes.size)
                    buffer.put(TYPE_PING)
                    buffer.put(idBytes.size.toByte())
                    buffer.put(idBytes)
                    buffer.put(nameBytes.size.toByte())
                    buffer.put(nameBytes)
                    buffer.put(chBytes.size.toByte())
                    buffer.put(chBytes)
                    buffer.array()
                }
            }
        }

        fun deserialize(bytes: ByteArray): NearbyPacket? {
            if (bytes.isEmpty()) return null
            return try {
                val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
                val type = buffer.get()
                when (type) {
                    TYPE_FLOOR_REQUEST -> {
                        val idLen = buffer.get().toInt() and 0xFF
                        val idBytes = ByteArray(idLen)
                        buffer.get(idBytes)
                        val nameLen = buffer.get().toInt() and 0xFF
                        val nameBytes = ByteArray(nameLen)
                        buffer.get(nameBytes)
                        val chLen = buffer.get().toInt() and 0xFF
                        val chBytes = ByteArray(chLen)
                        buffer.get(chBytes)
                        FloorRequest(
                            senderId = String(idBytes, Charsets.UTF_8),
                            senderName = String(nameBytes, Charsets.UTF_8),
                            channel = String(chBytes, Charsets.UTF_8)
                        )
                    }

                    TYPE_FLOOR_RELEASE -> {
                        val idLen = buffer.get().toInt() and 0xFF
                        val idBytes = ByteArray(idLen)
                        buffer.get(idBytes)
                        val chLen = buffer.get().toInt() and 0xFF
                        val chBytes = ByteArray(chLen)
                        buffer.get(chBytes)
                        FloorRelease(
                            senderId = String(idBytes, Charsets.UTF_8),
                            channel = String(chBytes, Charsets.UTF_8)
                        )
                    }

                    TYPE_AUDIO -> {
                        val idLen = buffer.get().toInt() and 0xFF
                        val idBytes = ByteArray(idLen)
                        buffer.get(idBytes)
                        val seqNum = buffer.getInt()
                        val pcmData = ByteArray(buffer.remaining())
                        buffer.get(pcmData)
                        AudioData(
                            senderId = String(idBytes, Charsets.UTF_8),
                            sequenceNumber = seqNum,
                            pcmData = pcmData
                        )
                    }

                    TYPE_FLOOR_BUSY -> {
                        val idLen = buffer.get().toInt() and 0xFF
                        val idBytes = ByteArray(idLen)
                        buffer.get(idBytes)
                        val nameLen = buffer.get().toInt() and 0xFF
                        val nameBytes = ByteArray(nameLen)
                        buffer.get(nameBytes)
                        FloorBusy(
                            currentSpeakerId = String(idBytes, Charsets.UTF_8),
                            currentSpeakerName = String(nameBytes, Charsets.UTF_8)
                        )
                    }

                    TYPE_PING -> {
                        val idLen = buffer.get().toInt() and 0xFF
                        val idBytes = ByteArray(idLen)
                        buffer.get(idBytes)
                        val nameLen = buffer.get().toInt() and 0xFF
                        val nameBytes = ByteArray(nameLen)
                        buffer.get(nameBytes)
                        val chLen = buffer.get().toInt() and 0xFF
                        val chBytes = ByteArray(chLen)
                        buffer.get(chBytes)
                        Ping(
                            senderId = String(idBytes, Charsets.UTF_8),
                            senderName = String(nameBytes, Charsets.UTF_8),
                            channel = String(chBytes, Charsets.UTF_8)
                        )
                    }

                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
