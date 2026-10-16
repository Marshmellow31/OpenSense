package org.opensense.core.transport

import kotlinx.coroutines.flow.Flow

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, RETRYING, FAILED }

/** No write API: no experimental control or initialization messages can be sent. */
interface ReadOnlyTransport {
    fun frames(): Flow<ByteArray>
}
