package org.opensense.core.model

/** A user-selected research profile, never evidence of physical compatibility. */
enum class InfotainmentFamily(val title: String, val originalApp: String, val variants: List<String>) {
    LEGACY("XUV500 · Legacy BlueSense", "Original MAHINDRA BLUE SENSE APP", listOf("W8", "Other / unsure")),
    NEW("XUV500 · New BlueSense", "NEW BLUE SENSE – XUV500", listOf("W6", "W8", "W10", "W7", "W9", "W11", "Other / unsure")),
}
data class VehicleProfile(val family: InfotainmentFamily, val variant: String) {
    init { require(variant in family.variants) }
    val label: String get() = "XUV500 $variant · ${if (family == InfotainmentFamily.LEGACY) "Legacy" else "New BlueSense"}"
}
