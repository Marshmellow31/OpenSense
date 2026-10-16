package org.opensense.core.protocol

import org.opensense.core.model.InfotainmentFamily

enum class MediaAction { PLAY, PAUSE, NEXT, PREVIOUS, RADIO, CD, USB, IPOD, BLUETOOTH, AUX }
enum class ClimateSwitch(val selector: Int) { AC(0), ECON(1), RECIRCULATE(4), AUTO(6), REAR(8) }

/** Independently implemented from the two APK command generators. No arbitrary packet API. */
object VehicleCommands {
    internal fun legacy(type: Int, feature: Int, function: Int, payload: List<Int>, number: Int = 0): ByteArray {
        require(payload.size <= 255 && payload.all { it in 0..255 })
        val bytes = (listOf(0xed, 1, 1, number, type, feature, function, payload.size) + payload + listOf(0, 0, 0xee))
            .map(Int::toByte).toByteArray()
        val crc = LegacyCrc16.compute(bytes, 8 + payload.size)
        bytes[8 + payload.size] = (crc shr 8).toByte(); bytes[9 + payload.size] = crc.toByte()
        return bytes
    }
    internal fun newer(id: Int, payload: List<Int>, checksumOverride: Int? = null): ByteArray {
        require(id in 0..255 && payload.size <= 255 && payload.all { it in 0..255 })
        val logical = listOf(id, checksumOverride ?: ((id + payload.size + payload.sum()) and 255), payload.size) + payload
        // Transmit raw nibbles. The +0x50 transformation belongs to receive ONLY.
        return logical.flatMap { listOf((it shr 4).toByte(), (it and 15).toByte()) }.toByteArray()
    }
    fun startup(family: InfotainmentFamily) = when (family) {
        InfotainmentFamily.LEGACY -> legacy(0x11, 5, 1, listOf(1))
        InfotainmentFamily.NEW -> configuration()
    }
    fun configuration() = newer(46, listOf(0, 1, 1, 1, 2, 1))
    fun pollReply() = newer(1, listOf(0))
    fun legacyAck(feature: Int, function: Int) = legacy(1, feature, function, listOf(1))
    fun legacyAlertQuery(function: Int): ByteArray {
        require(function in setOf(1, 2, 4, 5))
        return legacy(2, 1, function, listOf(1))
    }
    fun legacyRequest(feature: Int, function: Int) = legacy(0x20, feature, function, listOf(1), 1)
    fun media(family: InfotainmentFamily, action: MediaAction): ByteArray = when (family) {
        InfotainmentFamily.LEGACY -> legacy(0x11, 4, 1, listOf(when (action) {
            MediaAction.PLAY, MediaAction.PAUSE -> 1 // original has a single toggle
            MediaAction.NEXT -> 2; MediaAction.PREVIOUS -> 3; MediaAction.RADIO -> 4
            MediaAction.CD -> 5; MediaAction.USB -> 6; MediaAction.IPOD -> 7
            MediaAction.BLUETOOTH -> 8; MediaAction.AUX -> 9
        }))
        InfotainmentFamily.NEW -> when (action) {
            MediaAction.PLAY -> newer(17, listOf(0)); MediaAction.PAUSE -> newer(17, listOf(1))
            MediaAction.PREVIOUS -> newer(17, listOf(4)); MediaAction.NEXT -> newer(17, listOf(3))
            else -> newer(18, listOf(when (action) {
                MediaAction.IPOD -> 0; MediaAction.USB -> 1; MediaAction.AUX -> 5
                MediaAction.BLUETOOTH -> 3; MediaAction.RADIO -> 4; MediaAction.CD -> 2
                else -> error("unreachable")
            }))
        }
    }
    fun volume(family: InfotainmentFamily, value: Int): ByteArray {
        require(value in 0..if (family == InfotainmentFamily.LEGACY) 45 else 30)
        return if (family == InfotainmentFamily.LEGACY) legacy(0x11, 4, 2, listOf(value)) else newer(21, listOf(value))
    }
    fun radioMute(family: InfotainmentFamily, muted: Boolean): ByteArray =
        if (family == InfotainmentFamily.NEW) newer(17, listOf(if (muted) 6 else 5))
        else media(family, MediaAction.PLAY)
    fun radioPreset(position: Int): ByteArray { require(position in 1..6); return newer(25, listOf(position)) }
    fun nextRadioBand() = newer(26, listOf(1))
    fun storeRadioPreset(position: Int, whole: Int, fraction: Int): ByteArray {
        require(position in 1..6 && whole in 0..65535 && fraction in 0..99)
        // Original generator adds the 16-bit frequency as ONE number, unlike receive CRC.
        return newer(32, listOf(position, whole shr 8, whole and 255, 46, fraction), (32 + 5 + position + whole + 46 + fraction) and 255)
    }
    fun climate(switch: ClimateSwitch, enabled: Boolean) = newer(36, listOf(switch.selector, if (enabled) 1 else 0))
    fun fan(level: Int): ByteArray {
        require(level in 0..7)
        return newer(36, if (level == 0) listOf(5, 0) else listOf(2, level + 1))
    }
    fun vent(mode: Int): ByteArray { require(mode in 0..5); return newer(36, listOf(7, mode)) }
    fun temperature(celsius: Double): ByteArray {
        require(celsius in 17.0..32.0 && celsius * 2 == (celsius * 2).toInt().toDouble())
        val whole = celsius.toInt()
        return newer(38, listOf(whole shr 8, whole and 255, 46, if (celsius % 1.0 == 0.0) 0 else 5))
    }
}
