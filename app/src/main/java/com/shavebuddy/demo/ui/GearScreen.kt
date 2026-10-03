package com.shavebuddy.demo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shavebuddy.demo.R
import com.shavebuddy.demo.domain.*
import java.time.LocalDate

@Composable
fun GearScreen(snapshot: ShaveSnapshot, today: LocalDate, busy: Boolean, onReplace: () -> Unit, onUpdate: (String, Int?, Int?, Int, () -> Unit) -> Unit) {
    var replacing by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    val current = snapshot.cycles.firstOrNull { it.retiredOn == null }
    val equipment = requireNotNull(snapshot.equipment)
    BackHandler(enabled = editing) { editing = false }
    PageColumn {
        if (editing) {
            SectionTitle(stringResource(R.string.edit_title), stringResource(R.string.target_hint))
            SettingsForm(today, busy, equipment.name, current?.targetUses, current?.targetDays, equipment.intervalDays, editing = true) { name, _, uses, days, interval ->
                onUpdate(name, uses, days, interval) { editing = false }
            }
            TextButton(onClick = { editing = false }, enabled = !busy) { Text(stringResource(R.string.cancel)) }
        } else {
            SectionTitle(stringResource(R.string.gear_title), stringResource(R.string.gear_subtitle))
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.manual_only), style = MaterialTheme.typography.labelMedium)
                    Text(equipment.name, style = MaterialTheme.typography.headlineSmall)
                    current?.let { Text(stringResource(R.string.installed_date, formatDate(it.installedOn)), style = MaterialTheme.typography.bodyMedium) }
                    Button(onClick = { replacing = true }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(stringResource(R.string.replace_blade)) }
                    TextButton(onClick = { editing = true }, enabled = !busy) { Text(stringResource(R.string.edit_settings)) }
                }
            }
            Text(stringResource(R.string.cycle_history), style = MaterialTheme.typography.titleLarge)
            snapshot.cycles.sortedWith(compareByDescending<Cycle> { it.retiredOn == null }.thenByDescending { it.createdAt }).forEach { cycle ->
                val count = snapshot.events.count { it.cycleId == cycle.id }
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(if (cycle.retiredOn == null) R.string.active_history else R.string.retired_history, count), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.cycle_range, cycle.installedOn.toString(), cycle.retiredOn?.toString() ?: stringResource(R.string.present)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(stringResource(R.string.private_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (replacing) ConfirmDialog(stringResource(R.string.replace_title), stringResource(R.string.replace_body), stringResource(R.string.confirm_replace), { replacing = false }, onReplace)
}
