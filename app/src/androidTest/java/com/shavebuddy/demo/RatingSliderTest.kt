package com.shavebuddy.demo

import androidx.activity.ComponentActivity
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.shavebuddy.demo.domain.Satisfaction
import com.shavebuddy.demo.domain.ShaveRatings
import com.shavebuddy.demo.ui.RatingDialog
import com.shavebuddy.demo.ui.ShaveTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class RatingSliderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun slider(title: String) = compose.onNodeWithContentDescription(title)
    private fun rate(title: String, value: Float) {
        slider(title).performSemanticsAction(SemanticsActions.SetProgress) { it(value) }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        try {
            File(instrumentation.targetContext.cacheDir, name).outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } finally { bitmap.recycle() }
    }

    @Test fun untouchedSlidersSaveAllFourRatingsAsEmpty() {
        var saved: ShaveRatings? = null
        compose.setContent { ShaveTheme { RatingDialog("empty", false, false, {}, { saved = it }) } }
        listOf("整体感受", "剃净度", "顺滑度", "皮肤状况").forEach { title ->
            slider(title).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "未填写"))
        }
        compose.onNodeWithText("保存感受").performClick()
        compose.runOnIdle { assertEquals(ShaveRatings(), saved) }
    }

    @Test fun draggingAndClearingOneSliderDoesNotFillOtherRatings() {
        var saved: ShaveRatings? = null
        compose.setContent { ShaveTheme { RatingDialog("partial", false, false, {}, { saved = it }) } }
        screenshot("slider-before-drag.png")
        slider("整体感受").performTouchInput { swipe(Offset(width * 0.1f, centerY), Offset(width * 0.9f, centerY)) }
        screenshot("slider-after-drag.png")
        rate("剃净度", 1f)
        rate("顺滑度", 2f)
        compose.onNodeWithContentDescription("清空顺滑度").performClick()
        compose.onNodeWithText("保存感受").performClick()
        compose.runOnIdle {
            assertEquals(ShaveRatings(overall = Satisfaction.SATISFIED, closeness = Satisfaction.DISSATISFIED), saved)
        }
    }

    @Test fun slidingBackToMinimumKeepsTheLowestRating() {
        var saved: ShaveRatings? = null
        compose.setContent { ShaveTheme { RatingDialog("reset", false, false, {}, { saved = it }) } }
        rate("整体感受", 3f)
        rate("皮肤状况", 1f)
        slider("整体感受").performTouchInput { swipe(Offset(width * 0.9f, centerY), Offset(width * 0.1f, centerY)) }
        compose.onNodeWithText("保存感受").performClick()
        compose.runOnIdle { assertEquals(ShaveRatings(overall = Satisfaction.DISSATISFIED, skin = Satisfaction.DISSATISFIED), saved) }
    }

    @Test fun tappingCenterOnUntouchedSliderRecordsNeutral() {
        var saved: ShaveRatings? = null
        compose.setContent { ShaveTheme { RatingDialog("center", false, false, {}, { saved = it }) } }
        slider("顺滑度").performTouchInput { click(center) }
        compose.onNodeWithText("保存感受").performClick()
        compose.runOnIdle { assertEquals(ShaveRatings(smoothness = Satisfaction.NEUTRAL), saved) }
    }

    @Test fun restorationPreservesRatedAndEmptySliders() {
        var saved: ShaveRatings? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent { ShaveTheme { RatingDialog("restore", false, false, {}, { saved = it }) } }
        rate("整体感受", 2f)
        rate("顺滑度", 3f)
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("保存感受").performClick()
        compose.runOnIdle { assertEquals(ShaveRatings(overall = Satisfaction.NEUTRAL, smoothness = Satisfaction.SATISFIED), saved) }
    }

    @Test fun skippingDoesNotSaveEvenAfterDragging() {
        var skipped = false
        var saved: ShaveRatings? = null
        compose.setContent { ShaveTheme { RatingDialog("skip", false, false, { skipped = true }, { saved = it }) } }
        rate("整体感受", 3f)
        compose.onNodeWithText("暂时跳过").performClick()
        compose.runOnIdle { assertTrue(skipped); assertNull(saved) }
    }

    @Test fun busyStateDisablesSlidersAndActions() {
        compose.setContent { ShaveTheme { RatingDialog("busy", true, false, {}, {}) } }
        listOf("整体感受", "剃净度", "顺滑度", "皮肤状况").forEach { slider(it).assertIsNotEnabled() }
        compose.onNodeWithText("正在保存…").assertIsNotEnabled()
        compose.onNodeWithText("暂时跳过").assertIsNotEnabled()
    }
}
