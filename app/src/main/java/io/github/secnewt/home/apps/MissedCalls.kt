package io.github.secnewt.home.apps

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri

/**
 * How many missed calls Newt Phone says you haven't seen. Newt Phone shares only that
 * number, and only with apps signed with the Newt key, so Newt Home never needs access to
 * the call history. Null when Newt Phone isn't installed or isn't sharing.
 */
class MissedCalls(private val context: Context) {

    fun count(): Int? {
        if (context.checkSelfPermission(PERMISSION) != PackageManager.PERMISSION_GRANTED) return null
        return try {
            context.contentResolver.query(URI, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0).coerceIn(0, 999) else null
            }
        } catch (e: RuntimeException) {
            // Newt Phone is missing, too old to share, or its provider failed.
            null
        }
    }

    companion object {
        const val NEWT_PHONE = "io.github.secnewt.dialer"
        const val PERMISSION = "$NEWT_PHONE.permission.MISSED_CALL_COUNT"
        val URI: Uri = Uri.parse("content://$NEWT_PHONE.missedcalls/count")

        /** "2 missed" under Phone; nothing when there are none. */
        fun label(count: Int?): String? = count?.takeIf { it > 0 }?.let { "$it missed" }

        /** Whether [key] (an app's key) is Newt Phone, which gets the label. */
        fun isNewtPhone(key: String): Boolean = key.startsWith("$NEWT_PHONE/")
    }
}
