package com.shavebuddy.demo

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.shavebuddy.demo.ui.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DateHeaderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun saturdayIsAWholeUnitAtPhotoWidth() = checkHeader(280, 1f)
    @Test fun dateAndSaturdayAreWholeUnitsAtLargeFontAndNarrowWidth() = checkHeader(240, 2f)
    private fun checkHeader(width: Int, scale: Float) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                ShaveTheme { Box(Modifier.width(width.dp)) { IllustratedDateHeader(LocalDate.of(2026, 10, 10)) {} } }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onAllNodes(hasText("星期六", substring = true) or hasText("10月10日", substring = true), useUnmergedTree = true).fetchSemanticsNodes().forEach { node ->
            node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        }
        assertTrue("weekday must be rendered", layouts.isNotEmpty())
        val lines = layouts.flatMap { layout -> (0 until layout.lineCount).map {
            layout.layoutInput.text.text.substring(layout.getLineStart(it), layout.getLineEnd(it))
        } }
        assertTrue("weekday was broken: $lines", lines.any { it.contains("星期六") })
        assertTrue("date was broken: $lines", lines.any { it.contains("10月10日") })
        assertTrue("overflow: " + layouts.map { "${it.layoutInput.text}: size=${it.size}, paragraph=${it.multiParagraph.width}x${it.multiParagraph.height}, width=${it.didOverflowWidth}, height=${it.didOverflowHeight}" }, layouts.none { it.hasVisualOverflow })
        compose.onNodeWithContentDescription("10月10日 星期六").assertExists()
    }
}
