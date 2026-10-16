package org.opensense.core.protocol

import org.opensense.core.model.VehicleSnapshot

sealed interface DecodeResult {
    data class Sample(val value: VehicleSnapshot) : DecodeResult
    data class Rejected(val reason: String) : DecodeResult
}

/** OpenSense-only test format. This is NOT the Mahindra vehicle protocol. */
object SimulationCodec {
    fun encode(value: VehicleSnapshot): ByteArray =
        "SIM1|${value.sequence}|${value.fuelPercent}|${value.rangeKm}|${value.cabinCelsius}|${value.outsideCelsius}|${if (value.doorsClosed) 1 else 0}|${if (value.mediaPlaying) 1 else 0}\n".toByteArray(Charsets.US_ASCII)

    fun decode(line: String): DecodeResult {
        val fields = line.split('|')
        if (fields.size != 8 || fields[0] != "SIM1") return DecodeResult.Rejected("simulation_format")
        val numbers = fields.drop(1).map { it.toLongOrNull() ?: return DecodeResult.Rejected("invalid_number") }
        if (numbers[0] < 0 || numbers[1] !in 0..100 || numbers[2] !in 0..2000 ||
            numbers[3] !in -40..85 || numbers[4] !in -40..85 || numbers[5] !in 0..1 || numbers[6] !in 0..1)
            return DecodeResult.Rejected("out_of_range")
        return DecodeResult.Sample(VehicleSnapshot(numbers[0], numbers[1].toInt(), numbers[2].toInt(),
            numbers[3].toInt(), numbers[4].toInt(), numbers[5] == 1L, numbers[6] == 1L))
    }
}

/** Bounded incremental decoder; drops an oversized frame until its next delimiter. */
class SimulationStreamDecoder(private val maxFrameBytes: Int = 256) {
    init { require(maxFrameBytes > 0) }
    private val pending = StringBuilder()
    private var dropping = false
    fun reset() { pending.clear(); dropping = false }
    fun feed(bytes: ByteArray): List<DecodeResult> = buildList {
        for (byte in bytes) {
            val value = byte.toInt() and 0xff
            if (value == 10) {
                if (!dropping) add(SimulationCodec.decode(pending.toString()))
                pending.clear(); dropping = false
            } else if (!dropping) {
                if (value !in 32..126) {
                    add(DecodeResult.Rejected("non_ascii")); pending.clear(); dropping = true
                } else if (pending.length >= maxFrameBytes) {
                    add(DecodeResult.Rejected("frame_too_large")); pending.clear(); dropping = true
                } else pending.append(value.toChar())
            }
        }
    }
}

/** Deliberately no guessed UUID, commands, framing, or telemetry interpretation. */
object VehicleProtocol {
    const val verified = false
    fun decode(@Suppress("UNUSED_PARAMETER") bytes: ByteArray): DecodeResult =
        DecodeResult.Rejected("vehicle_protocol_unverified")
}
