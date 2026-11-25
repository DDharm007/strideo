package com.example

import com.example.data.model.ActivityRecord
import com.example.data.model.ActivityType
import com.example.data.model.DistanceUnit
import com.example.data.model.HaversineMath
import com.example.data.model.RoutePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun haversineDistance_computesSanFranciscoSegmentAccurately() {
        // ~1.11 km north-south (0.01 deg latitude)
        val dist = HaversineMath.distanceMeters(37.7749, -122.4194, 37.7849, -122.4194)
        assertTrue("Expected ~1111m, got $dist", dist in 1090.0..1135.0)
    }

    @Test
    fun routeDownsampling_capsPointsUnderFirestoreLimit() {
        val raw = (0 until 1200).map { i ->
            RoutePoint(lat = 37.77 + i * 0.00001, lng = -122.41, alt = 10.0, acc = 5.0, ts = i * 1000L)
        }
        val sampled = HaversineMath.downsampleRoute(raw, maxPoints = 450)
        assertEquals(450, sampled.size)
        assertEquals(raw.first(), sampled.first())
        assertEquals(raw.last(), sampled.last())
    }

    @Test
    fun paceAndBmiFormatting_matchExpectedPrecision() {
        assertEquals("5:00 /km", HaversineMath.formatPace(300.0, DistanceUnit.KM))
        val bmi = HaversineMath.calculateBmi(weightKg = 70.0, heightCm = 175.0)
        assertNotNull(bmi)
        assertTrue(bmi!! in 22.8..22.9)
    }

    @Test
    fun streaksAndCsvExport_deriveFromStoredRecords() {
        val now = System.currentTimeMillis()
        val act = ActivityRecord(
            id = "a1",
            userId = "u1",
            title = "Morning Run",
            type = ActivityType.RUNNING.name,
            startedAtEpochMs = now,
            durationSec = 1800,
            movingDurationSec = 1800,
            distanceMeters = 5000.0,
            avgSpeedMps = 2.77,
            avgPaceSecPerKm = 360.0,
            caloriesBurned = 350
        )
        val streaks = HaversineMath.computeStreaks(listOf(act), emptyList())
        assertEquals(1, streaks.currentStreakDays)
        val csv = HaversineMath.exportToCsv(listOf(act), emptyList())
        assertTrue(csv.contains("Morning Run"))
    }
}
