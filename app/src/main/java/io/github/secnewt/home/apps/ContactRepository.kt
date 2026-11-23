package io.github.secnewt.home.apps

import android.content.Context
import android.provider.ContactsContract.CommonDataKinds.Phone

/**
 * Reads names and phone numbers from the phone's own contacts, for People in search. Only
 * runs once people is switched on and contact access is given. Nothing is saved, and with no
 * internet nothing leaves the phone. This list is your own profile's contacts only, so work
 * profile and Private space contacts are left out.
 */
class ContactRepository(private val context: Context) {

    fun people(): List<Person> = try {
        val projection = arrayOf(Phone.CONTACT_ID, Phone.LOOKUP_KEY, Phone.DISPLAY_NAME_PRIMARY, Phone.NUMBER, Phone.TYPE, Phone.LABEL, Phone.DISPLAY_NAME_SOURCE)
        val rows = mutableListOf<ContactRow>()
        context.contentResolver.query(Phone.CONTENT_URI, projection, null, null, "${Phone.DISPLAY_NAME_PRIMARY} COLLATE NOCASE")?.use { c ->
            while (c.moveToNext()) {
                rows += ContactRow(
                    id = c.getLong(0),
                    lookupKey = c.getString(1).orEmpty(),
                    name = c.getString(2),
                    number = c.getString(3),
                    type = c.getInt(4),
                    label = c.getString(5),
                    nameSource = c.getInt(6),
                )
            }
        }
        People.fromRows(rows)
    } catch (e: RuntimeException) {
        // Access was taken away mid-read (SecurityException), or no contacts storage.
        emptyList()
    }
}
