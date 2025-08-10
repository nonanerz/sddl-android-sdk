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
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

object SDDLSDK {

    interface SDDLCallback {
        fun onSuccess(data: JsonObject)
        fun onError(error: String)
    }

    @Volatile
    private var resolving: Boolean = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(5, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    fun fetchDetails(context: Context, data: Uri? = null, callback: SDDLCallback) {
        synchronized(this) {
            if (resolving) return
            resolving = true
        }

        val id = extractIdentifier(context, data)
        if (id != null) {
            getDetailsAsync(id, callback)
        } else {
            getTryDetailsAsync(callback)
        }
    }

    private fun extractIdentifier(context: Context, data: Uri?): String? {
        val fromUrl = data
            ?.pathSegments
            ?.firstOrNull()
            ?.takeIf { isValidId(it) }
        if (fromUrl != null) return fromUrl

        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val clipText = cm.primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            ?.trim()

        return clipText?.takeIf { isValidId(it) }
    }

    private fun isValidId(s: String): Boolean {
        return s.length in 4..64 && s.matches(Regex("^[A-Za-z0-9_-]+$"))
    }

    private fun getDetailsAsync(id: String, callback: SDDLCallback) {
        Thread {
            try {
                val url = "https://sddl.me/api/$id/details"
                val req = Request.Builder().url(url).build()
                client.newCall(req).execute().use { r ->
                    when {
                        r.isSuccessful -> success(r, callback)
                        r.code == 404 || r.code == 410 -> getTryDetailsSync(callback)
                        else -> fail("HTTP ${r.code}", callback)
                    }
                }
            } catch (e: IOException) {
                fail("Network error: ${e.message}", callback)
            } finally {
                resolving = false
            }
        }.start()
    }

    private fun getTryDetailsAsync(callback: SDDLCallback) {
        Thread {
            try {
                getTryDetailsSync(callback)
            } finally {
                resolving = false
            }
        }.start()
    }

    private fun getTryDetailsSync(callback: SDDLCallback) {
        try {
            val req = Request.Builder().url("https://sddl.me/api/try/details").build()
            client.newCall(req).execute().use { r ->
                if (r.isSuccessful) {
                    success(r, callback)
                } else {
                    fail("TRY ${r.code}", callback)
                }
            }
        } catch (e: IOException) {
            fail("Network error: ${e.message}", callback)
        }
    }

    private fun success(r: Response, callback: SDDLCallback) {
        val body = r.body?.string().orEmpty()
        try {
            val el = JsonParser.parseString(body)
            val json = if (el.isJsonObject) el.asJsonObject else JsonObject()
            mainHandler.post { callback.onSuccess(json) }
        } catch (e: Exception) {
            mainHandler.post { callback.onError("Parse error: ${e.message ?: "invalid JSON"}") }
        }
    }

    private fun fail(msg: String, callback: SDDLCallback) {
        mainHandler.post { callback.onError(msg) }
    }
}