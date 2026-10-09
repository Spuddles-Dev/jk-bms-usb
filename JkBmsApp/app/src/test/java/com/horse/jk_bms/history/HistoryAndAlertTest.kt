package com.horse.jk_bms.history

import com.horse.jk_bms.connection.telemetryAge
import com.horse.jk_bms.monitoring.AlertDeduplicator
import com.horse.jk_bms.viewmodel.parseConfigNumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryAndAlertTest {
    @Test fun testMissingCellReadingCreatesGapEvenWithinOneSecond() {
        val sampler = ExtremaSampler(0, 1000)
        sampler.add(100, 3f)
        sampler.add(200, Float.NaN)
        sampler.add(300, 3.1f)
        assertNotEquals(sampler.samples().first().segment, sampler.samples().last().segment)
    }
    @Test fun testExtremaSurviveDownsamplingAndGapsAreSeparate() {
        val sampler = ExtremaSampler(0, 1_000_000, 10)
        for (timestamp in 0L..100_000L step 250) sampler.add(timestamp, if (timestamp == 50_000L) 9f else 3f)
        sampler.add(200_000, 1f)
        val samples = sampler.samples()
        assertTrue(samples.any { it.value == 9f })
        assertTrue(samples.size <= 40)
        assertNotEquals(samples.first().segment, samples.last().segment)
    }

    @Test fun testAlertTransitionsAreDeduplicatedAndCooledDown() {
        val alerts = AlertDeduplicator(1000)
        assertEquals(setOf("stale"), alerts.update(setOf("stale"), 0))
        assertTrue(alerts.update(setOf("stale"), 2000).isEmpty())
        alerts.update(emptySet(), 2100)
        assertEquals(setOf("stale"), alerts.update(setOf("stale"), 2200))
        alerts.update(emptySet(), 2300)
        assertTrue(alerts.update(setOf("stale"), 2400).isEmpty())
    }

    @Test fun testTextParsingRejectsIncompleteAndNonFiniteValues() {
        assertNull(parseConfigNumber(""))
        assertNull(parseConfigNumber("-"))
        assertNull(parseConfigNumber("NaN"))
        assertNull(parseConfigNumber("1,2.3"))
        assertEquals(3.3, parseConfigNumber("3,3")!!, 0.00001)
    }

    @Test fun testAgeIncreasesWithoutNewTelemetryAndIgnoresWallClock() {
        assertEquals(2500L, telemetryAge(3500, 1000))
        assertEquals(0L, telemetryAge(900, 1000))
        assertEquals(Long.MAX_VALUE, telemetryAge(1000, 0))
    }
}
