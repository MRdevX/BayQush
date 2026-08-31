package me.mrashidi.bayqush

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!Prefs.configured(context)) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return
        val from = messages.first().displayOriginatingAddress ?: "unknown"
        if (!Prefs.shouldForward(context, from)) return
        val body = messages.joinToString("") { it.displayMessageBody.orEmpty() }
        TelegramWorker.enqueue(context, formatSms(from, body))
    }
}
