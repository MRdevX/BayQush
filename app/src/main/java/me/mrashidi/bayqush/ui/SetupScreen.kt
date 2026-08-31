package me.mrashidi.bayqush.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.mrashidi.bayqush.InboxSms
import me.mrashidi.bayqush.Prefs
import me.mrashidi.bayqush.R
import me.mrashidi.bayqush.SenderRow
import me.mrashidi.bayqush.TelegramWorker
import me.mrashidi.bayqush.canonicalSender
import me.mrashidi.bayqush.loadInbox
import me.mrashidi.bayqush.sameSender
import me.mrashidi.bayqush.sendersFromInbox

private val smsPerms = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var token by remember { mutableStateOf(Prefs.token(context)) }
    var chatId by remember { mutableStateOf(Prefs.chatId(context)) }
    var tokenError by remember { mutableStateOf(false) }
    var chatIdError by remember { mutableStateOf(false) }
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
    var configured by remember { mutableStateOf(Prefs.configured(context)) }
    var editingTelegram by remember { mutableStateOf(!configured) }
    var showHelp by remember { mutableStateOf(false) }
    var newSender by remember { mutableStateOf("") }
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
        val message = if (receiveOk && readOk) {
            context.getString(R.string.snackbar_sms_granted)
        } else {
            context.getString(R.string.snackbar_sms_denied)
        }
        scope.launch { showSnackbar(snackbarHostState, message) }
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
            val message = when (info?.state) {
                WorkInfo.State.SUCCEEDED -> context.getString(R.string.snackbar_sent)
                WorkInfo.State.FAILED ->
                    info.outputData.getString(TelegramWorker.KEY_ERROR)
                        ?: context.getString(R.string.snackbar_send_failed)
                WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED ->
                    context.getString(R.string.snackbar_sending)
                else -> null
            }
            if (message != null) showSnackbar(snackbarHostState, message)
        }
    }

    fun saveTelegram() {
        val tokenBlank = token.isBlank()
        val chatBlank = chatId.isBlank()
        tokenError = tokenBlank
        chatIdError = chatBlank
        Prefs.save(context, token, chatId)
        configured = Prefs.configured(context)
        if (configured) {
            editingTelegram = false
            scope.launch {
                showSnackbar(snackbarHostState, context.getString(R.string.snackbar_saved))
            }
        } else {
            scope.launch {
                showSnackbar(
                    snackbarHostState,
                    context.getString(R.string.error_credentials_required),
                )
            }
        }
    }

    fun sendTest() {
        if (!Prefs.configured(context)) {
            editingTelegram = true
            tokenError = token.isBlank()
            chatIdError = chatId.isBlank()
            scope.launch {
                showSnackbar(
                    snackbarHostState,
                    context.getString(R.string.error_credentials_required),
                )
            }
            return
        }
        workId = TelegramWorker.enqueue(context, context.getString(R.string.test_message))
    }

    fun setForwardAll(value: Boolean) {
        forwardAll = value
        Prefs.saveFilter(context, value, selectedSenders)
        scope.launch {
            showSnackbar(
                snackbarHostState,
                context.getString(
                    if (value) R.string.forwarding_all else R.string.forwarding_chosen,
                ),
            )
        }
    }

    fun toggle(address: String) {
        toggleSender(context, address, selectedSenders) { next, all ->
            selectedSenders = next
            forwardAll = all
        }
        scope.launch {
            showSnackbar(snackbarHostState, context.getString(R.string.forwarding_chosen))
        }
    }

    fun addNumber() {
        val address = newSender.trim()
        if (address.isBlank()) return
        if (selectedSenders.any { sameSender(it, address) }) {
            newSender = ""
            return
        }
        val next = selectedSenders + address
        selectedSenders = next
        forwardAll = false
        Prefs.saveFilter(context, forwardAll = false, senders = next)
        newSender = ""
        scope.launch {
            showSnackbar(snackbarHostState, context.getString(R.string.added_sender))
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 4.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (!receiveOk || !readOk) {
                item {
                    SmsPermissionBanner(onGrant = { launcher.launch(smsPerms) })
                }
            }
            if (configured) {
                item {
                    StatusCard(
                        batteryOk = batteryOk,
                        chatId = Prefs.chatId(context),
                        forwardAll = forwardAll,
                        selectedCount = selectedSenders.size,
                        onSendTest = ::sendTest,
                    )
                }
            } else {
                item { SetupIntro() }
            }
            item {
                TelegramCard(
                    editing = editingTelegram,
                    token = token,
                    chatId = chatId,
                    tokenError = tokenError,
                    chatIdError = chatIdError,
                    savePrimary = receiveOk && readOk,
                    onTokenChange = {
                        token = it
                        if (tokenError && it.isNotBlank()) tokenError = false
                    },
                    onChatIdChange = {
                        chatId = it
                        if (chatIdError && it.isNotBlank()) chatIdError = false
                    },
                    onSave = ::saveTelegram,
                    onEdit = {
                        token = Prefs.token(context)
                        chatId = Prefs.chatId(context)
                        tokenError = false
                        chatIdError = false
                        editingTelegram = true
                    },
                    onHelp = { showHelp = true },
                )
            }
            item {
                ForwardingCard(
                    forwardAll = forwardAll,
                    selectedCount = selectedSenders.size,
                    readOk = readOk,
                    newSender = newSender,
                    senderRows = senderRows,
                    selectedSenders = selectedSenders,
                    onNewSenderChange = { newSender = it },
                    onAddSender = ::addNumber,
                    onForwardAllChange = ::setForwardAll,
                    onRefresh = { reload++ },
                    onToggleSender = ::toggle,
                )
            }
            item {
                BackgroundCard(
                    batteryOk = batteryOk,
                    allowPrimary = configured && receiveOk && readOk,
                    onAllowBackground = { openBackgroundSettings(context) },
                )
            }
        }
    }

    if (showHelp) {
        TelegramHelpSheet(onDismiss = { showHelp = false })
    }
}

private suspend fun showSnackbar(host: SnackbarHostState, message: String) {
    host.currentSnackbarData?.dismiss()
    host.showSnackbar(message)
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
