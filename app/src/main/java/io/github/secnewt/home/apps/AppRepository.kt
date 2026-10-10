package io.github.secnewt.home.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import androidx.compose.ui.graphics.asImageBitmap

/** An installed app, with what Android needs to open it. */
class InstalledApp(val app: LauncherApp, val component: ComponentName, val user: UserHandle)

/**
 * Lists the apps that can be opened from a home screen. Android gives this list to launchers
 * directly, so no permission is needed. Work profile and Private Space apps come later.
 */
class AppRepository(private val context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    fun load(): List<InstalledApp> {
        val user = Process.myUserHandle()
        return launcherApps.getActivityList(null, user)
            .filter { it.applicationInfo.packageName != context.packageName }
            .map { info ->
                val component = info.componentName
                val mono = (info.getIcon(0) as? AdaptiveIconDrawable)?.monochrome
                InstalledApp(
                    app = LauncherApp(
                        key = component.flattenToString(),
                        name = info.label.toString().ifBlank { component.packageName },
                        icon = mono?.let { safeZone(it).asImageBitmap() },
                    ),
                    component = component,
                    user = user,
                )
            }
    }

    /**
     * Draws only the middle of an adaptive icon layer (the part that is always visible,
     * 72 of its 108dp), so the shape fills the space it's given.
     */
    private fun safeZone(layer: Drawable, size: Int = ICON_PX): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val extra = size / 4
        layer.mutate().setBounds(-extra, -extra, size + extra, size + extra)
        layer.draw(Canvas(bitmap))
        return bitmap
    }

    private companion object {
        const val ICON_PX = 96
    }
}
