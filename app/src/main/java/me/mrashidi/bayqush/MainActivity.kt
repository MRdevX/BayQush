package me.mrashidi.bayqush

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.mrashidi.bayqush.ui.theme.TeleHootTheme

private val smsPerms = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TeleHootTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SetupScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun SetupScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var token by remember { mutableStateOf(Prefs.token(context)) }
    var chatId by remember { mutableStateOf(Prefs.chatId(context)) }
    var status by remember { mutableStateOf("") }
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
        status = when {
            receiveOk && readOk -> "SMS permission granted"
            else -> "SMS permission denied"
        }
    }

    LaunchedEffect(Unit) {
        if (!receiveOk || !readOk) launcher.launch(smsPerms)
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
            status = when (info?.state) {
                WorkInfo.State.SUCCEEDED -> "Sent to Telegram"
                WorkInfo.State.FAILED ->
                    info.outputData.getString(TelegramWorker.KEY_ERROR) ?: "Send failed"
                WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> "Sending…"
                else -> status
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
            Button(
                onClick = {
                    Prefs.save(context, token, chatId)
                    status = if (Prefs.configured(context)) "Saved" else "Token and chat_id required"
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
        }
        item {
            Button(
                onClick = {
                    Prefs.save(context, token, chatId)
                    if (!Prefs.configured(context)) {
                        status = "Token and chat_id required"
                        return@Button
                    }
                    workId = TelegramWorker.enqueue(context, "BayQush test")
                    status = "Sending…"
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Test send")
            }
        }
        item {
            Text(
                buildString {
                    append(if (receiveOk) "Receive: granted" else "Receive: not granted")
                    append(" · ")
                    append(if (readOk) "Read: granted" else "Read: not granted")
                },
            )
        }
        if (status.isNotBlank()) {
            item { Text(status) }
        }
        item {
            Text("Forward from")
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("All senders")
                Switch(
                    checked = forwardAll,
                    onCheckedChange = {
                        forwardAll = it
                        Prefs.saveFilter(context, it, selectedSenders)
                        status = if (it) {
                            "Forwarding every incoming SMS"
                        } else {
                            "Only checked senders"
                        }
                    },
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { reload++ }) { Text("Refresh") }
                if (!forwardAll) {
                    Text("${selectedSenders.size} selected")
                }
            }
        }
        if (!readOk) {
            item { Text("Grant SMS read permission to list senders.") }
        } else if (senderRows.isEmpty()) {
            item { Text("No SMS in inbox.") }
        }
        items(senderRows, key = { canonicalSender(it.address) }) { row ->
            val checked = forwardAll || selectedSenders.any { sameSender(it, row.address) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !forwardAll) {
                        toggleSender(context, row.address, selectedSenders) { next, all ->
                            selectedSenders = next
                            forwardAll = all
                            status = "Only checked senders"
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = checked,
                    enabled = !forwardAll,
                    onCheckedChange = {
                        toggleSender(context, row.address, selectedSenders) { next, all ->
                            selectedSenders = next
                            forwardAll = all
                            status = "Only checked senders"
                        }
                    },
                )
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(row.address)
                    if (row.count > 0) {
                        Text(
                            "${row.count} in inbox · ${row.lastBody}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private fun granted(context: Context, perm: String): Boolean =
    ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

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
