package org.opensense.core

import kotlin.test.*
import org.junit.Test
import org.opensense.core.protocol.*

class NewBlueSenseDecoderTest {
    // Synthetic framing fixtures, never captured vehicle responses.
    private fun fixture(id: Int = 10, payload: List<Int> = listOf(0x12, 0x34)): ByteArray {
        val logical = listOf(id, (id + payload.size + payload.sum()) and 0xff, payload.size) + payload
        return logical.flatMap { listOf(((it shr 4) + 0x50).toByte(), ((it and 15) + 0x50).toByte()) }.toByteArray()
    }
    @Test fun `incoming nibble envelope and sum decode without telemetry interpretation`() {
        assertEquals(NewDecodeResult.Frame(NewBlueSenseFrame(10, listOf(0x12, 0x34))), NewBlueSenseDecoder.decode(fixture()))
    }
    @Test fun `fragmented and concatenated aligned input`() {
        val decoder = NewBlueSenseStreamDecoder()
        val bytes = fixture()
        assertTrue(decoder.feed(bytes.copyOfRange(0, 5)).isEmpty())
        assertEquals(2, decoder.feed(bytes.copyOfRange(5, bytes.size) + bytes).size)
    }
    @Test fun `checksum length alphabet and odd sizes rejected`() {
        val checksum = fixture().also { it[2] = 0x50 }
        val length = fixture().also { it[5] = 0x53 }
        val alphabet = fixture().also { it[8] = 0x4f }
        listOf(checksum, length, alphabet, fixture().dropLast(1).toByteArray(), byteArrayOf()).forEach {
            assertIs<NewDecodeResult.Rejected>(NewBlueSenseDecoder.decode(it))
        }
    }
    @Test fun `maximum length and checksum wraparound`() {
        val payload = List(255) { 255 }
        val result = NewBlueSenseDecoder.decode(fixture(250, payload))
        assertEquals(NewDecodeResult.Frame(NewBlueSenseFrame(250, payload)), result)
        assertIs<NewDecodeResult.Frame>(NewBlueSenseDecoder.decode(fixture(0, emptyList())))
    }
    @Test fun `invalid stream loses alignment until explicit boundary reset`() {
        val decoder = NewBlueSenseStreamDecoder()
        val output = decoder.feed(byteArrayOf(0) + fixture())
        assertEquals(listOf(NewDecodeResult.Rejected("alignment_lost")), output)
        assertTrue(decoder.feed(fixture()).isEmpty())
        decoder.resetAtKnownBoundary()
        assertIs<NewDecodeResult.Frame>(decoder.feed(fixture()).single())
    }
    @Test fun `reset discards an incomplete declared frame`() {
        val decoder = NewBlueSenseStreamDecoder()
        decoder.feed(fixture().copyOfRange(0, 7)); decoder.resetAtKnownBoundary()
        assertIs<NewDecodeResult.Frame>(decoder.feed(fixture()).single())
    }
    @Test fun `arbitrary chunks cannot grow pending data beyond frame bound`() {
        val decoder = NewBlueSenseStreamDecoder()
        repeat(20) { decoder.feed(ByteArray(10000) { 0x5f }) }
        decoder.resetAtKnownBoundary()
        assertIs<NewDecodeResult.Frame>(decoder.feed(fixture()).single())
    }
}
