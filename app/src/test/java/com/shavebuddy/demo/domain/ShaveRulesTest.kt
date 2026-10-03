package com.shavebuddy.demo.domain

import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class ShaveRulesTest {
    private val today = LocalDate.parse("2024-03-01")
    private val current = Cycle("new", LocalDate.parse("2024-02-28"), targetUses = 2, targetDays = 30)
    private fun event(id: String, date: String = "2024-02-28", cycle: String = "new") =
        ShaveEvent(id, cycle, LocalDate.parse(date), id.hashCode().toLong(), "Asia/Shanghai")
    private fun snapshot(events: List<ShaveEvent>) = ShaveSnapshot(Equipment("手动剃须刀", 2), listOf(current), events)

    @Test fun sameDayEventsCountSeparatelyAndReachUsesTarget() {
        val result = ShaveRules.summarize(snapshot(listOf(event("1"), event("2"))), today)
        assertEquals(2, result.uses)
        assertEquals(2L, result.days)
        assertTrue(result.replacementSuggested)
        assertEquals(LocalDate.parse("2024-03-01"), result.nextDate)
    }
    @Test fun oldCycleDoesNotContributeToCurrentUsage() {
        val result = ShaveRules.summarize(snapshot(listOf(event("1", cycle = "old"))), today)
        assertEquals(0, result.uses)
        assertEquals(LocalDate.parse("2024-02-28"), result.lastDate)
    }
    @Test fun noEventsDoesNotInventNextDate() {
        val result = ShaveRules.summarize(snapshot(emptyList()), today)
        assertNull(result.lastDate)
        assertNull(result.nextDate)
    }
    @Test fun dayThresholdIsIndependentAndCanBeDisabled() {
        val data = snapshot(emptyList()).copy(cycles = listOf(current.copy(targetUses = null, targetDays = 2)))
        assertTrue(ShaveRules.summarize(data, today).replacementSuggested)
        assertFalse(ShaveRules.summarize(data.copy(cycles = listOf(current.copy(targetUses = null, targetDays = null))), today).replacementSuggested)
    }
    @Test fun deletingEventRecalculatesUsageAndNextDate() {
        val data = snapshot(listOf(event("1"), event("2", "2024-03-01")))
        assertEquals(LocalDate.parse("2024-03-03"), ShaveRules.summarize(data, today).nextDate)
        val result = ShaveRules.summarize(data.copy(events = data.events.dropLast(1)), today)
        assertEquals(1, result.uses)
        assertFalse(result.replacementSuggested)
        assertEquals(LocalDate.parse("2024-03-01"), result.nextDate)
    }
    @Test fun datesUseCalendarDaysAcrossDstAndMonthEnd() {
        val zone = ZoneId.of("America/New_York")
        val start = Instant.parse("2024-03-09T17:00:00Z").atZone(zone).toLocalDate()
        val end = Instant.parse("2024-03-11T16:00:00Z").atZone(zone).toLocalDate()
        assertEquals(2L, ShaveRules.summarize(snapshot(emptyList()).copy(cycles = listOf(current.copy(installedOn = start))), end).days)
        assertEquals(LocalDate.parse("2025-02-02"), ShaveRules.summarize(snapshot(listOf(event("1", "2025-01-31"))), LocalDate.parse("2025-01-31")).nextDate)
    }
    @Test fun cycleMatchingUsesInstallationBoundaryAndLatestSameDayCycle() {
        val old = current.copy(id = "old", installedOn = today.minusDays(10), retiredOn = today)
        val fresh = current.copy(installedOn = today, createdAt = 2)
        assertEquals("old", ShaveRules.cycleForDate(listOf(old, fresh), today.minusDays(1))?.id)
        assertEquals("new", ShaveRules.cycleForDate(listOf(old, fresh), today)?.id)
        assertNull(ShaveRules.cycleForDate(listOf(old, fresh), today.minusDays(11)))
    }
    @Test fun settingsRejectBlankAndNonPositiveValues() {
        assertTrue(ShaveRules.validSettings("刀片", 2, null, null))
        assertFalse(ShaveRules.validSettings("  ", 2, 10, 30))
        assertFalse(ShaveRules.validSettings("刀片", 0, 10, 30))
        assertFalse(ShaveRules.validSettings("刀片", 2, 0, 30))
        assertFalse(ShaveRules.validSettings("刀片", 2, 10, -1))
    }
}
