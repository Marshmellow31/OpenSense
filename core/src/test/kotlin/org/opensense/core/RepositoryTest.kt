package org.opensense.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import kotlin.test.*
import org.junit.Test
import org.opensense.core.diagnostics.*
import org.opensense.core.model.*
import org.opensense.core.protocol.SimulationCodec
import org.opensense.core.repository.SimulationRepository
import org.opensense.core.simulation.SimulatedTransport
import org.opensense.core.transport.*

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryTest {
    @Test fun `connect disconnect reconnect clears stale telemetry`() = runTest {
        val repository = SimulationRepository(SimulatedTransport(), backgroundScope, DiagnosticLog())
        repository.connect(); runCurrent()
        assertEquals(ConnectionState.CONNECTED, repository.state.value)
        assertNotNull(repository.sample.value)
        repository.disconnect(); runCurrent()
        assertNull(repository.sample.value)
        assertEquals(ConnectionState.DISCONNECTED, repository.state.value)
        repository.connect(); runCurrent()
        assertEquals(0, repository.sample.value?.sequence)
        repository.disconnect()
    }
    @Test fun `mock transport retries transient failure and recovers`() = runTest {
        var attempts = 0
        val transport = object : ReadOnlyTransport {
            override fun frames() = flow {
                if (attempts++ == 0) error("private MAC AA:BB:CC:DD:EE:FF must never be logged")
                emit(SimulationCodec.encode(VehicleSnapshot(1, 50, 300, 22, 30, true, false)))
                awaitCancellation()
            }
        }
        val log = DiagnosticLog()
        val repository = SimulationRepository(transport, backgroundScope, log, retryMillis = 50)
        repository.connect(); runCurrent()
        assertEquals(ConnectionState.RETRYING, repository.state.value)
        advanceTimeBy(50); runCurrent()
        assertEquals(ConnectionState.CONNECTED, repository.state.value)
        assertEquals(2, attempts)
        assertFalse(log.events.value.toString().contains("AA:BB"))
        repository.disconnect()
    }
    @Test fun `persistent failure has bounded retries`() = runTest {
        var attempts = 0
        val transport = object : ReadOnlyTransport {
            override fun frames() = flow<ByteArray> { attempts++; error("unavailable") }
        }
        val repository = SimulationRepository(transport, backgroundScope, DiagnosticLog(), retryMillis = 10)
        repository.connect(); runCurrent()
        advanceTimeBy(30); runCurrent()
        assertEquals(3, attempts)
        assertEquals(ConnectionState.FAILED, repository.state.value)
        assertNull(repository.sample.value)
    }
    @Test fun `disconnect during retry cancels automatic reconnect`() = runTest {
        var attempts = 0
        val transport = object : ReadOnlyTransport {
            override fun frames() = flow<ByteArray> { attempts++; error("unavailable") }
        }
        val repository = SimulationRepository(transport, backgroundScope, DiagnosticLog(), retryMillis = 10)
        repository.connect(); runCurrent(); repository.disconnect()
        advanceTimeBy(100); runCurrent()
        assertEquals(1, attempts)
        assertEquals(ConnectionState.DISCONNECTED, repository.state.value)
    }
    @Test fun `partial data cannot leak across reconnect`() = runTest {
        var attempts = 0
        val transport = object : ReadOnlyTransport {
            override fun frames() = flow {
                if (attempts++ == 0) { emit("SIM1|1|50|".toByteArray()); error("drop") }
                emit(SimulationCodec.encode(VehicleSnapshot(2, 50, 300, 22, 30, true, false)))
                awaitCancellation()
            }
        }
        val repository = SimulationRepository(transport, backgroundScope, DiagnosticLog(), retryMillis = 10)
        repository.connect(); runCurrent(); advanceTimeBy(10); runCurrent()
        assertEquals(2, repository.sample.value?.sequence)
        repository.disconnect()
    }
    @Test fun `capabilities never advertise real support`() {
        assertTrue(capabilities(false).all { it.support == Support.UNVERIFIED })
        assertEquals(Support.UNVERIFIED, capabilities(true).first { it.feature == Feature.TYRES }.support)
    }
    @Test fun `diagnostics bounded and clearable`() {
        val log = DiagnosticLog { 42L }
        repeat(200) { log.record(DiagnosticCode.SAMPLE_ACCEPTED) }
        assertEquals(100, log.events.value.size)
        log.clear(); assertTrue(log.events.value.isEmpty())
    }
}
