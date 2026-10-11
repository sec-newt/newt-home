package io.github.secnewt.home.apps

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * One app as the home screen shows it. [icon] is the app's one-color icon, which the screen
 * tints in Newt colors; apps that don't ship one have no icon and get a letter tile instead.
 */
@Immutable
data class LauncherApp(
    /** Stays the same while the app is installed; used to remember favorites and how often it's opened. */
    val key: String,
    val name: String,
    val icon: ImageBitmap? = null,
    /**
     * For a shortcut (like a website added from the browser's "Add to home screen"), the
     * app it came from, such as "Vanadium". Null for ordinary apps.
     */
    val from: String? = null,
) {
    val isShortcut: Boolean get() = from != null
}

/** The home screen's favorites, as an ordered list of app keys. */
object Favorites {

    fun toggle(favorites: List<String>, key: String): List<String> =
        if (key in favorites) favorites - key else favorites + key

    /** Moves an app up (-1) or down (+1); stays put at either end. */
    fun move(favorites: List<String>, key: String, by: Int): List<String> {
        val from = favorites.indexOf(key)
        val to = from + by
        if (from < 0 || to !in favorites.indices) return favorites
        return favorites.toMutableList().apply { add(to, removeAt(from)) }
    }
}
