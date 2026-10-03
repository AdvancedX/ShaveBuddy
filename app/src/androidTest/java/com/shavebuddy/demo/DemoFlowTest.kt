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
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DemoFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var database: ShaveDatabase
    private lateinit var databaseName: String
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before fun isolatedDatabase() {
        databaseName = "ui-${System.nanoTime()}.db"
        database = ShaveDatabase.open(context, databaseName)
        val repository = ShaveRepository(database)
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
}
