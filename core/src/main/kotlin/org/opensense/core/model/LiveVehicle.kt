package org.opensense.core.model

/** Null means not received/unsupported. Never populate these values from simulation. */
data class LiveClimate(val fan: Int? = null, val mode: Int? = null, val target: Double? = null,
    val outside: Double? = null, val ac: Boolean? = null, val auto: Boolean? = null,
    val econ: Boolean? = null, val recirculate: Boolean? = null, val rear: Boolean? = null) {
    val hasStatus: Boolean get() = fan in 0..8 || ac != null || auto != null || target != null
}
data class LiveMedia(val source: Int? = null, val playback: Int? = null, val volume: Int? = null,
    val title: String? = null, val availableSources: Set<Int> = emptySet(), val radioBand: Int? = null, val station: String? = null, val preset: Int? = null)
data class TyreReading(val position: String, val pressure: String?, val temperature: String?)
data class LiveEcoSense(val score: Int?, val economy: Double?, val distanceKm: Double?, val maxSpeed: Int?, val averageSpeed: Int?,
    val duration: String?, val accelerationEvents: Int?, val brakingEvents: Int?, val clutchEvents: Int?, val gearEvents: Int?, val overspeedEvents: Int?)
data class LiveVehicle(val range: Int? = null, val economy: String? = null, val speed: Int? = null,
    val doors: Int? = null, val service: Int? = null, val brake: Int? = null,
    val lamps: Int? = null, val wipers: Int? = null, val parkLamp: Int? = null,
    val climate: LiveClimate? = null, val media: LiveMedia? = null,
    val tyrePressures: List<Int>? = null, val tyreTemperatures: List<Int>? = null,
    val newTyres: List<TyreReading>? = null, val ecoSense: LiveEcoSense? = null)
