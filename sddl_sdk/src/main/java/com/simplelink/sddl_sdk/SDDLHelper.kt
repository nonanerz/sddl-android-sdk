package com.simplelink.sddl_sdk

import android.content.Intent
import androidx.activity.ComponentActivity
import com.google.gson.JsonObject

object SDDLHelper {
    fun resolve(
        activity: ComponentActivity,
        intent: Intent?,
        onSuccess: (JsonObject) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        SDDLSDK.fetchDetails(
            context = activity,
            data = intent?.data,
            callback = object : SDDLSDK.SDDLCallback {
                override fun onSuccess(data: JsonObject) = onSuccess(data)
                override fun onError(error: String) = onError(error)
            }
        )
    }
}