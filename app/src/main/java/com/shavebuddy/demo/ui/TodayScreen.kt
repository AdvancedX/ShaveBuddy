package com.shavebuddy.demo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shavebuddy.demo.R
import com.shavebuddy.demo.domain.*
import java.time.LocalDate

@Composable
fun TodayScreen(snapshot: ShaveSnapshot, today: LocalDate, busy: Boolean, onRecord: (LocalDate) -> Unit) {
    val summary = ShaveRules.summarize(snapshot, today)
    var backfill by remember { mutableStateOf(false) }
    PageColumn {
        Text(formatDate(today), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        SectionTitle(stringResource(R.string.today_title), stringResource(R.string.blade_intro))
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(stringResource(R.string.current_blade), style = MaterialTheme.typography.labelLarge)
                Text(snapshot.equipment?.name.orEmpty(), style = MaterialTheme.typography.titleLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Metric(Modifier.weight(1f), stringResource(R.string.uses_count, summary.uses), stringResource(R.string.used_times), summary.current?.targetUses?.let { stringResource(R.string.uses_target, it) } ?: stringResource(R.string.target_off))
                    Metric(Modifier.weight(1f), stringResource(R.string.days_count, summary.days), stringResource(R.string.installed_days), summary.current?.targetDays?.let { stringResource(R.string.days_target, it) } ?: stringResource(R.string.target_off))
                }
                summary.current?.targetUses?.let { target ->
                    LinearProgressIndicator(progress = { (summary.uses.toFloat() / target).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                }
                summary.current?.let { Text(stringResource(R.string.installed_date, formatDate(it.installedOn)), style = MaterialTheme.typography.bodySmall) }
            }
        }
        if (summary.replacementSuggested) {
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(16.dp)) {
                Text(stringResource(R.string.replace_suggested), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DateMetric(Modifier.weight(1f), stringResource(R.string.last_shave), summary.lastDate?.let(::formatDate) ?: stringResource(R.string.no_record))
            DateMetric(Modifier.weight(1f), stringResource(R.string.next_shave), summary.nextDate?.let(::formatDate) ?: stringResource(R.string.after_first_record))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onRecord(today) }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)) {
                Icon(Icons.Outlined.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (busy) R.string.saving else R.string.record_shave), style = MaterialTheme.typography.titleMedium)
            }
            val count = snapshot.events.count { it.localDate == today }
            Text(if (count > 0) stringResource(R.string.recorded_today, count) else stringResource(R.string.record_today_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (count > 0) Text(stringResource(R.string.multiple_records_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { backfill = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.backfill)) }
        }
    }
    if (backfill) ChooseDate(today, snapshot.cycles.minOfOrNull { it.installedOn }, today, { backfill = false }, onRecord)
}

@Composable
private fun Metric(modifier: Modifier, value: String, label: String, target: String) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.headlineLarge)
        Text(target, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DateMetric(modifier: Modifier, title: String, value: String) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
