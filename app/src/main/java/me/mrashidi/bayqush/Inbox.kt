package me.mrashidi.bayqush

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat

data class InboxSms(val id: Long, val from: String, val body: String, val date: Long)

data class SenderRow(val address: String, val lastBody: String, val count: Int)

fun canonicalSender(raw: String): String {
    val t = raw.trim()
    val digits = t.filter { it.isDigit() }
    return if (digits.length >= 8) digits else t.lowercase()
}

fun sameSender(a: String, b: String): Boolean {
    val ca = canonicalSender(a)
    val cb = canonicalSender(b)
    if (ca == cb) return true
    if (ca.all { it.isDigit() } && cb.all { it.isDigit() }) {
        return ca.endsWith(cb) || cb.endsWith(ca)
    }
    return false
}

fun shouldForward(from: String, forwardAll: Boolean, allowed: Set<String>): Boolean {
    if (forwardAll) return true
    return allowed.any { sameSender(it, from) }
}

fun sendersFromInbox(messages: List<InboxSms>): List<SenderRow> {
    val map = LinkedHashMap<String, SenderRow>()
    for (m in messages) {
        val key = canonicalSender(m.from)
        val existing = map[key]
        if (existing == null) {
            map[key] = SenderRow(m.from, m.body, 1)
        } else {
            map[key] = existing.copy(count = existing.count + 1)
        }
    }
    return map.values.toList()
}

// ponytail: newest 100 only; raise the cap if older SMS need forwarding
fun loadInbox(context: Context, limit: Int = 100): List<InboxSms> {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return emptyList()
    }
    val out = ArrayList<InboxSms>(limit)
    context.contentResolver.query(
        Telephony.Sms.Inbox.CONTENT_URI,
        arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
        null,
        null,
        "${Telephony.Sms.DATE} DESC",
    )?.use { c ->
        val iId = c.getColumnIndexOrThrow(Telephony.Sms._ID)
        val iFrom = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
        val iBody = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
        val iDate = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
        while (c.moveToNext() && out.size < limit) {
            out.add(
                InboxSms(
                    id = c.getLong(iId),
                    from = c.getString(iFrom) ?: "unknown",
                    body = c.getString(iBody).orEmpty(),
                    date = c.getLong(iDate),
                ),
            )
        }
    }
    return out
}
