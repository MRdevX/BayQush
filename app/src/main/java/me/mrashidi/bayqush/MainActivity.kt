package me.mrashidi.bayqush

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.mrashidi.bayqush.ui.theme.BayQushTheme

private val smsPerms = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)

private enum class StatusTone { Neutral, Ok, Error }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BayQushTheme {
                SetupScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen() {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var token by remember { mutableStateOf(Prefs.token(context)) }
    var chatId by remember { mutableStateOf(Prefs.chatId(context)) }
    var status by remember { mutableStateOf("") }
    var statusTone by remember { mutableStateOf(StatusTone.Neutral) }
    var workId by remember { mutableStateOf<UUID?>(null) }
    var receiveOk by remember {
        mutableStateOf(granted(context, Manifest.permission.RECEIVE_SMS))
    }
    var readOk by remember {
        mutableStateOf(granted(context, Manifest.permission.READ_SMS))
    }
    var messages by remember { mutableStateOf(emptyList<InboxSms>()) }
    var forwardAll by remember { mutableStateOf(Prefs.forwardAll(context)) }
    var selectedSenders by remember { mutableStateOf(Prefs.senders(context)) }
    var reload by remember { mutableStateOf(0) }
    var batteryOk by remember { mutableStateOf(ignoringBattery(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val senderRows = remember(messages, selectedSenders) {
        val inbox = sendersFromInbox(messages)
        val seen = inbox.map { canonicalSender(it.address) }.toSet()
        val extra = selectedSenders
            .filter { canonicalSender(it) !in seen }
            .map { SenderRow(it, "", 0) }
        inbox + extra
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        receiveOk = result[Manifest.permission.RECEIVE_SMS] == true
        readOk = result[Manifest.permission.READ_SMS] == true
        if (receiveOk && readOk) {
            status = "SMS permission granted"
            statusTone = StatusTone.Ok
        } else {
            status = "SMS permission denied"
            statusTone = StatusTone.Error
        }
    }

    fun setStatus(text: String, tone: StatusTone) {
        status = text
        statusTone = tone
    }

    LaunchedEffect(Unit) {
        if (!receiveOk || !readOk) launcher.launch(smsPerms)
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                batteryOk = ignoringBattery(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(readOk, reload) {
        messages = if (readOk) {
            withContext(Dispatchers.IO) { loadInbox(context) }
        } else {
            emptyList()
        }
    }
    LaunchedEffect(workId) {
        val id = workId ?: return@LaunchedEffect
        WorkManager.getInstance(context).getWorkInfoByIdFlow(id).collect { info ->
            when (info?.state) {
                WorkInfo.State.SUCCEEDED -> setStatus("Sent to Telegram", StatusTone.Ok)
                WorkInfo.State.FAILED -> setStatus(
                    info.outputData.getString(TelegramWorker.KEY_ERROR) ?: "Send failed",
                    StatusTone.Error,
                )
                WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED ->
                    setStatus("Sending…", StatusTone.Neutral)
                else -> Unit
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("BayQush") },
                actions = {
                    TextButton(onClick = { reload++ }) { Text("Refresh") }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "Telegram",
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            item {
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Bot token") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
            item {
                OutlinedTextField(
                    value = chatId,
                    onValueChange = { chatId = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Channel chat_id") },
                    singleLine = true,
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = {
                            Prefs.save(context, token, chatId)
                            if (Prefs.configured(context)) {
                                setStatus("Saved", StatusTone.Ok)
                            } else {
                                setStatus("Token and chat_id required", StatusTone.Error)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save")
                    }
                    OutlinedButton(
                        onClick = {
                            Prefs.save(context, token, chatId)
                            if (!Prefs.configured(context)) {
                                setStatus("Token and chat_id required", StatusTone.Error)
                                return@OutlinedButton
                            }
                            workId = TelegramWorker.enqueue(context, "BayQush test")
                            setStatus("Sending…", StatusTone.Neutral)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Test")
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { openBackgroundSettings(context) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (batteryOk) "Background settings" else "Allow background")
                }
            }
            item {
                Text(
                    if (batteryOk) {
                        "Background: allowed. On Samsung also add BayQush to Never sleeping apps."
                    } else {
                        "Background: restricted. Tap Allow background, then Allow."
                    },
                    color = if (batteryOk) scheme.onSurfaceVariant else scheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (status.isNotBlank()) {
                item {
                    Text(
                        status,
                        color = when (statusTone) {
                            StatusTone.Ok -> scheme.primary
                            StatusTone.Error -> scheme.error
                            StatusTone.Neutral -> scheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (!receiveOk || !readOk) {
                item {
                    Column {
                        Text(
                            "SMS access is needed to list senders and forward messages.",
                            color = scheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        TextButton(onClick = { launcher.launch(smsPerms) }) {
                            Text("Grant")
                        }
                    }
                }
            }
            item {
                Text(
                    "Forward from",
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("All senders") },
                    supportingContent = {
                        Text(
                            if (forwardAll) "Every incoming SMS" else "Only checked senders",
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = forwardAll,
                            onCheckedChange = {
                                forwardAll = it
                                Prefs.saveFilter(context, it, selectedSenders)
                                setStatus(
                                    if (it) "Forwarding every incoming SMS" else "Only checked senders",
                                    StatusTone.Neutral,
                                )
                            },
                        )
                    },
                )
            }
            if (!forwardAll) {
                item {
                    Text(
                        "${selectedSenders.size} selected",
                        color = scheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (!readOk) {
                item {
                    Text(
                        "Grant SMS read permission to list senders.",
                        color = scheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else if (senderRows.isEmpty()) {
                item {
                    Text(
                        "No SMS in inbox.",
                        color = scheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            items(senderRows, key = { canonicalSender(it.address) }) { row ->
                val checked = forwardAll || selectedSenders.any { sameSender(it, row.address) }
                ListItem(
                    modifier = Modifier.clickable(enabled = !forwardAll) {
                        toggleSender(context, row.address, selectedSenders) { next, all ->
                            selectedSenders = next
                            forwardAll = all
                            setStatus("Only checked senders", StatusTone.Neutral)
                        }
                    },
                    headlineContent = { Text(row.address) },
                    supportingContent = if (row.count > 0) {
                        {
                            Text(
                                "${row.count} in inbox · ${row.lastBody}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else {
                        null
                    },
                    trailingContent = {
                        Checkbox(
                            checked = checked,
                            enabled = !forwardAll,
                            onCheckedChange = {
                                toggleSender(context, row.address, selectedSenders) { next, all ->
                                    selectedSenders = next
                                    forwardAll = all
                                    setStatus("Only checked senders", StatusTone.Neutral)
                                }
                            },
                        )
                    },
                )
            }
        }
    }
}

private fun granted(context: Context, perm: String): Boolean =
    ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

private fun ignoringBattery(context: Context): Boolean {
    val pm = context.getSystemService(PowerManager::class.java)
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun openBackgroundSettings(context: Context) {
    val pkg = Uri.parse("package:${context.packageName}")
    val primary = if (!ignoringBattery(context)) {
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(pkg)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(pkg)
    }
    try {
        context.startActivity(primary)
    } catch (_: Exception) {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(pkg))
    }
}

private fun toggleSender(
    context: Context,
    address: String,
    current: Set<String>,
    apply: (Set<String>, Boolean) -> Unit,
) {
    val next = if (current.any { sameSender(it, address) }) {
        current.filterNot { sameSender(it, address) }.toSet()
    } else {
        current + address
    }
    apply(next, false)
    Prefs.saveFilter(context, forwardAll = false, senders = next)
}
