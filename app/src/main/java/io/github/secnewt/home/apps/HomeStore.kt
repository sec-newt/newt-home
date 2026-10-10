package io.github.secnewt.home.apps

import android.content.Context

/**
 * What the home screen remembers: favorites and how often each app is opened (to rank
 * search results). Kept in the app's private storage, which is excluded from backups.
 */
class HomeStore(context: Context) {

    private val prefs = context.getSharedPreferences("home", Context.MODE_PRIVATE)

    fun favorites(): List<String> =
        prefs.getString(FAVORITES, "").orEmpty().split('\n').filter { it.isNotEmpty() }

    fun setFavorites(keys: List<String>) {
        prefs.edit().putString(FAVORITES, keys.joinToString("\n")).apply()
    }

    fun uses(): Map<String, Int> =
        prefs.all.mapNotNull { (k, v) ->
            if (k.startsWith(USES) && v is Int) k.removePrefix(USES) to v else null
        }.toMap()

    fun recordUse(key: String) {
        prefs.edit().putInt(USES + key, prefs.getInt(USES + key, 0) + 1).apply()
    }

    private companion object {
        const val FAVORITES = "favorites"
        const val USES = "uses:"
    }
}
