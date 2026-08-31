package me.mrashidi.telehoot

import android.content.Context

object Prefs {
    private const val FILE = "telehoot"
    private const val TOKEN = "token"
    private const val CHAT_ID = "chat_id"

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
}
