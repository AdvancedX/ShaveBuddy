package com.shavebuddy.demo.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shavebuddy.demo.R
import com.shavebuddy.demo.update.*

@Composable
fun SettingsRoute(model: UpdateViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val state by model.state.collectAsStateWithLifecycle()
    val currentVersion = remember(context) { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
    val installer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { /* Keep Continue install when dismissed. */ }
    val launchInstall: () -> Unit = {
        model.prepareInstallation()?.let { file ->
            try {
                val uri = FileProvider.getUriForFile(context, context.packageName + ".updates", file)
                installer.launch(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            } catch (_: ActivityNotFoundException) { model.installationFailed() }
            catch (_: SecurityException) { model.installationFailed() }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (context.packageManager.canRequestPackageInstalls()) launchInstall()
    }
    val attemptInstall: () -> Unit = {
        if (context.packageManager.canRequestPackageInstalls()) launchInstall()
        else {
            model.waitForPermission()
            try { permission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName))) }
            catch (_: ActivityNotFoundException) { model.installationFailed() }
            catch (_: SecurityException) { model.installationFailed() }
        }
    }
    LaunchedEffect(state) {
        if ((state as? UpdateState.Ready)?.stage == InstallStage.AUTOMATIC) attemptInstall()
    }
    SettingsScreen(currentVersion, state, onBack, model::check, model::cancel, attemptInstall)
}

@Composable
fun SettingsScreen(currentVersion: String, state: UpdateState, onBack: () -> Unit, onCheck: () -> Unit, onCancel: () -> Unit, onInstall: () -> Unit) {
    PageColumn {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back)) }
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineLarge)
        }
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.app_updates), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.current_version, currentVersion), style = MaterialTheme.typography.bodyMedium)
                when (state) {
                    UpdateState.Idle -> Text(stringResource(R.string.update_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    UpdateState.Checking -> { CircularProgressIndicator(); Text(stringResource(R.string.checking_updates)) }
                    UpdateState.Latest -> Text(stringResource(R.string.already_latest))
                    UpdateState.Cancelled -> Text(stringResource(R.string.update_cancelled))
                    UpdateState.Cancelling -> { CircularProgressIndicator(); Text(stringResource(R.string.update_cancelling)) }
                    is UpdateState.Downloading -> {
                        Text(stringResource(R.string.downloading_version, state.update.release.tag))
                        if (state.total != null && state.total > 0) {
                            val fraction = (state.received.toFloat() / state.total).coerceIn(0f, 1f)
                            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                            Text(stringResource(R.string.download_percent, (fraction * 100).toInt()))
                        } else { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()); Text(stringResource(R.string.download_in_progress)) }
                    }
                    is UpdateState.Ready -> {
                        Text(stringResource(R.string.download_complete, state.update.release.tag))
                        Text(stringResource(if (state.stage == InstallStage.PERMISSION) R.string.install_permission_hint else R.string.install_ready_hint))
                        Button(onClick = onInstall, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(if (state.stage == InstallStage.PERMISSION) R.string.allow_installation else R.string.continue_installation))
                        }
                    }
                    is UpdateState.Failure -> Text(stringResource(problemMessage(state.problem)), color = MaterialTheme.colorScheme.error)
                }
                when (state) {
                    UpdateState.Checking, is UpdateState.Downloading -> OutlinedButton(onClick = onCancel) { Text(stringResource(R.string.cancel_update)) }
                    UpdateState.Cancelling, is UpdateState.Ready -> Unit
                    else -> Button(onClick = onCheck, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(if (state is UpdateState.Failure) R.string.retry_update else R.string.check_updates))
                    }
                }
            }
        }
    }
}

private fun problemMessage(problem: UpdateProblem): Int = when (problem) {
    UpdateProblem.NETWORK -> R.string.update_network_error
    UpdateProblem.RATE_LIMIT -> R.string.update_rate_limit
    UpdateProblem.METADATA -> R.string.update_metadata_error
    UpdateProblem.NO_PACKAGES -> R.string.update_no_package
    UpdateProblem.SIZE -> R.string.update_size_error
    UpdateProblem.LENGTH -> R.string.update_length_error
    UpdateProblem.DIGEST -> R.string.update_digest_error
    UpdateProblem.STORAGE -> R.string.update_storage_error
    UpdateProblem.PACKAGE -> R.string.update_package_error
    UpdateProblem.VERSION -> R.string.update_version_error
    UpdateProblem.SIGNATURE -> R.string.update_signature_error
    UpdateProblem.INSTALL -> R.string.update_install_error
}
