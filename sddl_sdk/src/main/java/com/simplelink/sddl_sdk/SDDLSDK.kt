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

    private const val USER_AGENT = "SDDLSDK-Android/1.0"

    fun fetchDetails(context: Context, data: Uri? = null, callback: SDDLCallback) {
        synchronized(this) {
            if (resolving) return
            resolving = true
        }

        val id = extractIdentifier(context, data)
        if (id != null) {
            getDetailsAsync(context, id, callback)
        } else {
            getTryDetailsAsync(context, callback)
        }
    }

    private fun extractIdentifier(context: Context, data: Uri?): String? {
        val fromUrl = data
            ?.pathSegments
            ?.firstOrNull()
            ?.takeIf { isValidId(it) }
        if (fromUrl != null) return fromUrl

        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            ?.trim()
            .orEmpty()

        return clip.takeIf { isValidId(it) }
    }

    private fun isValidId(s: String): Boolean {
        return s.length in 4..64 && s.matches(Regex("^[A-Za-z0-9_-]+$"))
    }

    // -- Networking -----------------------------------------------------------

    private fun addCommonHeaders(builder: Request.Builder, context: Context): Request.Builder {
        builder.header("User-Agent", USER_AGENT)
        builder.header("X-Device-Platform", "Android")
        val pkg = context.packageName
        if (!pkg.isNullOrBlank()) {
            builder.header("X-App-Identifier", pkg)
        }
        return builder
    }

    private fun getDetailsAsync(context: Context, id: String, callback: SDDLCallback) {
        Thread {
            try {
                val url = "https://sddl.me/api/$id/details"
                val req = addCommonHeaders(Request.Builder().url(url), context).build()
                client.newCall(req).execute().use { r ->
                    when {
                        r.isSuccessful -> success(r, callback)
                        r.code == 404 || r.code == 410 -> getTryDetailsSync(context, callback)
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

    private fun getTryDetailsAsync(context: Context, callback: SDDLCallback) {
        Thread {
            try {
                getTryDetailsSync(context, callback)
            } finally {
                resolving = false
            }
        }.start()
    }

    private fun getTryDetailsSync(context: Context, callback: SDDLCallback) {
        try {
            val req = addCommonHeaders(
                Request.Builder().url("https://sddl.me/api/try/details"),
                context
            ).build()

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
