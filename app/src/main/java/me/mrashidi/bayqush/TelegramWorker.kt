package me.mrashidi.bayqush

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.workDataOf
import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

fun formatSms(from: String, body: String): String = "From: $from\n\n$body".take(4096)

fun telegramErrorText(raw: String): String {
    val key = "\"description\":\""
    val i = raw.indexOf(key)
    if (i >= 0) {
        val start = i + key.length
        val end = raw.indexOf('"', start)
        if (end > start) {
            return raw.substring(start, end).removePrefix("Bad Request: ").trim()
        }
    }
    return raw.removePrefix("Telegram ").trim().ifBlank { "Telegram error" }
}

class TelegramWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val token = Prefs.token(applicationContext)
        val chatId = Prefs.chatId(applicationContext)
        val text = inputData.getString(KEY_TEXT)?.take(4096)
            ?: return Result.failure(workDataOf(KEY_ERROR to "empty message"))
        if (token.isBlank() || chatId.isBlank()) {
            return Result.failure(workDataOf(KEY_ERROR to "Token and chat_id required"))
        }

        return try {
            post(token, chatId, text)
            Result.success()
        } catch (e: IllegalStateException) {
            Log.e(TAG, e.message ?: "Telegram error")
            Result.failure(workDataOf(KEY_ERROR to telegramErrorText(e.message ?: "Telegram error")))
        } catch (e: IOException) {
            Log.e(TAG, "network", e)
            Result.retry()
        }
    }

    companion object {
        const val KEY_TEXT = "text"
        const val KEY_ERROR = "error"
        private const val TAG = "BayQush"

        fun enqueue(context: Context, text: String): UUID {
            val request = OneTimeWorkRequestBuilder<TelegramWorker>()
                .setInputData(workDataOf(KEY_TEXT to text.take(4096)))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueue(request)
            return request.id
        }
    }
}

internal fun post(token: String, chatId: String, text: String) {
    val url = URL("https://api.telegram.org/bot$token/sendMessage")
    val body =
        "chat_id=${URLEncoder.encode(chatId, Charsets.UTF_8.name())}" +
            "&text=${URLEncoder.encode(text, Charsets.UTF_8.name())}"
    val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        connectTimeout = 15_000
        readTimeout = 15_000
        setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
    }
    try {
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        if (code !in 200..299) {
            val err = (conn.errorStream ?: conn.inputStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code in 400..499 && code != 429) {
                throw IllegalStateException("Telegram $code $err")
            }
            throw IOException("Telegram $code $err")
        }
    } finally {
        conn.disconnect()
    }
}
