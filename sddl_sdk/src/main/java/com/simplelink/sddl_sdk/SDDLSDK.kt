package com.simplelink.sddl_sdk

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

    fun fetchDetails(data: Uri?, callback: SDDLCallback) {
        if (data == null) {
            callback.onError("No valid App Link provided")
            return
        }

        val identifier = data.lastPathSegment
        if (identifier.isNullOrEmpty()) {
            callback.onError("Invalid identifier in App Link")
            return
        }

        proceedWithRequest(identifier, callback)
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