package org.opensense.core

import kotlin.test.*
import org.junit.Test
import org.opensense.core.model.VehicleSnapshot
import org.opensense.core.protocol.*

class ProtocolTest {
    private val sample = VehicleSnapshot(7, 68, 412, 23, 31, true, false)
    @Test fun `synthetic round trip explicitly retains simulated origin`() {
        val decoded = SimulationStreamDecoder().feed(SimulationCodec.encode(sample))
        assertEquals(listOf(DecodeResult.Sample(sample)), decoded)
    }
    @Test fun `split and concatenated frames`() {
        val decoder = SimulationStreamDecoder()
        val bytes = SimulationCodec.encode(sample)
        assertTrue(decoder.feed(bytes.copyOfRange(0, 5)).isEmpty())
        assertEquals(2, decoder.feed(bytes.copyOfRange(5, bytes.size) + bytes).filterIsInstance<DecodeResult.Sample>().size)
    }
    @Test fun `malformed packets rejected`() {
        listOf("", "SIM1|x|68|412|23|31|1|0", "SIM1|7|101|412|23|31|1|0",
            "SIM1|7|68|-1|23|31|1|0", "SIM1|7|68|412|90|31|1|0", "SIM1|7|68|412|23|31|2|0",
            "SIM1|-1|68|412|23|31|1|0", "SIM1|999999999999999999999|68|412|23|31|1|0",
            "SIM1|7|68|412|23|31|1|0|extra", "REAL|7|68|412|23|31|1|0")
            .forEach { assertIs<DecodeResult.Rejected>(SimulationCodec.decode(it), it) }
    }
    @Test fun `oversized frame discarded then parser resynchronizes`() {
        val decoder = SimulationStreamDecoder(64)
        val output = decoder.feed("x".repeat(10000).toByteArray() + "\n".toByteArray() + SimulationCodec.encode(sample))
        assertEquals(2, output.size)
        assertEquals(DecodeResult.Rejected("frame_too_large"), output.first())
        assertEquals(DecodeResult.Sample(sample), output.last())
    }
    @Test fun `binary garbage rejected and recovery is bounded`() {
        val decoder = SimulationStreamDecoder()
        assertIs<DecodeResult.Rejected>(decoder.feed(byteArrayOf(0xff.toByte(), 10)).single())
        assertIs<DecodeResult.Sample>(decoder.feed(SimulationCodec.encode(sample)).single())
    }
    @Test fun `reset discards partial frame`() {
        val decoder = SimulationStreamDecoder()
        decoder.feed("SIM1|7|".toByteArray()); decoder.reset()
        assertIs<DecodeResult.Sample>(decoder.feed(SimulationCodec.encode(sample)).single())
    }
    @Test fun `real vehicle parser never accepts synthetic or arbitrary data`() {
        assertFalse(VehicleProtocol.verified)
        assertIs<DecodeResult.Rejected>(VehicleProtocol.decode(SimulationCodec.encode(sample)))
        assertIs<DecodeResult.Rejected>(VehicleProtocol.decode(byteArrayOf(1, 2, 3)))
    }
    @Test fun `seeded garbage cannot crash decoder`() {
        val random = kotlin.random.Random(47)
        val decoder = SimulationStreamDecoder()
        repeat(1000) { decoder.feed(random.nextBytes(random.nextInt(0, 1024))) }
        decoder.feed(byteArrayOf(10))
        assertIs<DecodeResult.Sample>(decoder.feed(SimulationCodec.encode(sample)).single())
    }
}
