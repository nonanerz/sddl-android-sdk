package com.simplelink.sddl_sdk

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

internal data class ReferrerInfo(
    val raw: String,
    val clickTsSec: Long,
    val installBeginTsSec: Long,
    val params: Map<String, String>
)

internal object InstallReferrerFetcher {

    private const val PREFS = "sddl_sdk_prefs"
    private const val KEY_RAW = "sddl.referrer.raw"
    private const val KEY_CLICK = "sddl.referrer.click"
    private const val KEY_INSTALL = "sddl.referrer.install"
    private const val KEY_SENT = "sddl.referrer.sent.v1"

    fun fetchOnceAsync(context: Context, onDone: (ReferrerInfo?) -> Unit) {
        readCached(context)?.let { onDone(it); return }

        thread(name = "sddl-referrer") {
            val client = InstallReferrerClient.newBuilder(context).build()
            var result: ReferrerInfo? = null
            try {
                val latch = CountDownLatch(1)
                client.startConnection(object : InstallReferrerStateListener {
                    override fun onInstallReferrerSetupFinished(code: Int) {
                        try {
                            if (code == InstallReferrerClient.InstallReferrerResponse.OK) {
                                val resp = client.installReferrer
                                val raw = resp.installReferrer.orEmpty()
                                if (raw.isNotBlank()) {
                                    val info = ReferrerInfo(
                                        raw = raw,
                                        clickTsSec = resp.referrerClickTimestampSeconds,
                                        installBeginTsSec = resp.installBeginTimestampSeconds,
                                        params = SDDLQuery.parse(raw)
                                    )
                                    cache(context, info)
                                    result = info
                                }
                            }
                        } catch (_: Throwable) { /* ignore */ }
                        finally { latch.countDown() }
                    }
                    override fun onInstallReferrerServiceDisconnected() { /* no-op */ }
                })
                latch.await(2500, TimeUnit.MILLISECONDS)
            } catch (_: Throwable) {
                // ignore
            } finally {
                try { client.endConnection() } catch (_: Throwable) {}
                onDone(result)
            }
        }
    }

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun cache(ctx: Context, info: ReferrerInfo) {
        prefs(ctx).edit()
            .putString(KEY_RAW, info.raw)
            .putLong(KEY_CLICK, info.clickTsSec)
            .putLong(KEY_INSTALL, info.installBeginTsSec)
            .apply()
    }

    fun readCached(ctx: Context): ReferrerInfo? {
        val p = prefs(ctx)
        val raw = p.getString(KEY_RAW, null) ?: return null
        val click = p.getLong(KEY_CLICK, 0L)
        val install = p.getLong(KEY_INSTALL, 0L)
        return ReferrerInfo(raw, click, install, SDDLQuery.parse(raw))
    }

    fun markSent(ctx: Context) {
        prefs(ctx).edit().putBoolean(KEY_SENT, true).apply()
    }

    fun isSent(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_SENT, false)
}