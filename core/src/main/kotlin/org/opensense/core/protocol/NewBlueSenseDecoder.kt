package org.opensense.core.protocol

/** Static APK-derived framing only. No vehicle telemetry or runtime compatibility is implied. */
data class NewBlueSenseFrame(val messageId: Int, val payload: List<Int>)
sealed interface NewDecodeResult {
    data class Frame(val value: NewBlueSenseFrame) : NewDecodeResult
    data class Rejected(val reason: String) : NewDecodeResult
}

/** Incoming direction only: each logical nibble occupies one wire byte in 0x50..0x5f.
 * See docs/protocol-new.md. No encoder or Bluetooth connection is supplied.
 */
object NewBlueSenseDecoder {
    const val MAX_WIRE_BYTES = 516
    fun decode(wire: ByteArray): NewDecodeResult {
        if (wire.size < 6 || wire.size > MAX_WIRE_BYTES || wire.size % 2 != 0)
            return NewDecodeResult.Rejected("invalid_size")
        if (wire.any { (it.toInt() and 0xff) !in 0x50..0x5f })
            return NewDecodeResult.Rejected("invalid_nibble")
        val logical = wire.asList().chunked(2).map { ((it[0].toInt() - 0x50) shl 4) or (it[1].toInt() - 0x50) }
        if (logical.size != logical[2] + 3) return NewDecodeResult.Rejected("length_mismatch")
        val sum = logical.indices.filter { it != 1 }.sumOf { logical[it] } and 0xff
        if (sum != logical[1]) return NewDecodeResult.Rejected("checksum_mismatch")
        return NewDecodeResult.Frame(NewBlueSenseFrame(logical[0], logical.drop(3)))
    }
}

/** Requires a known message boundary on construction/reset. This format has no confirmed sync
 * marker. Invalid data loses alignment: resume only after the caller establishes a new boundary.
 */
class NewBlueSenseStreamDecoder {
    private val pending = ArrayList<Byte>(NewBlueSenseDecoder.MAX_WIRE_BYTES)
    private var aligned = true
    fun resetAtKnownBoundary() { pending.clear(); aligned = true }
    fun feed(chunk: ByteArray): List<NewDecodeResult> = buildList {
        for (byte in chunk) {
            if (!aligned) continue
            if ((byte.toInt() and 0xff) !in 0x50..0x5f) {
                add(NewDecodeResult.Rejected("alignment_lost")); aligned = false; pending.clear(); continue
            }
            pending.add(byte)
            if (pending.size >= 6) {
                val size = 6 + 2 * (((pending[4].toInt() - 0x50) shl 4) or (pending[5].toInt() - 0x50))
                if (pending.size == size) {
                    val result = NewBlueSenseDecoder.decode(pending.toByteArray())
                    add(result); pending.clear()
                    if (result is NewDecodeResult.Rejected) aligned = false
                }
            }
        }
    }
}
