package io.github.secnewt.home.share

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import io.github.secnewt.home.apps.HomeStore
import io.github.secnewt.home.ui.HomeLook

/**
 * Shares Newt Home's look (TERMINAL or CLEAN) with apps signed with the Newt key, so Newt
 * Weather and Newt Phone follow `look terminal` / `look clean`. One row, one column, read-only,
 * guarded by a signature permission nobody is asked for. Newt Home is the only switch.
 */
class LookProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        val context = context ?: return null
        if (uri.lastPathSegment != PATH) return null
        return MatrixCursor(arrayOf(COLUMN)).apply {
            addRow(arrayOf(HomeLook.named(HomeStore(context).look()).name))
            setNotificationUri(context.contentResolver, uri(context))
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        const val PATH = "look"
        const val COLUMN = "look"

        fun uri(context: Context): Uri = Uri.parse("content://${context.packageName}.look/$PATH")

        /** Tells the other Newt apps the look changed, so they switch right away. */
        fun changed(context: Context) = context.contentResolver.notifyChange(uri(context), null)
    }
}
