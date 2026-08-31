package me.mrashidi.bayqush

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import me.mrashidi.bayqush.ui.theme.TeleHootTheme

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
    var smsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        smsGranted = granted
        status = if (granted) "SMS permission granted" else "SMS permission denied"
    }

    LaunchedEffect(Unit) {
        if (!smsGranted) launcher.launch(Manifest.permission.RECEIVE_SMS)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Bot token") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )
        OutlinedTextField(
            value = chatId,
            onValueChange = { chatId = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Channel chat_id") },
            singleLine = true,
        )
        Button(
            onClick = {
                Prefs.save(context, token, chatId)
                status = if (Prefs.configured(context)) "Saved" else "Token and chat_id required"
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save")
        }
        Button(
            onClick = {
                Prefs.save(context, token, chatId)
                if (!Prefs.configured(context)) {
                    status = "Token and chat_id required"
                    return@Button
                }
                TelegramWorker.enqueue(context, "BayQush test")
                status = "Test send queued"
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Test send")
        }
        Text(if (smsGranted) "SMS: granted" else "SMS: not granted")
        if (status.isNotBlank()) Text(status)
    }
}
