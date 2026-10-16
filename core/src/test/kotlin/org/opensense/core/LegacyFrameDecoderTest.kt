package org.opensense.core

import kotlin.test.*
import org.junit.Test
import org.opensense.core.protocol.*

class LegacyFrameDecoderTest {
    // Synthetic packets built for offline parser tests. These are not recorded vehicle traffic.
    private fun fixture(payload: ByteArray = byteArrayOf(1)): ByteArray {
        require(payload.size <= 255)
        val bytes = byteArrayOf(0xed.toByte(), 1, 1, 0, 0x21, 3, 1, payload.size.toByte()) +
            payload + byteArrayOf(0, 0, 0xee.toByte())
        val checksum = LegacyCrc16.compute(bytes, bytes.size - 3)
        bytes[bytes.size - 3] = (checksum ushr 8).toByte()
        bytes[bytes.size - 2] = checksum.toByte()
        return bytes
    }

    @Test fun `CRC matches independent published check value`() {
        assertEquals(0x31c3, LegacyCrc16.compute("123456789".toByteArray()))
        assertEquals(0, LegacyCrc16.compute(byteArrayOf()))
    }

    @Test fun `every fragmentation boundary preserves packet`() {
        val bytes = fixture(byteArrayOf(0xed.toByte(), 0xee.toByte(), 0, 0xff.toByte()))
        for (split in 0..bytes.size) {
            val decoder = LegacyFrameDecoder()
            val results = decoder.feed(bytes.copyOfRange(0, split)) + decoder.feed(bytes.copyOfRange(split, bytes.size))
            val frame = assertIs<LegacyDecodeResult.Packet>(results.single()).frame
            assertEquals(0x21, frame.messageType)
            assertEquals(3, frame.featureId)
            assertContentEquals(bytes.copyOfRange(8, 12), frame.payload)
            assertEquals(0, decoder.bufferedBytes)
        }
    }

    @Test fun `empty and maximum payload and concatenated frames`() {
        val decoder = LegacyFrameDecoder()
        val maxPayload = ByteArray(255) { it.toByte() }
        val frames = decoder.feed(fixture(byteArrayOf()) + fixture(maxPayload)).filterIsInstance<LegacyDecodeResult.Packet>()
        assertEquals(2, frames.size)
        assertTrue(frames.first().frame.payload.isEmpty())
        assertContentEquals(maxPayload, frames.last().frame.payload)
        assertEquals(0, decoder.bufferedBytes)
    }

    @Test fun `bad CRC and trailer rejected before following valid packet`() {
        for (index in listOf(8, 9, 10, 11)) {
            val bad = fixture().also { it[index] = (it[index].toInt() xor 1).toByte() }
            val results = LegacyFrameDecoder().feed(bad + fixture())
            assertTrue(results.any { it is LegacyDecodeResult.Rejected })
            assertEquals(1, results.filterIsInstance<LegacyDecodeResult.Packet>().size)
        }
    }

    @Test fun `reset discards partial packet and garbage is bounded`() {
        val decoder = LegacyFrameDecoder()
        decoder.feed(fixture().copyOfRange(0, 5))
        decoder.reset()
        val results = decoder.feed(ByteArray(100_000) { 0 } + fixture())
        assertEquals(1, results.filterIsInstance<LegacyDecodeResult.Packet>().size)
        assertEquals(0, decoder.bufferedBytes)
    }

    @Test fun `malformed random streams never exceed packet storage bound`() {
        val decoder = LegacyFrameDecoder()
        val random = kotlin.random.Random(500)
        repeat(1000) {
            decoder.feed(random.nextBytes(random.nextInt(0, 512)))
            assertTrue(decoder.bufferedBytes < LegacyFrameDecoder.MAX_FRAME_BYTES)
        }
        decoder.reset()
        assertIs<LegacyDecodeResult.Packet>(decoder.feed(fixture()).single())
    }
}
