package io.github.secnewt.home.apps

/** Keys for pinned shortcuts, kept apart from app keys so favorites and folders can hold both. */
object Shortcuts {

    private const val PREFIX = "shortcut:"

    fun key(packageName: String, id: String): String = "$PREFIX$packageName/$id"

    fun isShortcutKey(key: String): Boolean = key.startsWith(PREFIX)

    /** The package and shortcut id in a key from [key], or null for an app's key. */
    fun parse(key: String): Pair<String, String>? {
        if (!isShortcutKey(key)) return null
        val rest = key.removePrefix(PREFIX)
        val slash = rest.indexOf('/')
        if (slash <= 0 || slash == rest.lastIndex) return null
        return rest.substring(0, slash) to rest.substring(slash + 1)
    }
}
