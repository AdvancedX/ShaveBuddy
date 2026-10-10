package com.shavebuddy.demo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shavebuddy.demo.R
import com.shavebuddy.demo.domain.ShaveRules
import java.time.LocalDate

@Composable
fun SetupScreen(today: LocalDate, busy: Boolean, onSetup: (String, LocalDate, Int?, Int?, Int) -> Unit) {
    PageColumn {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        IllustratedHeader(stringResource(R.string.welcome_title))
        SettingsForm(today = today, busy = busy, onSave = onSetup)
        Text(stringResource(R.string.private_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsForm(
    today: LocalDate,
    busy: Boolean,
    initialName: String? = null,
    initialUses: Int? = 10,
    initialDays: Int? = 30,
    initialInterval: Int = 2,
    editing: Boolean = false,
    onSave: (String, LocalDate, Int?, Int?, Int) -> Unit,
) {
    val defaultName = initialName ?: stringResource(R.string.default_equipment)
    var name by rememberSaveable { mutableStateOf(defaultName) }
    var installed by rememberSaveable { mutableStateOf(today.toString()) }
    var uses by rememberSaveable { mutableStateOf(initialUses?.toString() ?: "") }
    var days by rememberSaveable { mutableStateOf(initialDays?.toString() ?: "") }
    var interval by rememberSaveable { mutableStateOf(initialInterval.toString()) }
    var pickingDate by remember { mutableStateOf(false) }
    val parsedUses = uses.toIntOrNull()
    val parsedDays = days.toIntOrNull()
    val parsedInterval = interval.toIntOrNull()
    val valid = parsedInterval != null && (uses.isBlank() || parsedUses != null) &&
        (days.isBlank() || parsedDays != null) && ShaveRules.validSettings(name, parsedInterval, parsedUses, parsedDays)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.equipment_name)) }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
        if (!editing) {
            OutlinedButton(onClick = { pickingDate = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.installed_on) + " · " + formatDate(LocalDate.parse(installed)))
            }
        }
        NumberField(uses, { uses = it }, R.string.target_uses, busy)
        NumberField(days, { days = it }, R.string.target_days, busy)
        Text(stringResource(R.string.target_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        NumberField(interval, { interval = it }, R.string.interval_days, busy)
        Text(stringResource(R.string.form_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = { onSave(name, LocalDate.parse(installed), parsedUses, parsedDays, requireNotNull(parsedInterval)) },
            enabled = valid && !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(stringResource(if (busy) R.string.saving else if (editing) R.string.save_settings else R.string.start))
        }
    }
    if (pickingDate) ChooseDate(LocalDate.parse(installed), null, today, { pickingDate = false }, { installed = it.toString() })
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: Int, busy: Boolean) {
    OutlinedTextField(value, onChange, label = { Text(stringResource(label)) }, enabled = !busy, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
}
