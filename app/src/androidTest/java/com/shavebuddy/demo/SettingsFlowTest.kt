package com.shavebuddy.demo

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
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

class SettingsFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var database: ShaveDatabase
    private lateinit var databaseName: String
    private lateinit var restoration: StateRestorationTester
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Before fun start() {
        databaseName = "settings-${System.nanoTime()}.db"
        database = ShaveDatabase.open(context, databaseName)
        val repository = ShaveRepository(database)
        restoration = StateRestorationTester(compose)
        restoration.setContent {
            val model: ShaveViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = ShaveViewModel(repository) as T
            })
            ShaveTheme { ShaveApp(model) }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("开始记录").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("开始记录").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("记录剃须").fetchSemanticsNodes().isNotEmpty() }
    }
    @After fun cleanup() {
        compose.runOnUiThread { compose.activity.viewModelStore.clear() }; database.close(); context.deleteDatabase(databaseName)
    }
    @Test fun everyTabReturnsFromSettingsToItsOriginWithoutBottomNavigation() {
        listOf("今天" to "当前刀片", "日历" to "回到今天", "装备" to "更换刀片").forEach { (tab, marker) ->
            compose.onNodeWithText(tab).performClick()
            compose.onNodeWithContentDescription("设置").performScrollTo().performClick()
            compose.onNodeWithText("应用更新").assertExists()
            compose.onNodeWithText("检查更新").assertExists()
            compose.onNodeWithText("今天").assertDoesNotExist()
            compose.onNodeWithText("日历").assertDoesNotExist()
            compose.onNodeWithText("装备").assertDoesNotExist()
            compose.onNodeWithContentDescription("返回").performClick()
            compose.onNodeWithText(marker).assertExists()
        }
    }
    @Test fun calendarSelectionAndSettingsRouteSurviveStateRestoration() {
        compose.onNodeWithText("日历").performClick()
        compose.onNodeWithContentDescription("上个月").performClick()
        compose.onNodeWithText("1", useUnmergedTree = true).performClick()
        val monthTitle = compose.onAllNodes(hasText("年", substring = true) and hasText("月", substring = true)).fetchSemanticsNodes().first().config[androidx.compose.ui.semantics.SemanticsProperties.Text].first().text
        compose.onNodeWithContentDescription("设置").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("应用更新").assertExists()
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithText(monthTitle).assertExists()
        compose.onNode(hasText("日历") and isSelectable()).assertIsSelected()
    }
}
