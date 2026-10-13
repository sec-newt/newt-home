package io.github.secnewt.home.share

import android.content.ComponentName
import android.content.Context
import android.content.pm.PermissionInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.secnewt.home.apps.HomeStore
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class LookProviderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun read(): String {
        val provider = Robolectric.buildContentProvider(LookProvider::class.java).create("${context.packageName}.look").get()
        return provider.query(LookProvider.uri(context), null, null, null, null)!!.use {
            it.moveToFirst()
            it.getString(0)
        }
    }

    @Test
    fun terminalUntilSwitched() = assertEquals("TERMINAL", read())

    @Test
    fun cleanAfterLookClean() {
        HomeStore(context).setLook("CLEAN")
        assertEquals("CLEAN", read())
    }

    /** Sharing rests on these manifest lines, so they're checked. */
    @Test
    fun onlyNewtSignedAppsCanRead() {
        val permission = "${context.packageName}.permission.READ_LOOK"
        assertEquals(PermissionInfo.PROTECTION_SIGNATURE, context.packageManager.getPermissionInfo(permission, 0).protection)
        val provider = context.packageManager.getProviderInfo(ComponentName(context, LookProvider::class.java), 0)
        assertEquals(permission, provider.readPermission)
        assertEquals(permission, provider.writePermission)
    }
}
