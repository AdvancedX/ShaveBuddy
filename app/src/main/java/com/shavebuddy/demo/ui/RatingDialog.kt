package com.shavebuddy.demo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentSatisfiedAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import com.shavebuddy.demo.R
import com.shavebuddy.demo.domain.Satisfaction
import com.shavebuddy.demo.domain.ShaveRatings
import kotlin.math.roundToInt

@Composable
fun satisfactionLabel(rating: Satisfaction?): String = stringResource(when (rating) {
    Satisfaction.DISSATISFIED -> R.string.rating_dissatisfied
    Satisfaction.NEUTRAL -> R.string.rating_neutral
    Satisfaction.SATISFIED -> R.string.rating_satisfied
    null -> R.string.rating_unrated
})

@Composable
fun SatisfactionIcon(rating: Satisfaction, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    Icon(when (rating) {
        Satisfaction.DISSATISFIED -> Icons.Outlined.SentimentDissatisfied
        Satisfaction.NEUTRAL -> Icons.Outlined.SentimentNeutral
        Satisfaction.SATISFIED -> Icons.Outlined.SentimentSatisfiedAlt
    }, contentDescription = null, modifier = modifier.size(size))
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RatingDialog(eventId: String, busy: Boolean, error: Boolean, onSkip: () -> Unit, onSave: (ShaveRatings) -> Unit) {
    var overall by rememberSaveable(eventId) { mutableStateOf<Int?>(null) }
    var closeness by rememberSaveable(eventId) { mutableStateOf<Int?>(null) }
    var smoothness by rememberSaveable(eventId) { mutableStateOf<Int?>(null) }
    var skin by rememberSaveable(eventId) { mutableStateOf<Int?>(null) }
    val currentBusy by rememberUpdatedState(busy)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || !currentBusy })
    ModalBottomSheet(
        onDismissRequest = { if (!busy) onSkip() },
        sheetState = sheetState,
        sheetGesturesEnabled = !busy,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = !busy),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Text(stringResource(R.string.rating_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.rating_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        RatingSlider(RatingAspect.OVERALL, overall, busy, prominent = true) { overall = it }
                    }
                }
                Column(Modifier.padding(horizontal = 8.dp)) {
                    RatingSlider(RatingAspect.CLOSENESS, closeness, busy) { closeness = it }
                    RatingSlider(RatingAspect.SMOOTHNESS, smoothness, busy) { smoothness = it }
                    RatingSlider(RatingAspect.SKIN, skin, busy) { skin = it }
                }
                if (error) Text(stringResource(R.string.rating_error), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = { onSave(ShaveRatings(Satisfaction.fromCode(overall), Satisfaction.fromCode(closeness),
                Satisfaction.fromCode(smoothness), Satisfaction.fromCode(skin))) }, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(18.dp)) {
                Text(stringResource(if (busy) R.string.saving else R.string.rating_save))
            }
            TextButton(onClick = onSkip, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.rating_skip_all), color = if (busy) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private enum class RatingAspect(val title: Int, val low: Int, val high: Int) {
    OVERALL(R.string.rating_input_overall, R.string.rating_overall_low, R.string.rating_overall_high),
    CLOSENESS(R.string.rating_closeness, R.string.rating_closeness_low, R.string.rating_closeness_high),
    SMOOTHNESS(R.string.rating_smoothness, R.string.rating_smoothness_low, R.string.rating_smoothness_high),
    SKIN(R.string.rating_skin, R.string.rating_skin_low, R.string.rating_skin_high),
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RatingSlider(aspect: RatingAspect, value: Int?, busy: Boolean, prominent: Boolean = false, onChange: (Int?) -> Unit) {
    val title = stringResource(aspect.title)
    val lowLabel = stringResource(aspect.low)
    val highLabel = stringResource(aspect.high)
    val colors = MaterialTheme.colorScheme
    val accent = if (busy) colors.onSurface.copy(alpha = 0.38f) else colors.primary
    val trackColor = colors.outlineVariant.copy(alpha = 0.6f)
    val thumbSize = if (prominent) 28.dp else 18.dp
    val trackHeight = if (prominent) 10.dp else 4.dp
    val description = when (value) {
        1 -> lowLabel
        2 -> stringResource(R.string.rating_okay)
        3 -> highLabel
        else -> stringResource(R.string.rating_unfilled)
    }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // A first tap on the neutral stop must record a rating even when Slider's value is unchanged.
    var gestureValue = value ?: 2
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = if (prominent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                color = if (prominent) colors.onSurface else colors.onSurfaceVariant)
            Surface(color = if (value == null) colors.surfaceContainerHigh else colors.primaryContainer,
                shape = RoundedCornerShape(50)) {
                Text(description, Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (value == null) colors.onSurfaceVariant else colors.onPrimaryContainer)
            }
            if (value != null) {
                IconButton(onClick = { onChange(null) }, enabled = !busy) {
                    Icon(Icons.Outlined.Close, stringResource(R.string.rating_clear, title),
                        modifier = Modifier.size(16.dp), tint = colors.onSurfaceVariant)
                }
            }
        }
        Slider(value = value?.toFloat() ?: 2f,
            onValueChange = {
                gestureValue = it.roundToInt().coerceIn(1, 3)
                onChange(gestureValue)
            },
            onValueChangeFinished = { onChange(gestureValue) },
            valueRange = 1f..3f, steps = 1, enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                contentDescription = title
                stateDescription = description
                setProgress { target ->
                    if (busy || !target.isFinite()) false else {
                        val selected = target.coerceIn(1f, 3f).roundToInt()
                        if (selected == value) false else { onChange(selected); true }
                    }
                }
            },
            thumb = {
                Box(Modifier.size(thumbSize).alpha(if (value == null) 0f else 1f)
                    .shadow(2.dp, CircleShape)
                    .background(accent, CircleShape)
                    .border(3.dp, colors.surface, CircleShape))
            },
            track = { state ->
                Canvas(Modifier.fillMaxWidth().height(trackHeight)) {
                    val start = if (isRtl) size.width else 0f
                    val end = if (isRtl) 0f else size.width
                    val centerY = size.height / 2
                    drawLine(trackColor, Offset(start, centerY), Offset(end, centerY), strokeWidth = size.height, cap = StrokeCap.Round)
                    if (value != null) {
                        val progress = (state.value - 1f) / 2f
                        drawLine(accent, Offset(start, centerY), Offset(start + (end - start) * progress, centerY),
                            strokeWidth = size.height, cap = StrokeCap.Round)
                    }
                    for (step in 0..2) {
                        drawCircle(if (value != null && step < value) colors.surface.copy(alpha = 0.5f) else colors.outline.copy(alpha = 0.35f),
                            radius = 1.5.dp.toPx(), center = Offset(start + (end - start) * step / 2f, centerY))
                    }
                }
            })
        Row(Modifier.fillMaxWidth().padding(bottom = if (prominent) 8.dp else 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(lowLabel, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Text(highLabel, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
fun RatingDetails(ratings: ShaveRatings) {
    listOf(R.string.rating_overall to ratings.overall, R.string.rating_closeness to ratings.closeness,
        R.string.rating_smoothness to ratings.smoothness, R.string.rating_skin to ratings.skin).forEach { (title, rating) ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (rating != null) SatisfactionIcon(rating, size = 20.dp)
            Text(stringResource(R.string.rating_detail, stringResource(title), satisfactionLabel(rating)),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
