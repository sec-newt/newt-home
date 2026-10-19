package io.github.secnewt.home.apps

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri

/**
 * How many messages Newt Messages says are unread. Newt Messages shares only that number,
 * and only with apps signed with the Newt key, so Newt Home never sees a message. Null when
 * Newt Messages isn't installed or isn't sharing.
 */
class UnreadMessages(private val context: Context) {

    fun count(): Int? {
        if (context.checkSelfPermission(PERMISSION) != PackageManager.PERMISSION_GRANTED) return null
        return try {
            context.contentResolver.query(URI, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0).coerceIn(0, 999) else null
            }
        } catch (e: RuntimeException) {
            // Newt Messages is missing, too old to share, or its provider failed.
            null
        }
    }

    companion object {
        const val NEWT_MESSAGES = "io.github.secnewt.messages"
        const val PERMISSION = "$NEWT_MESSAGES.permission.UNREAD_COUNT"
        // Built on first use, so the plain-Kotlin tests can use the rest without Android.
        val URI: Uri by lazy { Uri.parse("content://$NEWT_MESSAGES.unread/count") }

        /** "3 unread" under Messages; nothing when there are none. */
        fun label(count: Int?): String? = count?.takeIf { it > 0 }?.let { "$it unread" }

        /** Whether [key] (an app's key) is Newt Messages, which gets the label. */
        fun isNewtMessages(key: String): Boolean = key.startsWith("$NEWT_MESSAGES/")
    }
}
