package com.shavebuddy.demo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shavebuddy.demo.R
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE))

@Composable
fun PageColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp), content = content)
}

@Composable
fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.headlineLarge)
}

@Composable
fun IllustratedHeader(title: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge)
        Image(painterResource(R.drawable.razor_illustration), contentDescription = null, modifier = Modifier.size(88.dp))
    }
}

@Composable
fun ConfirmDialog(title: String, body: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(body) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseDate(initial: LocalDate, min: LocalDate?, max: LocalDate, onDismiss: () -> Unit, onSelected: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = remember(min, max) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return !date.isAfter(max) && (min == null || !date.isBefore(min))
                }
                override fun isSelectableYear(year: Int): Boolean = year <= max.year && (min == null || year >= min.year)
            }
        },
    )
    DatePickerDialog(onDismissRequest = onDismiss,
        confirmButton = { TextButton(enabled = state.selectedDateMillis != null, onClick = {
            state.selectedDateMillis?.let { onSelected(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
            onDismiss()
        }) { Text(stringResource(R.string.confirm_date)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) { DatePicker(state = state) }
}
