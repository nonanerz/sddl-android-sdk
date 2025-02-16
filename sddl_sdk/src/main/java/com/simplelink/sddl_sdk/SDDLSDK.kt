package com.simplelink.sddl_sdk

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

object SDDLSDK {

    interface SDDLCallback {
        fun onSuccess(data: JsonObject)
        fun onError(error: String)
    }

    fun fetchDetails(context: Context, data: Uri? = null, customScheme: String = "", callback: SDDLCallback) {
        if (data == null) {
            Handler(Looper.getMainLooper()).postDelayed({
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val id = if (clipboard.hasPrimaryClip() && (clipboard.primaryClip?.itemCount ?: 0) > 0) {
                    clipboard.primaryClip?.getItemAt(0)?.coerceToText(context).toString()
                } else {
                    ""
                }
                if (id.isEmpty() || id == "null") {
                    callback.onError("No identifier found")
                    return@postDelayed
                }
                proceedWithRequest(id, callback)
            }, 500)
        } else if (customScheme.isEmpty() || data.scheme == customScheme) {
            val id = data.host
            if (id.isNullOrEmpty()) {
                callback.onError("No identifier found")
                return
            }
            proceedWithRequest(id, callback)
        }
    }

    private fun proceedWithRequest(id: String, callback: SDDLCallback) {
        val url = "https://sddl.me/api/$id/details"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        Thread {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        callback.onError("Request failed: ${response.message}")
                        return@Thread
                    }
                    val responseBody = response.body?.string().orEmpty()
                    val jsonData = JsonParser.parseString(responseBody).asJsonObject
                    callback.onSuccess(jsonData)
                }
            } catch (e: IOException) {
                callback.onError("Network error: ${e.message}")
            }
        }.start()
    }
}