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

    fun fetchDetails(context: Context, data: Uri? = null, callback: SDDLCallback) {
        val identifier = extractIdentifier(context, data)
        if (identifier != null) {
            proceedWithRequest(identifier, callback)
        } else {
            fetchTryDetails(callback)
        }
    }

    private fun extractIdentifier(context: Context, data: Uri?): String? {
        val firstSegment = data
            ?.pathSegments
            ?.firstOrNull()
            ?.takeIf { segment ->
                segment.length in 4..64 &&
                        segment.matches(Regex("^[a-zA-Z0-9_-]+$"))
            }
        if (firstSegment != null) return firstSegment

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val clipText = clipboard.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            ?.trim()

        return clipText?.takeIf { c ->
            c.length in 4..64 &&
                    c.matches(Regex("^[a-zA-Z0-9_-]+$"))
        }
    }

    private fun fetchTryDetails(callback: SDDLCallback) {
        val tryUrl = "https://sddl.me/api/try/details"
        val client = OkHttpClient()
        val request = Request.Builder().url(tryUrl).build()

        Thread {
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string().orEmpty()
                        val json = JsonParser.parseString(body).asJsonObject
                        Handler(Looper.getMainLooper()).post {
                            callback.onSuccess(json)
                        }
                    } else {
                        Handler(Looper.getMainLooper()).post {
                            callback.onError("Try/details failed: ${response.code}")
                        }
                    }
                }
            } catch (e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    callback.onError("Network error: ${e.message}")
                }
            }
        }.start()
    }

    private fun proceedWithRequest(id: String, callback: SDDLCallback) {
        val url = "https://sddl.me/api/$id/details"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        Thread {
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string().orEmpty()
                        val json = JsonParser.parseString(body).asJsonObject
                        Handler(Looper.getMainLooper()).post {
                            callback.onSuccess(json)
                        }
                    } else {
                        Handler(Looper.getMainLooper()).post {
                            fetchTryDetails(callback)
                        }
                    }
                }
            } catch (e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    callback.onError("Network error: ${e.message}")
                }
            }
        }.start()
    }
}