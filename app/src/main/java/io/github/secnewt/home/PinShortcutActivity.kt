package io.github.secnewt.home

import android.content.pm.LauncherApps
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.secnewt.home.ui.PinShortcutSheet
import io.github.secnewt.home.ui.theme.HomeTheme

/**
 * Android opens this when an app asks to pin a shortcut to the home screen, like a browser's
 * "Add to home screen". It asks first; the shortcut then shows up in All apps and search.
 * Only the default home app is asked, and only the system can hand over a real request.
 */
class PinShortcutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launcherApps = getSystemService(LauncherApps::class.java)
        val request = try {
            launcherApps.getPinItemRequest(intent)
        } catch (e: RuntimeException) {
            null
        }
        val shortcut = request?.shortcutInfo
        if (request == null || request.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT || shortcut == null) {
            finish()
            return
        }
        val name = (shortcut.shortLabel ?: shortcut.longLabel)?.toString()?.trim().orEmpty().ifEmpty { "Shortcut" }
        val from = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(shortcut.`package`, 0)).toString()
        } catch (e: Exception) {
            shortcut.`package`
        }
        setContent {
            HomeTheme {
                PinShortcutSheet(
                    name = name,
                    from = from,
                    onAdd = {
                        val added = try {
                            request.isValid && request.accept()
                        } catch (e: RuntimeException) {
                            false
                        }
                        Toast.makeText(
                            this,
                            if (added) "Added $name to All apps" else "Couldn't add $name",
                            Toast.LENGTH_LONG,
                        ).show()
                        finish()
                    },
                    onCancel = ::finish,
                )
            }
        }
    }
}
