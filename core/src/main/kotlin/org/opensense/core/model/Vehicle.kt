package org.opensense.core.model

enum class DataOrigin { SIMULATED }
enum class Feature(val title: String, val requirement: String) {
    MEDIA("Media", "Head-unit audio service and verified commands"),
    CLIMATE("Climate", "Compatible HVAC controller and head-unit integration"),
    FUEL("Fuel & range", "Vehicle signals exposed by the head unit"),
    TYRES("Tyre pressure", "Factory TPMS hardware; W7 fitment unconfirmed"),
    DOORS("Door status", "Body status exposed by the head unit"),
    TRIPS("Trips & EcoSense", "Compatible transmission and infotainment firmware"),
    PARKING("Parked location", "Phone location consent; data origin unconfirmed"),
}
enum class Support { SIMULATED, UNVERIFIED }
data class Capability(val feature: Feature, val support: Support)
data class VehicleSnapshot(
    val sequence: Long,
    val fuelPercent: Int,
    val rangeKm: Int,
    val cabinCelsius: Int,
    val outsideCelsius: Int,
    val doorsClosed: Boolean,
    val mediaPlaying: Boolean,
    val origin: DataOrigin = DataOrigin.SIMULATED,
)
fun capabilities(simulated: Boolean) = Feature.entries.map {
    Capability(it, if (simulated && it in setOf(Feature.MEDIA, Feature.CLIMATE, Feature.FUEL, Feature.DOORS))
        Support.SIMULATED else Support.UNVERIFIED)
}
