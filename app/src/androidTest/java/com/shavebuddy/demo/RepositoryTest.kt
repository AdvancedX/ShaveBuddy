package com.shavebuddy.demo

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.shavebuddy.demo.data.ShaveDatabase
import com.shavebuddy.demo.data.ShaveRepository
import com.shavebuddy.demo.domain.ShaveRules
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = Clock.fixed(Instant.parse("2026-10-03T04:00:00Z"), ZoneId.of("Asia/Shanghai"))
    private val today = LocalDate.parse("2026-10-03")

    @Test fun historySurvivesReplacementDeletionAndDatabaseReopen() = runBlocking {
        val name = "repository-${System.nanoTime()}.db"
        var db = ShaveDatabase.open(context, name)
        try {
            var repository = ShaveRepository(db, clock)
            repository.setup("我的剃须刀", today.minusDays(3), 10, 30, 2)
            repository.addEvent(today.minusDays(1))
            repository.addEvent(today.minusDays(1))
            assertEquals(2, ShaveRules.summarize(repository.read(), today).uses)
            repository.replaceBlade()
            val replaced = repository.read()
            assertEquals(0, ShaveRules.summarize(replaced, today).uses)
            assertEquals(2, replaced.events.size)
            assertEquals(1, replaced.cycles.count { it.retiredOn != null })
            repository.addEvent(today.minusDays(1))
            repository.addEvent(today)
            assertEquals(1, ShaveRules.summarize(repository.read(), today).uses)
            repository.deleteEvent(replaced.events.first().id)
            db.close()
            db = ShaveDatabase.open(context, name)
            repository = ShaveRepository(db, clock)
            val reopened = repository.read()
            assertEquals("我的剃须刀", reopened.equipment?.name)
            assertEquals(3, reopened.events.size)
            assertEquals(1, ShaveRules.summarize(reopened, today).uses)
            assertEquals(today.plusDays(2), ShaveRules.summarize(reopened, today).nextDate)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun invalidWritesDoNotLeavePartialState() = runBlocking {
        val name = "invalid-${System.nanoTime()}.db"
        val db = ShaveDatabase.open(context, name)
        try {
            val repository = ShaveRepository(db, clock)
            try { repository.setup("", today, 10, 30, 2); fail("invalid setup accepted") }
            catch (_: IllegalArgumentException) { }
            assertNull(repository.read().equipment)
            repository.setup("手动剃须刀", today, null, null, 2)
            for (date in listOf(today.plusDays(1), today.minusDays(1))) {
                try { repository.addEvent(date); fail("invalid date accepted") }
                catch (_: IllegalArgumentException) { }
            }
            assertTrue(repository.read().events.isEmpty())
            try { repository.setup("重复装备", today, 10, 30, 2); fail("duplicate setup accepted") }
            catch (_: IllegalArgumentException) { }
            assertEquals(1, repository.read().cycles.size)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun settingsAndSameDayReplacementsKeepExactlyOneCurrentCycle() = runBlocking {
        val name = "settings-${System.nanoTime()}.db"
        val db = ShaveDatabase.open(context, name)
        try {
            val repository = ShaveRepository(db, clock)
            repository.setup("手动剃须刀", today, 10, null, 2)
            repository.addEvent(today)
            repository.updateSettings("旅行剃须刀", 1, null, 3)
            val updated = repository.read()
            assertEquals("旅行剃须刀", updated.equipment?.name)
            assertTrue(ShaveRules.summarize(updated, today).replacementSuggested)
            assertEquals(today.plusDays(3), ShaveRules.summarize(updated, today).nextDate)
            repository.replaceBlade()
            repository.replaceBlade()
            repository.addEvent(today)
            val data = repository.read()
            assertEquals(1, data.cycles.count { it.retiredOn == null })
            assertEquals(3, data.cycles.size)
            assertEquals(1, ShaveRules.summarize(data, today).uses)
            assertEquals(updated.events.first().cycleId, data.events.first { it.id == updated.events.first().id }.cycleId)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
