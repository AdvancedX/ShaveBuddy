package com.shavebuddy.demo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.shavebuddy.demo.R
import com.shavebuddy.demo.domain.*
import java.time.*
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(snapshot: ShaveSnapshot, today: LocalDate, busy: Boolean, onRecord: (LocalDate) -> Unit, onDelete: (String) -> Unit) {
    var selectedValue by rememberSaveable { mutableStateOf(today.toString()) }
    var monthValue by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = LocalDate.parse(selectedValue)
    val month = YearMonth.parse(monthValue)
    val counts = snapshot.events.groupingBy { it.localDate }.eachCount()
    val events = snapshot.events.filter { it.localDate == selected }.sortedByDescending { it.occurredAt }
    val current = snapshot.cycles.firstOrNull { it.retiredOn == null }
    val canRecord = !selected.isAfter(today) && ShaveRules.cycleForDate(snapshot.cycles, selected) != null
    PageColumn {
        SectionTitle(stringResource(R.string.calendar_title), stringResource(R.string.calendar_subtitle))
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { monthValue = month.minusMonths(1).toString() }) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(R.string.previous_month))
                    }
                    Text(stringResource(R.string.month_title, month.year, month.monthValue), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { monthValue = month.plusMonths(1).toString() }, enabled = month < YearMonth.from(today)) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(R.string.next_month))
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    stringResource(R.string.weekdays).split(",").forEach { day ->
                        Box(Modifier.weight(1f).height(32.dp), contentAlignment = Alignment.Center) {
                            Text(day, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                val offset = month.atDay(1).dayOfWeek.value - 1
                val rows = (offset + month.lengthOfMonth() + 6) / 7
                repeat(rows) { week ->
                    Row(Modifier.fillMaxWidth()) {
                        repeat(7) { weekday ->
                            val number = week * 7 + weekday - offset + 1
                            if (number !in 1..month.lengthOfMonth()) Box(Modifier.weight(1f).heightIn(min = 48.dp))
                            else {
                                val date = month.atDay(number)
                                val isSelected = date == selected
                                val description = stringResource(R.string.day_accessibility, formatDate(date), counts[date] ?: 0)
                                val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                val fg = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                Column(Modifier.weight(1f).heightIn(min = 48.dp).background(bg, RoundedCornerShape(12.dp))
                                    .clickable { selectedValue = date.toString() }.semantics { contentDescription = description }
                                    .padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(number.toString(), color = fg, style = MaterialTheme.typography.bodyMedium)
                                    Box(Modifier.size(4.dp).background(if (counts.containsKey(date)) fg else bg, CircleShape))
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = { selectedValue = today.toString(); monthValue = YearMonth.from(today).toString() }) { Text(stringResource(R.string.return_today)) }
            }
        }
        Text(formatDate(selected), style = MaterialTheme.typography.titleMedium)
        if (events.isEmpty()) Text(stringResource(if (canRecord) R.string.empty_day else R.string.no_cycle_day), color = MaterialTheme.colorScheme.onSurfaceVariant)
        events.forEach { event ->
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val time = Instant.ofEpochMilli(event.occurredAt).atZone(ZoneId.of(event.timeZone)).format(DateTimeFormatter.ofPattern("HH:mm"))
                        Text(stringResource(R.string.shave_entry, time), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(if (event.cycleId == current?.id) R.string.current_cycle_label else R.string.historical_cycle_label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { deleting = event.id }, enabled = !busy) { Text(stringResource(R.string.delete)) }
                }
            }
        }
        if (canRecord) OutlinedButton(onClick = { onRecord(selected) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.record_selected_day)) }
    }
    deleting?.let { id -> ConfirmDialog(stringResource(R.string.delete_title), stringResource(R.string.delete_body), stringResource(R.string.confirm_delete), { deleting = null }, { onDelete(id) }) }
}
