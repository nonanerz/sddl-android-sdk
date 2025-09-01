package com.simplelink.sddl_sdk

import android.net.Uri

internal object SDDLQuery {
    fun parse(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        val out = LinkedHashMap<String, String>()
        var start = 0
        val s = raw
        while (start <= s.length) {
            val amp = s.indexOf('&', start).let { if (it == -1) s.length else it }
            val pair = s.substring(start, amp)
            if (pair.isNotEmpty()) {
                val eq = pair.indexOf('=')
                val name: String
                val value: String
                if (eq >= 0) {
                    name = Uri.decode(pair.substring(0, eq))
                    value = Uri.decode(pair.substring(eq + 1))
                } else {
                    name = Uri.decode(pair)
                    value = ""
                }
                if (name.isNotEmpty() && !out.containsKey(name)) {
                    out[name] = value
                }
            }
            if (amp == s.length) break
            start = amp + 1
        }
        return out
    }
}