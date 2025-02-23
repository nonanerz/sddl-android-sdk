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
        val tryDetailsUrl = "https://sddl.me/api/try/details"
        val client = OkHttpClient()
        val requestTry = Request.Builder().url(tryDetailsUrl).build()

        Thread {
            try {
                client.newCall(requestTry).execute().use { response ->
                    if (response.code == 200) {
                        val responseBody = response.body?.string().orEmpty()
                        val jsonData = JsonParser.parseString(responseBody).asJsonObject
                        Handler(Looper.getMainLooper()).post {
                            callback.onSuccess(jsonData)
                        }
                    } else {
                        fallbackFetchDetails(context, data, customScheme, callback)
                    }
                }
            } catch (e: IOException) {
                fallbackFetchDetails(context, data, customScheme, callback)
            }
        }.start()
    }

    private fun fallbackFetchDetails(context: Context, data: Uri?, customScheme: String, callback: SDDLCallback) {
        var identifier: String? = null

        if (data != null && (customScheme.isEmpty() || data.scheme == customScheme)) {
            identifier = data.host?.replace("/", "")
        }

        if (identifier.isNullOrEmpty()) {
            Handler(Looper.getMainLooper()).postDelayed({
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val localId = if (clipboard.hasPrimaryClip() && (clipboard.primaryClip?.itemCount ?: 0) > 0) {
                    clipboard.primaryClip?.getItemAt(0)?.coerceToText(context).toString().trim()
                } else {
                    ""
                }

                if (localId.isEmpty() || localId == "null") {
                    callback.onError("No identifier found")
                } else {
                    proceedWithRequest(localId, callback)
                }
            }, 500)
        } else {
            proceedWithRequest(identifier, callback)
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
                    Handler(Looper.getMainLooper()).post {
                        callback.onSuccess(jsonData)
                    }
                }
            } catch (e: IOException) {
                callback.onError("Network error: ${e.message}")
            }
        }.start()
    }
}