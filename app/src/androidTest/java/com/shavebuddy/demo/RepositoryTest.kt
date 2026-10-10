package com.shavebuddy.demo

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.shavebuddy.demo.data.ShaveDatabase
import com.shavebuddy.demo.data.ShaveRepository
import com.shavebuddy.demo.domain.ShaveRules
import com.shavebuddy.demo.domain.ShaveRatings
import com.shavebuddy.demo.domain.Satisfaction
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = Clock.fixed(Instant.parse("2026-10-03T04:00:00Z"), ZoneId.of("Asia/Shanghai"))
    private val today = LocalDate.parse("2026-10-03")

    @Test fun versionOneUpgradePreservesHistoryAndLeavesRatingsEmpty() = runBlocking {
        val name = "migration-${System.nanoTime()}.db"
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.shavebuddy.demo.data.ShaveDatabase/1.json").bufferedReader().use { it.readText() })
            .getJSONObject("database")
        context.getDatabasePath(name).parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices")
                if (indices != null) for (j in 0 until indices.length()) {
                    old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
            old.execSQL("INSERT INTO equipment VALUES (1, '旧剃须刀', 2)")
            old.execSQL("INSERT INTO cycles VALUES ('old', '2026-10-01', NULL, 10, 30, 1, 1)")
            old.execSQL("INSERT INTO events VALUES ('old-event', 'old', '2026-10-02', 123, 'Asia/Shanghai')")
            old.version = 1
        }
        val db = ShaveDatabase.open(context, name)
        try {
            val snapshot = ShaveRepository(db, clock).read()
            assertEquals("旧剃须刀", snapshot.equipment?.name)
            assertEquals("old-event", snapshot.events.single().id)
            assertEquals("old", snapshot.events.single().cycleId)
            assertEquals(123L, snapshot.events.single().occurredAt)
            assertEquals(1, ShaveRules.summarize(snapshot, today).uses)
            assertEquals(2, db.openHelper.writableDatabase.version)
            db.openHelper.writableDatabase.query("SELECT overallRating, closenessRating, smoothnessRating, skinRating FROM events").use {
                assertTrue(it.moveToFirst())
                for (column in 0..3) assertTrue(it.isNull(column))
            }
        } finally { db.close(); context.deleteDatabase(name) }
    }

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

    @Test fun partialRatingsStayWithTheirEventAcrossReplacementAndReopen() = runBlocking {
        val name = "ratings-${System.nanoTime()}.db"
        var db = ShaveDatabase.open(context, name)
        try {
            var repository = ShaveRepository(db, clock)
            repository.setup("剃须刀", today.minusDays(3), 10, 30, 2)
            val first = repository.addEvent(today.minusDays(1))
            val skipped = repository.addEvent(today.minusDays(1))
            repository.rateEvent(first, ShaveRatings(overall = Satisfaction.NEUTRAL, closeness = Satisfaction.SATISFIED,
                skin = Satisfaction.DISSATISFIED))
            repository.replaceBlade()
            db.close()
            db = ShaveDatabase.open(context, name)
            repository = ShaveRepository(db, clock)
            val snapshot = repository.read()
            val rated = snapshot.events.first { it.id == first }
            assertEquals(Satisfaction.NEUTRAL, rated.ratings.overall)
            assertEquals(Satisfaction.SATISFIED, rated.ratings.closeness)
            assertNull(rated.ratings.smoothness)
            assertEquals(Satisfaction.DISSATISFIED, rated.ratings.skin)
            assertEquals(ShaveRatings(), snapshot.events.first { it.id == skipped }.ratings)
            assertEquals(0, ShaveRules.summarize(snapshot, today).uses)
            repository.deleteEvent(first)
            assertEquals(skipped, repository.read().events.single().id)
            try { repository.rateEvent(first, ShaveRatings(overall = Satisfaction.SATISFIED)); fail("rating nonexistent event accepted") }
            catch (_: IllegalStateException) { }
            assertEquals(ShaveRatings(), repository.read().events.single().ratings)
        } finally { db.close(); context.deleteDatabase(name) }
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

    @Test fun confirmedEarlyBackfillUsesFirstCycleAndPreservesExistingHistory() = runBlocking {
        val name = "early-${System.nanoTime()}.db"
        val db = ShaveDatabase.open(context, name)
        try {
            val repository = ShaveRepository(db, clock)
            repository.setup("手动剃须刀", today, 10, 30, 2)
            repository.addEvent(today)
            val original = repository.read()
            repository.replaceBlade()
            repository.addEvent(today.minusDays(2), adjustFirstInstallation = true)
            val data = repository.read()
            assertEquals(today.minusDays(2), data.cycles.first { it.id == original.cycles.single().id }.installedOn)
            assertEquals(today, data.cycles.first { it.retiredOn == null }.installedOn)
            assertEquals(original.cycles.single().id, data.events.first { it.localDate == today.minusDays(2) }.cycleId)
            assertEquals(original.events.single(), data.events.first { it.id == original.events.single().id })
            assertEquals(0, ShaveRules.summarize(data, today).uses)
            try { repository.addEvent(today.plusDays(1), adjustFirstInstallation = true); fail("future date accepted") }
            catch (_: IllegalArgumentException) { }
            assertEquals(data, repository.read())
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun failedEarlyBackfillRollsBackInstallationAdjustment() = runBlocking {
        val name = "early-rollback-${System.nanoTime()}.db"
        val db = ShaveDatabase.open(context, name)
        try {
            val repository = ShaveRepository(db, clock)
            repository.setup("手动剃须刀", today, 10, 30, 2)
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_event BEFORE INSERT ON events BEGIN SELECT RAISE(ABORT, 'test write failure'); END")
            try { repository.addEvent(today.minusDays(2), adjustFirstInstallation = true); fail("write should fail") }
            catch (_: android.database.sqlite.SQLiteException) { }
            val data = repository.read()
            assertEquals(today, data.cycles.single().installedOn)
            assertTrue(data.events.isEmpty())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
