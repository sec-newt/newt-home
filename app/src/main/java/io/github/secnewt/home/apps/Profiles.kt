package io.github.secnewt.home.apps

import androidx.compose.runtime.Immutable

/** Which part of the phone an app lives in. */
enum class ProfileKind(val title: String) {
    MAIN("Apps"),
    WORK("Work"),
    PRIVATE("Private space"),

    /** Another profile Android shares with the home app (like a clone profile). */
    OTHER("Other profile"),
}

/**
 * A work profile or Private space as All apps shows it: its apps, and whether it's locked
 * (Android calls this "quiet mode"; a locked Private space hides its apps).
 */
@Immutable
data class ProfileView(val kind: ProfileKind, val locked: Boolean, val apps: List<LauncherApp>)

object Profiles {

    /**
     * An app's key. Apps in the main profile keep their plain key (so favorites and folders
     * made before profiles existed still work); apps in another profile add its number, since
     * the same app can be installed in both.
     */
    fun key(component: String, serial: Long, main: Boolean): String = if (main) component else "$component#$serial"

    /** Private space apps stay out of search, favorites and folders: only its own section shows them. */
    fun searchable(kind: ProfileKind): Boolean = kind != ProfileKind.PRIVATE

    /** Sections in the order All apps lists them, after your own apps. */
    fun ordered(views: List<ProfileView>): List<ProfileView> =
        views.filter { it.kind != ProfileKind.MAIN }.sortedBy { it.kind.ordinal }
}
