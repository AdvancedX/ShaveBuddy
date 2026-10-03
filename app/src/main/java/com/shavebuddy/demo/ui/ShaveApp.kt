package com.shavebuddy.demo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.shavebuddy.demo.R
import kotlinx.coroutines.delay

@Composable
fun ShaveApp(model: ShaveViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, model) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { model.refreshDate(); delay(15_000) }
        }
    }
    BackHandler(enabled = tab != 0) { tab = 0 }
    val snapshot = state.snapshot
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (snapshot?.equipment != null) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                listOf(R.string.today to Icons.Outlined.Today, R.string.calendar to Icons.Outlined.CalendarMonth, R.string.gear to Icons.Outlined.Tune).forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = { Icon(icon, null) }, label = { Text(stringResource(label)) })
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.error) {
                Surface(color = MaterialTheme.colorScheme.errorContainer) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(stringResource(R.string.storage_error), color = MaterialTheme.colorScheme.onErrorContainer)
                        Row {
                            TextButton(onClick = model::reload) { Text(stringResource(R.string.reload)) }
                            if (snapshot != null) TextButton(onClick = model::dismissError) { Text(stringResource(R.string.dismiss)) }
                        }
                    }
                }
            }
            when {
                snapshot == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (!state.error) Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.loading))
                    }
                }
                snapshot.equipment == null -> SetupScreen(state.today, state.busy, model::setup)
                tab == 0 -> TodayScreen(snapshot, state.today, state.busy, model::record)
                tab == 1 -> CalendarScreen(snapshot, state.today, state.busy, model::record, model::delete)
                else -> GearScreen(snapshot, state.today, state.busy, model::replace, model::updateSettings)
            }
        }
    }
}
