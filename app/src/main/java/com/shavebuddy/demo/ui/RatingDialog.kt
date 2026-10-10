package com.shavebuddy.demo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentSatisfiedAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shavebuddy.demo.R
import com.shavebuddy.demo.domain.Satisfaction
import com.shavebuddy.demo.domain.ShaveRatings

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
fun RatingDialog(eventId: String, busy: Boolean, error: Boolean, onSkip: () -> Unit, onSave: (ShaveRatings) -> Unit) {
    var overall by rememberSaveable(eventId) { mutableStateOf<Int?>(null) }
    var closeness by rememberSaveable(eventId) { mutableStateOf<Int?>(null) }
    var smoothness by rememberSaveable(eventId) { mutableStateOf<Int?>(null) }
    var skin by rememberSaveable(eventId) { mutableStateOf<Int?>(null) }
    Dialog(onDismissRequest = { if (!busy) onSkip() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.padding(16.dp).widthIn(max = 560.dp).fillMaxWidth().heightIn(max = 720.dp),
            shape = RoundedCornerShape(28.dp), tonalElevation = 6.dp) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.rating_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.rating_hint), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    RatingRow(stringResource(R.string.rating_overall), overall, busy) { overall = it }
                    RatingRow(stringResource(R.string.rating_closeness), closeness, busy) { closeness = it }
                    RatingRow(stringResource(R.string.rating_smoothness), smoothness, busy) { smoothness = it }
                    RatingRow(stringResource(R.string.rating_skin), skin, busy) { skin = it }
                    if (error) Text(stringResource(R.string.rating_error), color = MaterialTheme.colorScheme.error)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onSkip, enabled = !busy, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.rating_skip_all))
                    }
                    Button(onClick = { onSave(ShaveRatings(Satisfaction.fromCode(overall), Satisfaction.fromCode(closeness),
                        Satisfaction.fromCode(smoothness), Satisfaction.fromCode(skin))) }, enabled = !busy,
                        modifier = Modifier.weight(1f)) {
                        Text(stringResource(if (busy) R.string.saving else R.string.rating_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingRow(title: String, value: Int?, busy: Boolean, onChange: (Int?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            if (value == null) Text(stringResource(R.string.rating_unrated), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            else TextButton(onClick = { onChange(null) }, enabled = !busy,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.semantics { contentDescription = "$title，跳过" }) {
                Text(stringResource(R.string.rating_skip))
            }
        }
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Satisfaction.entries.forEach { rating ->
                val selected = value == rating.code
                val label = satisfactionLabel(rating)
                Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) {
                    Column(Modifier.fillMaxWidth().heightIn(min = 76.dp)
                        .selectable(selected, enabled = !busy, role = Role.RadioButton) { onChange(if (selected) null else rating.code) }
                        .semantics { contentDescription = "$title，$label" }.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SatisfactionIcon(rating)
                        Text(label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
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
