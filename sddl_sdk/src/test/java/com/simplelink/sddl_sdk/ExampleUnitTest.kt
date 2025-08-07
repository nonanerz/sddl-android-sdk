package com.simplelink.sddl_sdk

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    private fun invokeExtractIdentifier(context: Context, uri: Uri?): String? {
        val method = SDDLSDK::class.java.getDeclaredMethod(
            "extractIdentifier",
            Context::class.java,
            Uri::class.java
        )
        method.isAccessible = true
        return method.invoke(SDDLSDK, context, uri) as String?
    }

    @Test
    fun extractIdentifier_returnsSegmentFromUri() {
        val context = Mockito.mock(Context::class.java)
        val uri = Uri.parse("https://example.com/validId")

        val result = invokeExtractIdentifier(context, uri)

        assertEquals("validId", result)
    }

    @Test
    fun extractIdentifier_invalidInputReturnsNull() {
        val context = Mockito.mock(Context::class.java)
        val clipboard = Mockito.mock(ClipboardManager::class.java)
        val clipData = ClipData.newPlainText("label", "bad!")

        Mockito.`when`(context.getSystemService(Context.CLIPBOARD_SERVICE)).thenReturn(clipboard)
        Mockito.`when`(clipboard.primaryClip).thenReturn(clipData)

        val uri = Uri.parse("https://example.com/!!!")

        val result = invokeExtractIdentifier(context, uri)

        assertNull(result)
    }
}
