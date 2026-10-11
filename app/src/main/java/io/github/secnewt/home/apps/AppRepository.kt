package io.github.secnewt.home.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.os.UserManager
import android.os.UserHandle
import androidx.compose.ui.graphics.asImageBitmap

/**
 * An installed app, with what Android needs to open it. For a pinned shortcut, [shortcut]
 * is set and [component] is the app it belongs to.
 */
class InstalledApp(
    val app: LauncherApp,
    val component: ComponentName,
    val user: UserHandle,
    val shortcut: ShortcutInfo? = null,
)

/** A work profile or Private space the home app can see, and whether it's locked. */
class ProfileState(val user: UserHandle, val kind: ProfileKind, val locked: Boolean)

/** Everything the home screen lists: apps (and shortcuts) from every profile, and the profiles. */
class Loaded(val apps: List<InstalledApp>, val profiles: List<ProfileState>)

/**
 * Lists the apps that can be opened from a home screen, in your own profile and in any work
 * profile or Private space Android shares with the home app. Android gives these lists to
 * launchers directly, so no permission is needed (Private space needs Newt Home to be the
 * default home app).
 */
class AppRepository(private val context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val users = context.getSystemService(UserManager::class.java)

    fun load(): Loaded {
        val me = Process.myUserHandle()
        val profiles = try {
            launcherApps.profiles
        } catch (e: RuntimeException) {
            listOf(me)
        }
        val apps = loadApps(me, ProfileKind.MAIN)
        val others = profiles.filter { it != me }.map { user ->
            val kind = kindOf(user)
            val locked = try {
                users.isQuietModeEnabled(user)
            } catch (e: RuntimeException) {
                false
            }
            ProfileState(user, kind, locked) to if (locked) emptyList() else loadApps(user, kind)
        }
        return Loaded(apps + loadShortcuts(apps) + others.flatMap { it.second }, others.map { it.first })
    }

    /** Work profile, Private space or something else; Android 15 says which, older ones only have work profiles. */
    private fun kindOf(user: UserHandle): ProfileKind {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val type = try {
                launcherApps.getLauncherUserInfo(user)?.userType
            } catch (e: RuntimeException) {
                null
            }
            return when (type) {
                // Android's own names for these profile types.
                "android.os.usertype.profile.MANAGED" -> ProfileKind.WORK
                "android.os.usertype.profile.PRIVATE" -> ProfileKind.PRIVATE
                null -> ProfileKind.WORK
                else -> ProfileKind.OTHER
            }
        }
        return ProfileKind.WORK
    }

    private fun loadApps(user: UserHandle, kind: ProfileKind): List<InstalledApp> {
        val main = kind == ProfileKind.MAIN
        val serial = if (main) 0L else users.getSerialNumberForUser(user)
        val list = try {
            launcherApps.getActivityList(null, user)
        } catch (e: RuntimeException) {
            // The profile was removed or locked a moment ago.
            return emptyList()
        }
        return list
            .filter { !main || it.applicationInfo.packageName != context.packageName }
            .map { info ->
                val component = info.componentName
                val mono = (info.getIcon(0) as? AdaptiveIconDrawable)?.monochrome
                InstalledApp(
                    app = LauncherApp(
                        key = Profiles.key(component.flattenToString(), serial, main),
                        name = info.label.toString().ifBlank { component.packageName },
                        icon = mono?.let { safeZone(it).asImageBitmap() },
                        profile = kind,
                    ),
                    component = component,
                    user = user,
                )
            }
    }

    /**
     * Shortcuts pinned to the home screen, like websites from the browser's "Add to home
     * screen". Android only shares these with the default home app; otherwise none.
     */
    private fun loadShortcuts(apps: List<InstalledApp>): List<InstalledApp> {
        val user = Process.myUserHandle()
        val pinned = try {
            if (!launcherApps.hasShortcutHostPermission()) return emptyList()
            launcherApps.getShortcuts(ShortcutQuery().setQueryFlags(ShortcutQuery.FLAG_MATCH_PINNED), user).orEmpty()
        } catch (e: RuntimeException) {
            // Not the default home app (SecurityException), or the user is locked.
            return emptyList()
        }
        val byPackage = apps.associateBy { it.component.packageName }
        return pinned.filter { it.isEnabled }.mapNotNull { info ->
            val source = byPackage[info.`package`] ?: return@mapNotNull null
            val label = (info.shortLabel ?: info.longLabel)?.toString()?.trim().orEmpty()
            InstalledApp(
                app = LauncherApp(
                    key = Shortcuts.key(info.`package`, info.id),
                    name = label.ifEmpty { source.app.name },
                    // Shortcut icons are full-color pictures; a letter tile keeps the Newt look.
                    icon = null,
                    from = source.app.name,
                ),
                component = info.activity ?: source.component,
                user = user,
                shortcut = info,
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
