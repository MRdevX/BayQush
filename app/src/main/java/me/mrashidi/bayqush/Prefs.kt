package me.mrashidi.bayqush

import android.content.Context

object Prefs {
    private const val FILE = "bayqush"
    private const val TOKEN = "token"
    private const val CHAT_ID = "chat_id"
    private const val FORWARD_ALL = "forward_all"
    private const val SENDERS = "senders"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun token(context: Context): String = prefs(context).getString(TOKEN, "") ?: ""

    fun chatId(context: Context): String = prefs(context).getString(CHAT_ID, "") ?: ""

    fun save(context: Context, token: String, chatId: String) {
        prefs(context).edit()
            .putString(TOKEN, token.trim())
            .putString(CHAT_ID, chatId.trim())
            .apply()
    }

    fun configured(context: Context): Boolean =
        token(context).isNotBlank() && chatId(context).isNotBlank()

    fun forwardAll(context: Context): Boolean =
        prefs(context).getBoolean(FORWARD_ALL, true)

    fun senders(context: Context): Set<String> =
        HashSet(prefs(context).getStringSet(SENDERS, emptySet()) ?: emptySet())

    fun saveFilter(context: Context, forwardAll: Boolean, senders: Set<String>) {
        prefs(context).edit()
            .putBoolean(FORWARD_ALL, forwardAll)
            .putStringSet(SENDERS, HashSet(senders))
            .apply()
    }

    fun shouldForward(context: Context, from: String): Boolean =
        shouldForward(from, forwardAll(context), senders(context))
}
