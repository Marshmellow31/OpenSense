package org.opensense.core.simulation

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.opensense.core.model.VehicleSnapshot
import org.opensense.core.protocol.SimulationCodec
import org.opensense.core.transport.ReadOnlyTransport

class SimulatedTransport(private val intervalMillis: Long = 1500) : ReadOnlyTransport {
    override fun frames(): Flow<ByteArray> = flow {
        var sequence = 0L
        while (true) {
            emit(SimulationCodec.encode(VehicleSnapshot(sequence, 68, 412, 23, 31, true, sequence % 4 != 0L)))
            sequence++
            delay(intervalMillis)
        }
    }
}
