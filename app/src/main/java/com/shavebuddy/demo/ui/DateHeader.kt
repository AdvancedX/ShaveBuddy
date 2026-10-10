package com.shavebuddy.demo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.shavebuddy.demo.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Date and weekday are measured as whole units; the illustration yields before text does. */
@Composable
fun IllustratedDateHeader(date: LocalDate, onSettings: (() -> Unit)? = null) {
    val day = date.format(DateTimeFormatter.ofPattern("M月d日", Locale.SIMPLIFIED_CHINESE))
    val weekday = date.format(DateTimeFormatter.ofPattern("EEEE", Locale.SIMPLIFIED_CHINESE))
    val textMeasurer = rememberTextMeasurer()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        BoxWithConstraints(Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = formatDate(date) }) {
            var style = MaterialTheme.typography.headlineLarge
            // At extreme font sizes, fit one complete unit to the full width, without clipping.
            while (maxOf(textMeasurer.measure(day, style, softWrap = false).size.width,
                    textMeasurer.measure(weekday, style, softWrap = false).size.width) > constraints.maxWidth && style.fontSize.value > 1f) {
                style = style.copy(fontSize = style.fontSize * 0.95f)
            }
            val dateWidth = textMeasurer.measure(day, style, softWrap = false).size.width
            val weekdayWidth = textMeasurer.measure(weekday, style, softWrap = false).size.width
            Layout(content = {
                Text(day, style = style, maxLines = 1)
                Text(weekday, style = style, maxLines = 1)
                Image(painterResource(R.drawable.razor_illustration), contentDescription = null, modifier = Modifier.size(88.dp))
            }) { measurables, bounds ->
                val gap = 8.dp.roundToPx()
                val rowGap = 4.dp.roundToPx()
                val loose = Constraints(maxWidth = bounds.maxWidth)
                val datePart = measurables[0].measure(Constraints.fixedWidth(dateWidth.coerceAtMost(bounds.maxWidth)))
                val weekPart = measurables[1].measure(Constraints.fixedWidth(weekdayWidth.coerceAtMost(bounds.maxWidth)))
                val illustration = measurables[2].measure(loose)
                val beside = maxOf(datePart.width, weekPart.width) + gap + illustration.width <= bounds.maxWidth
                val textWidth = if (beside) bounds.maxWidth - illustration.width - gap else bounds.maxWidth
                val singleRow = datePart.width + gap + weekPart.width <= textWidth
                val textHeight = if (singleRow) maxOf(datePart.height, weekPart.height) else datePart.height + rowGap + weekPart.height
                val height = if (beside) maxOf(textHeight, illustration.height) else textHeight + gap + illustration.height
                layout(bounds.maxWidth, height) {
                    datePart.placeRelative(0, 0)
                    weekPart.placeRelative(if (singleRow) datePart.width + gap else 0, if (singleRow) 0 else datePart.height + rowGap)
                    illustration.placeRelative(if (beside) bounds.maxWidth - illustration.width else 0,
                        if (beside) (height - illustration.height) / 2 else textHeight + gap)
                }
            }
        }
        onSettings?.let { SettingsAction(it) }
    }
}
