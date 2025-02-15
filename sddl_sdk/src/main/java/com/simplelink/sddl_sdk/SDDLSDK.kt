package com.simplelink.sddl_sdk

import android.content.ClipboardManager
import android.content.Context
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import android.net.Uri

object SDDLSDK {

    interface SDDLCallback {
        fun onSuccess(data: JsonObject)
        fun onError(error: String)
    }

    fun fetchDetails(context: Context, data: Uri? = null, customScheme: String = "", callback: SDDLCallback) {
        var id: String? = null

        if (data == null) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip() && (clipboard.primaryClip?.itemCount ?: 0) > 0) {
                id = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context).toString()
            }
        } else if (customScheme.isEmpty() || data.scheme == customScheme) {
            id = data.host
        }

        if (id.isNullOrEmpty()) {
            callback.onError("No identifier found")
            return
        }

        val url = "https://sddl.me/api/$id/details"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        Thread {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        callback.onError("Request failed: ${'$'}{response.message}")
                        return@Thread
                    }
                    val jsonData = JsonParser.parseString(response.body?.string()).asJsonObject
                    callback.onSuccess(jsonData)
                }
            } catch (e: IOException) {
                callback.onError("Network error: ${'$'}{e.message}")
            }
        }.start()
    }
}