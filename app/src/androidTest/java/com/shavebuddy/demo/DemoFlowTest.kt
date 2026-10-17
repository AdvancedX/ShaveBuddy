package com.shavebuddy.demo

import android.content.Context
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.shavebuddy.demo.data.ShaveDatabase
import com.shavebuddy.demo.data.ShaveRepository
import com.shavebuddy.demo.ui.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DemoFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var database: ShaveDatabase
    private lateinit var databaseName: String
    private lateinit var repository: ShaveRepository
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before fun isolatedDatabase() {
        databaseName = "ui-${System.nanoTime()}.db"
        database = ShaveDatabase.open(context, databaseName)
        repository = ShaveRepository(database)
        compose.setContent {
            val model: ShaveViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = ShaveViewModel(repository) as T
            })
            ShaveTheme { ShaveApp(model) }
        }
    }

    @After fun cleanup() {
        compose.runOnUiThread { compose.activity.viewModelStore.clear() }
        database.close()
        context.deleteDatabase(databaseName)
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        try { File(context.cacheDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        finally { bitmap.recycle() }
    }

    private fun rate(title: String, value: Float) {
        compose.onNodeWithContentDescription(title).performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(value) }
    }

    @Test fun optionalRatingsAppearAfterRecordingAndInCalendar() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("开始记录").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("开始记录").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("记录剃须").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("记录剃须").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("今天已记录 1 次").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("这次剃得怎么样？").assertExists()
        rate("整体感受", 3f)
        rate("剃净度", 2f)
        rate("顺滑度", 1f)
        compose.onNodeWithContentDescription("清空顺滑度").performClick()
        rate("顺滑度", 1f)
        rate("皮肤状况", 3f)
        compose.onNodeWithContentDescription("清空皮肤状况").performClick()
        screenshot("rating-panel.png")
        compose.onNodeWithText("保存感受").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("这次剃得怎么样？").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("日历").performClick()
        compose.onNodeWithText("总体评分：满意").assertExists()
        compose.onNodeWithText("剃净度：一般").assertExists()
        compose.onNodeWithText("顺滑度：不满意").assertExists()
        compose.onNodeWithText("皮肤状况：未评分").assertExists()
        compose.onNodeWithContentDescription("${com.shavebuddy.demo.ui.formatDate(LocalDate.now())}，1 条剃须记录，总体评分：满意").assertExists()
        screenshot("rating-calendar.png")
        // A later skipped rating must not erase the day's last rated shave.
        compose.onNodeWithText("记录这一天").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("这次剃得怎么样？").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("暂时跳过").performClick()
        compose.onNodeWithContentDescription("${com.shavebuddy.demo.ui.formatDate(LocalDate.now())}，2 条剃须记录，总体评分：满意").assertExists()
        compose.onNodeWithText("记录这一天").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("这次剃得怎么样？").fetchSemanticsNodes().isNotEmpty() }
        rate("整体感受", 1f)
        compose.onNodeWithText("保存感受").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("这次剃得怎么样？").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithContentDescription("${com.shavebuddy.demo.ui.formatDate(LocalDate.now())}，3 条剃须记录，总体评分：不满意").assertExists()
        runBlocking {
            val events = repository.read().events.sortedBy { it.occurredAt }
            assertEquals(3, events.size)
            assertEquals(com.shavebuddy.demo.domain.Satisfaction.SATISFIED, events.first().ratings.overall)
            assertNull(events[1].ratings.overall)
            assertEquals(com.shavebuddy.demo.domain.Satisfaction.DISSATISFIED, events.last().ratings.overall)
        }
    }

    @Test fun dismissingRatingsKeepsTheShaveAndFailedRatingCanRetryWithoutDuplicate() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("开始记录").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("开始记录").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("记录剃须").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("记录剃须").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("这次剃得怎么样？").fetchSemanticsNodes().isNotEmpty() }
        androidx.test.espresso.Espresso.pressBack()
        runBlocking { assertEquals(1, repository.read().events.size); assertNull(repository.read().events.single().ratings.overall) }
        compose.onNodeWithText("记录剃须").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("这次剃得怎么样？").fetchSemanticsNodes().isNotEmpty() }
        rate("整体感受", 3f)
        database.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_rating BEFORE UPDATE ON events BEGIN SELECT RAISE(ABORT, 'test rating failure'); END")
        compose.onNodeWithText("保存感受").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("评分保存未完成，可重试或跳过。剃须记录已保留。").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("整体感受").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "很满意"))
        runBlocking { assertEquals(2, repository.read().events.size); assertTrue(repository.read().events.all { it.ratings.overall == null }) }
        database.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_rating")
        compose.onNodeWithText("保存感受").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("这次剃得怎么样？").fetchSemanticsNodes().isEmpty() }
        runBlocking { assertEquals(2, repository.read().events.size); assertEquals(1, repository.read().events.count { it.ratings.overall != null }) }
        compose.onNodeWithText("记录剃须").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("这次剃得怎么样？").fetchSemanticsNodes().isNotEmpty() }
        database.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_rating BEFORE UPDATE ON events BEGIN SELECT RAISE(ABORT, 'test rating failure'); END")
        compose.onNodeWithText("保存感受").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("评分保存未完成，可重试或跳过。剃须记录已保留。").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("暂时跳过").performClick()
        compose.onNodeWithText("操作可能未完成。重新读取记录，确认后再试。").assertDoesNotExist()
        runBlocking { assertEquals(3, repository.read().events.size) }
    }

    @Test fun setupRecordCalendarAndReplaceKeepHistory() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("开始记录").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("开始记录").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("记录剃须").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("记录剃须").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("今天已记录 1 次").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("暂时跳过").performClick()
        compose.onNodeWithText("日历").performClick()
        compose.onNodeWithText("删除").assertExists()
        compose.onNodeWithText("装备").performClick()
        compose.onNodeWithText("更换刀片").performScrollTo().performClick()
        compose.onNodeWithText("确认更换").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("已结束 · 1 次剃须").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("今天").performClick()
        compose.onNodeWithText("0 次").assertExists()
        compose.onNodeWithText("日历").performClick()
        compose.onNodeWithText("删除").assertExists()
        compose.onNodeWithText("删除").performClick()
        compose.onNodeWithText("确认删除").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("删除").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("今天").performClick()
        compose.onNodeWithText(context.getString(R.string.no_record)).assertExists()
        compose.onNodeWithText("装备").performClick()
        compose.onNodeWithText(context.getString(R.string.edit_settings)).performScrollTo().performClick()
        compose.onNodeWithText("装备名称").performTextReplacement("旅行剃须刀")
        compose.onNodeWithText("保存设置").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("更换刀片").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("旅行剃须刀").assertExists()
        compose.onNodeWithText("更换刀片").assertExists()
    }

    @Test fun failedWriteCanReloadWithoutSilentlyReplayingMutation() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("开始记录").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("开始记录").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("记录剃须").fetchSemanticsNodes().isNotEmpty() }
        database.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_event BEFORE INSERT ON events BEGIN SELECT RAISE(ABORT, 'test write failure'); END")
        compose.onNodeWithText("记录剃须").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("重新读取").fetchSemanticsNodes().isNotEmpty() }
        database.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_event")
        compose.onNodeWithText("重新读取").performClick()
        compose.onNodeWithText("0 次").assertExists()
        compose.onNodeWithText("记录剃须").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("今天已记录 1 次").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("暂时跳过").performClick()
    }

    @Test fun backfillBeforeDefaultInstallationCanSelectDateAndRequiresConfirmation() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("开始记录").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("开始记录").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("补记其他日期").fetchSemanticsNodes().isNotEmpty() }
        val today = LocalDate.now()
        val previous = today.withDayOfMonth(1)
        // Pick the first of the displayed month (yesterday if today is the first).
        val date = if (previous == today) today.minusDays(1) else previous
        val label = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", context.resources.configuration.locales[0]))
        fun selectPastDate() {
            compose.onNodeWithText("补记其他日期").performScrollTo().performClick()
            if (date.month != today.month) compose.onNodeWithContentDescription("Change to previous month").performClick()
            compose.onNode(hasText(label, substring = true) or hasContentDescription(label, substring = true)).assertIsEnabled().performClick()
            compose.onNodeWithText("选择日期").performClick()
        }
        selectPastDate()
        compose.onNodeWithText("调整安装日期并补记？").assertExists()
        compose.onNodeWithText("取消").performClick()
        runBlocking {
            assertTrue(repository.read().events.isEmpty())
            assertEquals(today, repository.read().cycles.single().installedOn)
        }
        selectPastDate()
        compose.onNodeWithText("调整并补记").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("1 次").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("这次剃得怎么样？").assertExists()
        rate("剃净度", 3f)
        compose.onNodeWithText("保存感受").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("这次剃得怎么样？").fetchSemanticsNodes().isEmpty() }
        runBlocking {
            val data = repository.read()
            assertEquals(date, data.events.single().localDate)
            assertEquals(date, data.cycles.single().installedOn)
            assertEquals(data.cycles.single().id, data.events.single().cycleId)
            assertNull(data.events.single().ratings.overall)
            assertEquals(com.shavebuddy.demo.domain.Satisfaction.SATISFIED, data.events.single().ratings.closeness)
        }
    }
}
