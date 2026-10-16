package org.opensense.core.protocol

import org.opensense.core.model.*

data class SessionWrite(val bytes: ByteArray, val delayMillis: Long = 0)
data class SessionUpdate(val writes: List<SessionWrite> = emptyList(), val accepted: Int = 0,
    val rejected: Int = 0, val fatal: Boolean = false)

/** One connection, one family, one initial stream boundary. No Android dependency. */
class VehicleSession(val family: InfotainmentFamily, private val now: () -> Long = System::currentTimeMillis) {
    private val legacy = LegacyFrameDecoder()
    private val newer = NewBlueSenseStreamDecoder()
    var vehicle = LiveVehicle(); private set
    @Volatile var ready = false; private set
    private var initialized = false
    private val fresh = mutableMapOf<String, Long>()
    @Volatile var busy = false; private set
    @Volatile var celsius: Boolean? = null; private set
    fun start() = listOf(SessionWrite(VehicleCommands.startup(family), if (family == InfotainmentFamily.LEGACY) 2000 else 0))
    @Synchronized fun receive(bytes: ByteArray): SessionUpdate {
        val writes = mutableListOf<SessionWrite>()
        var accepted = 0; var rejected = 0; var fatal = false
        if (family == InfotainmentFamily.LEGACY) {
            legacy.feed(bytes).forEach { result -> when (result) {
                is LegacyDecodeResult.Rejected -> rejected++
                is LegacyDecodeResult.Packet -> {
                    val f = result.frame
                    if (f.version != 1 || f.applicationId != 1) { rejected++; return@forEach }
                    accepted++
                    if (f.messageType == 2 && f.featureId == 5 && f.functionId == 1) { fatal = true; return@forEach }
                    if (f.messageType == 1 && f.featureId == 5 && f.functionId == 1 && !initialized) {
                        initialized = true; ready = true
                        listOf(2 to 1, 3 to 1, 4 to 3, 4 to 2, 4 to 1).forEachIndexed { index, (feature, function) ->
                            writes += SessionWrite(VehicleCommands.legacyRequest(feature, function), index * 100L)
                        }
                        listOf(5, 2, 1, 4).forEachIndexed { index, function ->
                            writes += SessionWrite(VehicleCommands.legacyAlertQuery(function), (index + 1) * 4000L)
                        }
                    }
                    if (f.messageType == 0x21) {
                        val p = f.payload.map { it.toInt() and 255 }
                        if (applyLegacy(f.featureId, f.functionId, p)) {
                            writes += SessionWrite(VehicleCommands.legacyAck(f.featureId, f.functionId))
                            if (f.featureId == 2 && f.functionId == 1) writes += SessionWrite(VehicleCommands.legacyRequest(2, 1), 1000)
                            if (f.featureId == 4 && f.functionId == 2) writes += SessionWrite(VehicleCommands.legacyRequest(4, 2), 4000)
                        }
                    }
                }
            } }
        } else {
            newer.feed(bytes).forEach { result -> when (result) {
                is NewDecodeResult.Rejected -> { rejected++; fatal = true }
                is NewDecodeResult.Frame -> {
                    accepted++
                    val f = result.value
                    if (f.messageId == 1 && f.payload.size == 1) {
                        busy = f.payload[0] == 1
                        writes += SessionWrite(VehicleCommands.pollReply(), 1000)
                        if (!initialized) { writes += SessionWrite(VehicleCommands.configuration()); initialized = true }
                        ready = true
                    }
                    if (f.messageId == 4 && f.payload == listOf(4)) fatal = true
                    applyNew(f)
                }
            } }
        }
        return SessionUpdate(writes, accepted, rejected, fatal)
    }
    private fun flag(i: Int) = when(i) { 0 -> false; 1 -> true; else -> null }
    private fun List<Int>.word(offset: Int) = (this[offset] shl 8) or this[offset + 1]
    private fun text(p: List<Int>) = p.takeWhile { it != 0 }.filter { it in 32..126 }.take(80).map(Int::toChar).joinToString("").trim().ifBlank { null }
    private fun applyLegacy(feature: Int, function: Int, p: List<Int>): Boolean {
        when {
            feature == 1 && p.isNotEmpty() -> vehicle = when (function) {
                1 -> vehicle.copy(service = p[0]); 2 -> vehicle.copy(doors = p[0]); 4 -> vehicle.copy(brake = p[0])
                5 -> if (p.size >= 17) vehicle.copy(tyreTemperatures = p.subList(7, 12), tyrePressures = p.subList(12, 17)) else return false
                else -> return false
            }
            feature == 2 && function == 1 && p.size >= 7 -> vehicle = vehicle.copy(wipers = p[0], parkLamp = p[1], lamps = p[2], range = p.word(3))
            feature == 2 && function == 3 && p.size >= 2 -> vehicle = vehicle.copy(speed = p.word(0))
            feature == 3 && function == 1 && p.size >= 8 -> vehicle = vehicle.copy(climate = LiveClimate(
                fan = p[0], mode = p[1], econ = flag(p[2]), auto = flag(p[3]), ac = flag(p[4]), recirculate = flag(p[5]),
                target = p[6].takeIf { it != 255 && it != 0 }?.let { it * .5 - 40 },
                outside = p[7].takeIf { it != 255 && it != 0 }?.let { it - 40.0 }))
            feature == 4 && function == 1 && p.size >= 5 -> vehicle = vehicle.copy(media = (vehicle.media ?: LiveMedia()).copy(source = p[0]))
            feature == 4 && function == 2 && p.size >= 2 -> vehicle = vehicle.copy(media = (vehicle.media ?: LiveMedia()).copy(volume = p[1]))
            feature == 4 && function == 3 && p.size >= 2 -> vehicle = vehicle.copy(media = (vehicle.media ?: LiveMedia()).copy(title = text(p.drop(1))))
            else -> return false
        }
        fresh["$feature/$function"] = now()
        return true
    }
    private fun applyNew(f: NewBlueSenseFrame) {
        val p = f.payload
        val supported = when {
            f.messageId == 6 && p.size == 7 -> true
            f.messageId == 10 && p.size == 7 -> true
            f.messageId == 19 && p.size == 12 -> true
            f.messageId == 20 && p.size == 6 -> true
            f.messageId == 23 && p.size == 61 -> true
            f.messageId == 13 && p.size == 61 -> true
            else -> false
        }
        if (supported) fresh["new/${f.messageId}"] = now()
        when {
            f.messageId == 6 && p.size == 7 -> vehicle = vehicle.copy(speed = p.word(1))
            f.messageId == 10 && p.size == 7 -> vehicle = vehicle.copy(range = p.word(5), economy = "${p[1]}.${p[3]}")
            f.messageId == 19 && p.size == 12 -> vehicle = vehicle.copy(media = (vehicle.media ?: LiveMedia()).copy(
                availableSources = listOf(0, 1, 5, 3, 4, 2).filterIndexed { index, _ -> p[index * 2 + 1] == 1 }.toSet()))
            f.messageId == 20 && p.size == 6 -> vehicle = vehicle.copy(media = (vehicle.media ?: LiveMedia()).copy(playback = p[1], source = p[3], volume = p[5]))
            f.messageId == 23 && p.size == 61 -> vehicle = vehicle.copy(media = (vehicle.media ?: LiveMedia()).copy(title = text(p.drop(1))))
            f.messageId == 13 && p.size == 61 -> vehicle = vehicle.copy(doors = p[2], service = p[52])
            f.messageId == 21 && p.size == 1 -> {
                fresh["preset"] = now()
                vehicle = vehicle.copy(media = (vehicle.media ?: LiveMedia()).copy(preset = p[0]))
            }
            // Mirrors the original's length-based climate dispatch. The UI identifies all live
            // mappings as APK-derived/unconfirmed; never infer hardware fitment from this.
            p.size == 21 -> {
                fresh["climate"] = now()
                val whole = ((p[7] and 127) shl 8) or p[8]
                val target = "$whole.${p[10]}".toDoubleOrNull()?.takeIf { it in 17.0..32.0 }
                vehicle = vehicle.copy(climate = LiveClimate(outside = vehicle.climate?.outside, ac = flag(p[1]), econ = flag(p[3]),
                    fan = p[5], target = target, recirculate = flag(p[12]), auto = flag(p[16]), mode = p[18], rear = flag(p[20])))
            }
            p.size == 2 -> { if (p[1] in 0..1) celsius = p[1] == 0 }
            p.size == 11 -> {
                fresh["lamps"] = now()
                val old = vehicle.climate ?: LiveClimate()
                vehicle = vehicle.copy(wipers = p[1], lamps = p[3], parkLamp = p[5],
                    climate = old.copy(outside = decimal(p, 7)))
                fresh["outside"] = now()
            }
            p.size == 14 -> {
                fresh["radio"] = now()
                vehicle = vehicle.copy(media = (vehicle.media ?: LiveMedia()).copy(radioBand = p[1], station = text(p.subList(4, 12)), preset = p[13]))
                fresh["preset"] = now()
            }
            p.size == 50 -> {
                fresh["tyres"] = now()
                vehicle = vehicle.copy(newTyres = listOf("Front left", "Front right", "Rear left", "Rear right").mapIndexed { index, label ->
                    val offset = 1 + index * 10
                    TyreReading(label, decimal(p, offset)?.toString(), decimal(p, offset + 5)?.toString())
                })
            }
            p.size == 171 -> {
                // New utils.n length 1392 bits -> utils.f.a(byte[], Context), current EcoSense.
                // Identity/location segments are intentionally ignored.
                fresh["eco"] = now()
                fun metric(index: Int) = p[index].takeUnless { it == 255 }
                fun wordMetric(index: Int) = p.word(index).takeUnless { it == 65535 }
                val hour = p[14]; val minute = p[15] and 127
                vehicle = vehicle.copy(ecoSense = LiveEcoSense(metric(12)?.takeIf { it <= 100 }, metric(17)?.div(10.0), wordMetric(9)?.div(10.0),
                    wordMetric(19), wordMetric(22), if (hour != 255 && minute < 60) "$hour:${minute.toString().padStart(2, '0')}" else null,
                    metric(25), metric(27), metric(29), metric(31), metric(33)))
            }
        }
    }
    private fun decimal(p: List<Int>, offset: Int): Double? {
        val sign = if (p[offset] and 128 != 0) "-" else ""
        val whole = ((p[offset] and 127) shl 8) or p[offset + 1]
        if (p[offset + 2] != 46) return null
        return "$sign$whole.${p[offset + 3]}".toDoubleOrNull()
    }
    /** Invalidate individual sections even if poll messages continue arriving. */
    @Synchronized fun expire(): LiveVehicle {
        fun old(key: String) = now() - (fresh[key] ?: Long.MIN_VALUE / 2) > 20000
        vehicle = if (family == InfotainmentFamily.LEGACY) vehicle.copy(
            range = vehicle.range.takeUnless { old("2/1") }, speed = vehicle.speed.takeUnless { old("2/3") },
            doors = vehicle.doors.takeUnless { old("1/2") }, service = vehicle.service.takeUnless { old("1/1") }, brake = vehicle.brake.takeUnless { old("1/4") },
            lamps = vehicle.lamps.takeUnless { old("2/1") }, wipers = vehicle.wipers.takeUnless { old("2/1") }, parkLamp = vehicle.parkLamp.takeUnless { old("2/1") },
            climate = vehicle.climate.takeUnless { old("3/1") },
            tyrePressures = vehicle.tyrePressures.takeUnless { old("1/5") }, tyreTemperatures = vehicle.tyreTemperatures.takeUnless { old("1/5") },
            media = vehicle.media?.copy(source = vehicle.media?.source.takeUnless { old("4/1") }, volume = vehicle.media?.volume.takeUnless { old("4/2") }, title = vehicle.media?.title.takeUnless { old("4/3") }))
        else vehicle.copy(range = vehicle.range.takeUnless { old("new/10") }, economy = vehicle.economy.takeUnless { old("new/10") },
            doors = vehicle.doors.takeUnless { old("new/13") }, service = vehicle.service.takeUnless { old("new/13") },
            lamps = vehicle.lamps.takeUnless { old("lamps") }, wipers = vehicle.wipers.takeUnless { old("lamps") }, parkLamp = vehicle.parkLamp.takeUnless { old("lamps") },
            newTyres = vehicle.newTyres.takeUnless { old("tyres") },
            ecoSense = vehicle.ecoSense.takeUnless { old("eco") },
            speed = vehicle.speed.takeUnless { old("new/6") }, climate = vehicle.climate?.let { c ->
                val main = if (old("climate")) LiveClimate() else c
                main.copy(outside = c.outside.takeUnless { old("outside") })
            },
            media = vehicle.media?.copy(source = vehicle.media?.source.takeUnless { old("new/20") }, playback = vehicle.media?.playback.takeUnless { old("new/20") },
                volume = vehicle.media?.volume.takeUnless { old("new/20") }, title = vehicle.media?.title.takeUnless { old("new/23") },
                availableSources = vehicle.media?.availableSources?.takeUnless { old("new/19") }.orEmpty()))
        if (family == InfotainmentFamily.NEW) vehicle = vehicle.copy(media = vehicle.media?.copy(
            radioBand = vehicle.media?.radioBand.takeUnless { old("radio") }, station = vehicle.media?.station.takeUnless { old("radio") }, preset = vehicle.media?.preset.takeUnless { old("preset") }))
        return vehicle
    }
}
