package org.opensense.core.protocol

/** Raw legacy-profile packet, accepted offline after framing and checksum checks only. */
data class LegacyFrame(
    val version: Int,
    val applicationId: Int,
    val messageNumber: Int,
    val messageType: Int,
    val featureId: Int,
    val functionId: Int,
    val payload: ByteArray,
)

sealed interface LegacyDecodeResult {
    data class Packet(val frame: LegacyFrame) : LegacyDecodeResult
    data class Rejected(val reason: String) : LegacyDecodeResult
}

/** CRC-16/XMODEM parameters recovered statically: poly 0x1021, init 0, no reflection/xor. */
object LegacyCrc16 {
    fun compute(bytes: ByteArray, endExclusive: Int = bytes.size): Int {
        require(endExclusive in 0..bytes.size)
        var remainder = 0
        for (index in 0 until endExclusive) {
            remainder = remainder xor ((bytes[index].toInt() and 255) shl 8)
            repeat(8) {
                remainder = ((remainder shl 1) xor
                    if (remainder and 0x8000 != 0) 0x1021 else 0) and 0xffff
            }
        }
        return remainder
    }
}

/**
 * Read-only offline framing for the com.visteon 0.46 profile. No transport or vehicle commands.
 * Incremental processing retains at most 266 bytes, including a possible partial packet.
 * Header and payload values remain raw; acceptance establishes no hardware compatibility.
 */
class LegacyFrameDecoder {
    private val pending = ArrayList<Byte>(MAX_FRAME_BYTES)
    val bufferedBytes: Int get() = pending.size

    fun reset() = pending.clear()

    fun feed(bytes: ByteArray): List<LegacyDecodeResult> = buildList {
        var discardedGarbage = false
        for (byte in bytes) {
            if (pending.isEmpty() && unsigned(byte) != START) {
                if (!discardedGarbage) add(LegacyDecodeResult.Rejected("missing_start"))
                discardedGarbage = true
                continue
            }
            discardedGarbage = false
            pending.add(byte)
            while (pending.size >= HEADER_BYTES) {
                if (unsigned(pending[0]) != START) {
                    pending.removeAt(0)
                    continue
                }
                val payloadSize = unsigned(pending[7])
                val frameSize = HEADER_BYTES + payloadSize + TRAILER_BYTES
                if (pending.size < frameSize) break
                val packet = ByteArray(frameSize) { pending[it] }
                val checksumOffset = HEADER_BYTES + payloadSize
                val suppliedChecksum = (unsigned(packet[checksumOffset]) shl 8) or
                    unsigned(packet[checksumOffset + 1])
                val error = when {
                    unsigned(packet.last()) != END -> "invalid_end"
                    LegacyCrc16.compute(packet, checksumOffset) != suppliedChecksum -> "invalid_checksum"
                    else -> null
                }
                if (error != null) {
                    add(LegacyDecodeResult.Rejected(error))
                    pending.removeAt(0)
                    while (pending.isNotEmpty() && unsigned(pending[0]) != START) pending.removeAt(0)
                } else {
                    add(LegacyDecodeResult.Packet(LegacyFrame(
                        unsigned(packet[1]), unsigned(packet[2]), unsigned(packet[3]),
                        unsigned(packet[4]), unsigned(packet[5]), unsigned(packet[6]),
                        packet.copyOfRange(HEADER_BYTES, checksumOffset),
                    )))
                    repeat(frameSize) { pending.removeAt(0) }
                }
            }
        }
    }

    private fun unsigned(value: Byte) = value.toInt() and 255

    companion object {
        const val MAX_PAYLOAD_BYTES = 255
        const val MAX_FRAME_BYTES = 266
        private const val HEADER_BYTES = 8
        private const val TRAILER_BYTES = 3
        private const val START = 0xed
        private const val END = 0xee
    }
}
