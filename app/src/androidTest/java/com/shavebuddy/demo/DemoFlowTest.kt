package com.shavebuddy.demo

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.core.app.ApplicationProvider
import com.shavebuddy.demo.data.ShaveDatabase
import com.shavebuddy.demo.data.ShaveRepository
import com.shavebuddy.demo.ui.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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

    @Test fun setupRecordCalendarAndReplaceKeepHistory() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("开始记录").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("开始记录").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("记录剃须").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("记录剃须").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("今天已记录 1 次").fetchSemanticsNodes().isNotEmpty() }
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
        compose.onNodeWithText("还没有记录").assertExists()
        compose.onNodeWithText("装备").performClick()
        compose.onNodeWithText("调整装备与目标").performClick()
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
        runBlocking {
            val data = repository.read()
            assertEquals(date, data.events.single().localDate)
            assertEquals(date, data.cycles.single().installedOn)
            assertEquals(data.cycles.single().id, data.events.single().cycleId)
        }
    }
}
