package org.opensense.core

import org.junit.Test
import kotlin.test.*
import org.opensense.core.model.InfotainmentFamily.*
import org.opensense.core.protocol.*

/** Synthetic streams and fixed APK-derived command vectors; no vehicle captures. */
class VehicleSessionTest {
    private fun hex(s: String) = s.replace(" ", "").chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    private fun newerIncoming(id: Int, vararg p: Int): ByteArray {
        // Independent receive fixture in logical-byte notation, nibble-offset wire direction.
        val logical = listOf(id, (id + p.size + p.sum()) % 256, p.size) + p.toList()
        return logical.flatMap { listOf((80 + it / 16).toByte(), (80 + it % 16).toByte()) }.toByteArray()
    }
    @Test fun legacyGoldenVectorsMatchRecoveredGenerators() {
        // CRCs independently cross-checked using Python binascii.crc_hqx, init 0.
        assertContentEquals(hex("ed0101001105010101e078ee"), VehicleCommands.startup(LEGACY))
        assertContentEquals(hex("ed0101012002010101524aee"), VehicleCommands.legacyRequest(2, 1))
        assertContentEquals(hex("ed0101001104010102a6afee"), VehicleCommands.media(LEGACY, MediaAction.NEXT))
        assertContentEquals(hex("ed010100110402012d2a72ee"), VehicleCommands.volume(LEGACY, 45))
    }
    @Test fun newerGoldenVectorsKeepTransmitAndReceiveDirectionsDistinct() {
        assertContentEquals(hex("02 0e 03 0a 00 06 00 00 00 01 00 01 00 01 00 02 00 01"), VehicleCommands.startup(NEW))
        assertContentEquals(hex("00 01 00 02 00 01 00 00"), VehicleCommands.pollReply())
        assertContentEquals(hex("01 01 01 05 00 01 00 03"), VehicleCommands.media(NEW, MediaAction.NEXT))
        assertContentEquals(hex("01 02 01 06 00 01 00 03"), VehicleCommands.media(NEW, MediaAction.BLUETOOTH))
        assertContentEquals(hex("01 05 03 04 00 01 01 0e"), VehicleCommands.volume(NEW, 30))
        assertTrue(VehicleCommands.climate(ClimateSwitch.AC, true).all { it in 0..15 })
    }
    @Test fun newerTemperatureMatchesSmaliSelector38AndDecimalPointEncoding() {
        assertContentEquals(hex("02 06 07 03 00 04 00 00 01 06 02 0e 00 05"), VehicleCommands.temperature(22.5))
        assertFailsWith<IllegalArgumentException> { VehicleCommands.temperature(22.3) }
        assertFailsWith<IllegalArgumentException> { VehicleCommands.temperature(16.5) }
        assertFailsWith<IllegalArgumentException> { VehicleCommands.volume(NEW, 31) }
        assertFailsWith<IllegalArgumentException> { VehicleCommands.volume(LEGACY, 46) }
    }
    @Test fun radioMuteIsDistinctFromMusicPlaybackOnNewBlueSense() {
        assertContentEquals(hex("01 01 01 08 00 01 00 06"), VehicleCommands.radioMute(NEW, true))
        assertContentEquals(hex("01 01 01 07 00 01 00 05"), VehicleCommands.radioMute(NEW, false))
        assertContentEquals(VehicleCommands.media(LEGACY, MediaAction.PLAY), VehicleCommands.radioMute(LEGACY, true))
    }
    @Test fun radioPresetSelectionAndStorageHaveDifferentCommandsAndFrequencyChecksum() {
        assertContentEquals(hex("01 09 01 0c 00 01 00 02"), VehicleCommands.radioPreset(2))
        assertContentEquals(hex("01 0a 01 0c 00 01 00 01"), VehicleCommands.nextRadioBand())
        // AM frequency is summed as a 16-bit number in the original outgoing generator.
        assertContentEquals(hex("02 00 03 0d 00 05 00 02 00 03 0e 08 02 0e 00 00"), VehicleCommands.storeRadioPreset(2, 1000, 0))
    }
    @Test fun newerPollHandshakeIsFragmentSafeAndConfigurationSentOnce() {
        val session = VehicleSession(NEW)
        val poll = newerIncoming(1, 0)
        assertEquals(0, session.receive(poll.copyOfRange(0, 3)).accepted)
        assertFalse(session.ready)
        val first = session.receive(poll.copyOfRange(3, poll.size))
        assertTrue(session.ready)
        assertEquals(2, first.writes.size)
        assertEquals(1000, first.writes.first().delayMillis)
        assertContentEquals(VehicleCommands.pollReply(), first.writes.first().bytes)
        assertEquals(1, session.receive(poll).writes.size)
        session.receive(newerIncoming(1, 1)); assertTrue(session.busy)
        session.receive(poll); assertFalse(session.busy)
    }
    @Test fun legacyHandshakeRequiresStartupAckAndRejectsNack() {
        val session = VehicleSession(LEGACY)
        session.receive(VehicleCommands.legacy(1, 4, 1, listOf(1)))
        assertFalse(session.ready)
        val ack = session.receive(VehicleCommands.legacy(1, 5, 1, listOf(1)))
        assertTrue(session.ready); assertEquals(9, ack.writes.size)
        assertTrue(session.receive(VehicleCommands.legacy(2, 5, 1, listOf(1))).fatal)
        assertTrue(session.receive(VehicleCommands.legacy(1, 5, 1, listOf(1))).writes.isEmpty())
    }
    @Test fun legacyStatusIsChecksumCheckedAndNotSimulated() {
        val session = VehicleSession(LEGACY)
        val packet = VehicleCommands.legacy(0x21, 2, 1, listOf(1, 0, 1, 1, 156, 0, 20))
        val corrupt = packet.copyOf().also { it[8] = 7 }
        assertTrue(session.receive(corrupt).rejected > 0); assertNull(session.vehicle.range)
        session.receive(packet)
        assertEquals(412, session.vehicle.range); assertNull(session.vehicle.climate)
        assertEquals(0, session.vehicle.parkLamp)
    }
    @Test fun shortLegacyPayloadDoesNotInventFieldsOrCrash() {
        val session = VehicleSession(LEGACY)
        session.receive(VehicleCommands.legacy(0x21, 3, 1, listOf(1)))
        assertNull(session.vehicle.climate)
        session.receive(VehicleCommands.legacy(0x21, 3, 1, listOf(3, 1, 0, 1, 1, 0, 124, 70)))
        assertEquals(22.0, session.vehicle.climate?.target)
        assertEquals(30.0, session.vehicle.climate?.outside)
    }
    @Test fun newerFuelAndMediaHaveExactLengthsAndIndependentFields() {
        val session = VehicleSession(NEW)
        session.receive(newerIncoming(10, 0, 14, 46, 5, 0, 1, 156))
        assertEquals(412, session.vehicle.range); assertEquals("14.5", session.vehicle.economy)
        session.receive(newerIncoming(20, 0, 1, 0, 3, 0, 12))
        assertEquals(12, session.vehicle.media?.volume); assertEquals(3, session.vehicle.media?.source)
        session.receive(newerIncoming(10, 1))
        assertEquals(412, session.vehicle.range)
    }
    @Test fun newestClimateShapeDecodesStatusButDoesNotPretendUnitsAreKnown() {
        val session = VehicleSession(NEW)
        val p = IntArray(21); p[1] = 1; p[5] = 4; p[8] = 22; p[9] = 46; p[10] = 5
        session.receive(newerIncoming(37, *p))
        assertEquals(22.5, session.vehicle.climate?.target); assertEquals(true, session.vehicle.climate?.ac)
        assertNull(session.celsius)
        session.receive(newerIncoming(9, 0, 0)); assertEquals(true, session.celsius)
    }
    @Test fun invalidNewerStreamIsFatalAndCannotSelfRealign() {
        val session = VehicleSession(NEW)
        assertTrue(session.receive(byteArrayOf(0)).fatal)
        assertEquals(0, session.receive(newerIncoming(10, 0, 14, 46, 5, 0, 1, 156)).accepted)
        assertNull(session.vehicle.range)
    }
    @Test fun individualReadingsExpireWhilePollsContinue() {
        var now = 0L
        val session = VehicleSession(NEW) { now }
        session.receive(newerIncoming(10, 0, 14, 46, 5, 0, 1, 156))
        now = 19000; session.receive(newerIncoming(1, 0)); assertEquals(412, session.expire().range)
        now = 21000; session.receive(newerIncoming(1, 0)); assertNull(session.expire().range)
        assertTrue(session.ready)
    }
    @Test fun truncatedButChecksummedMessagesCannotRefreshOlderReadings() {
        var now = 0L
        val session = VehicleSession(NEW) { now }
        session.receive(newerIncoming(10, 0, 14, 46, 5, 0, 1, 156))
        now = 19000; session.receive(newerIncoming(10, 1))
        now = 21000; assertNull(session.expire().range)
    }
    @Test fun newerWarningAndLampFieldsMatchRecoveredByteOffsets() {
        val session = VehicleSession(NEW)
        val warning = IntArray(61); warning[2] = 4; warning[52] = 1
        session.receive(newerIncoming(13, *warning))
        assertEquals(4, session.vehicle.doors); assertEquals(1, session.vehicle.service)
        val lamp = intArrayOf(0, 1, 1, 0, 2, 1, 3, 0, 30, 46, 5)
        session.receive(newerIncoming(34, *lamp))
        assertEquals(1, session.vehicle.wipers); assertEquals(0, session.vehicle.lamps)
        assertEquals(30.5, session.vehicle.climate?.outside)
        assertFalse(session.vehicle.climate!!.hasStatus) // outside alone cannot enable HVAC writes
    }
    @Test fun newerClimateAndTyresExpireWithoutUnitOrFitmentAssumptions() {
        var now = 0L
        val session = VehicleSession(NEW) { now }
        val tyres = IntArray(50)
        listOf(1, 6, 11, 16, 21, 26, 31, 36).forEach { offset -> tyres[offset + 1] = 32; tyres[offset + 2] = 46; tyres[offset + 3] = 5 }
        session.receive(newerIncoming(15, *tyres))
        assertEquals("32.5", session.vehicle.newTyres?.first()?.pressure)
        assertEquals(4, session.vehicle.newTyres?.size) // spare mapping is not recovered
        assertNull(session.celsius)
        now = 21000; assertNull(session.expire().newTyres)
    }
    @Test fun currentEcoSenseRecordUsesOriginalOffsetsAndDiscardsPrivateSegments() {
        val session = VehicleSession(NEW)
        val p = IntArray(171); p[12] = 81; p[17] = 145; p[9] = 1; p[10] = 244
        p[14] = 1; p[15] = 20; p[25] = 2; p[27] = 3; p[22] = 0; p[23] = 45
        session.receive(newerIncoming(50, *p))
        assertEquals(81, session.vehicle.ecoSense?.score)
        assertEquals(14.5, session.vehicle.ecoSense?.economy)
        assertEquals(50.0, session.vehicle.ecoSense?.distanceKm)
        assertEquals("1:20", session.vehicle.ecoSense?.duration)
        assertEquals(45, session.vehicle.ecoSense?.averageSpeed)
    }
}
