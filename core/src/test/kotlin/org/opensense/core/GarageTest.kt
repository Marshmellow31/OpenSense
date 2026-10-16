package org.opensense.core

import kotlin.test.*
import org.junit.Test
import org.opensense.core.garage.*

class GarageTest {
    private fun fuel(id: String, odo: Double, litres: Double, full: Boolean) = FuelEntry(id, 1, odo, litres, 10000, full)
    @Test fun partialFillsCountBetweenFullTanksButStartingFuelDoesNot() {
        val intervals = FuelMath.intervals(listOf(fuel("start", 1000.0, 50.0, true), fuel("partial", 1200.0, 10.0, false), fuel("end", 1500.0, 30.0, true)))
        assertEquals(1, intervals.size); assertEquals(40.0, intervals.single().litres)
        assertEquals(12.5, intervals.single().kmPerLitre)
    }
    @Test fun noEfficiencyWithoutTwoFullTanks() {
        assertTrue(FuelMath.intervals(listOf(fuel("a", 1000.0, 30.0, false), fuel("b", 1200.0, 20.0, true))).isEmpty())
    }
    @Test fun nextFullTankStartsANewInterval() {
        val results = FuelMath.intervals(listOf(fuel("a", 0.0, 50.0, true), fuel("b", 500.0, 40.0, true), fuel("c", 900.0, 25.0, true)))
        assertEquals(listOf(12.5, 16.0), results.map { it.kmPerLitre })
    }
    @Test fun malformedFuelNeverEntersTheLedger() {
        assertFailsWith<IllegalArgumentException> { fuel("a", -1.0, 30.0, true) }
        assertFailsWith<IllegalArgumentException> { fuel("a", 1.0, Double.NaN, true) }
        assertFailsWith<IllegalArgumentException> { fuel("a", 1.0, 0.0, true) }
        assertFailsWith<IllegalArgumentException> { FuelEntry("a", 1, 1.0, 1.0, -1, true) }
    }
    @Test fun gpsRejectsLowAccuracyAndImplausibleJumps() {
        val trip = TripAccumulator()
        assertFalse(trip.add(TrackPoint(0.0, 0.0, 0, 100f)))
        assertTrue(trip.add(TrackPoint(0.0, 0.0, 0, 5f)))
        assertFalse(trip.add(TrackPoint(1.0, 1.0, 1000, 5f)))
        assertEquals(0.0, trip.distanceMetres)
    }
    @Test fun gpsComputesDistanceAndDoesNotBridgeLongGaps() {
        val trip = TripAccumulator()
        trip.add(TrackPoint(0.0, 0.0, 0, 5f)); trip.add(TrackPoint(0.0, .0001, 2000, 5f))
        assertEquals(11.12, trip.distanceMetres, .02)
        trip.add(TrackPoint(0.0, .001, 40000, 5f))
        assertEquals(11.12, trip.distanceMetres, .02)
        assertEquals(2000L, trip.movingMillis)
    }
    @Test fun gpsDoesNotAcceptTimeReversalOrNonFinitePositions() {
        val trip = TripAccumulator(); trip.add(TrackPoint(0.0, 0.0, 1000, 5f))
        assertFalse(trip.add(TrackPoint(0.0, .0001, 900, 5f)))
        assertFalse(trip.add(TrackPoint(Double.NaN, 0.0, 2000, 5f)))
    }
    @Test fun reminderAdvanceTimeIsExplicit() {
        assertEquals(6400000L, Reminder(1, "Service", 10000000, 3600000).alertAt)
        assertFailsWith<IllegalArgumentException> { Reminder(0, "Service", 10000000) }
        assertFailsWith<IllegalArgumentException> { Reminder(1, "", 10000000) }
    }
}
