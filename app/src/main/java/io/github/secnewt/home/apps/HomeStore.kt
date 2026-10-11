package io.github.secnewt.home.apps

import android.content.Context

/**
 * What the home screen remembers: favorites, folders, the folder open last, and how often
 * each app is opened (to rank search results). Kept in the app's private storage, which is
 * excluded from backups.
 */
class HomeStore(context: Context) {

    private val prefs = context.getSharedPreferences("home", Context.MODE_PRIVATE)

    fun favorites(): List<String> =
        prefs.getString(FAVORITES, "").orEmpty().split('\n').filter { it.isNotEmpty() }

    fun setFavorites(keys: List<String>) {
        prefs.edit().putString(FAVORITES, keys.joinToString("\n")).apply()
    }

    /** The folders, or the starter set (AI, Blind tools, Games, Productivity) the first time. */
    fun folders(): List<Folder> =
        prefs.getString(FOLDERS, null)?.let { Folders.decode(it) } ?: Folders.STARTERS

    fun setFolders(folders: List<Folder>) {
        prefs.edit().putString(FOLDERS, Folders.encode(folders)).apply()
    }

    /** The folder left open last time, so it's still open next time. */
    fun openFolder(): String? = prefs.getString(OPEN_FOLDER, null)

    fun setOpenFolder(name: String?) {
        prefs.edit().putString(OPEN_FOLDER, name).apply()
    }

    /** Whether today's events show under the date: true, false, or null if not asked yet. */
    fun showEvents(): Boolean? = if (prefs.contains(SHOW_EVENTS)) prefs.getBoolean(SHOW_EVENTS, false) else null

    fun setShowEvents(show: Boolean?) {
        prefs.edit().apply { if (show == null) remove(SHOW_EVENTS) else putBoolean(SHOW_EVENTS, show) }.apply()
    }

    /** Apps hidden from All apps and folders. Search still finds them. */
    fun hidden(): Set<String> =
        prefs.getString(HIDDEN, "").orEmpty().split('\n').filter { it.isNotEmpty() }.toSet()

    fun setHidden(keys: Set<String>) {
        prefs.edit().putString(HIDDEN, keys.joinToString("\n")).apply()
    }

    /** Your own search words for apps, by app key. */
    fun searchWords(): Map<String, List<String>> = SearchWords.decode(prefs.getString(SEARCH_WORDS, "").orEmpty())

    fun setSearchWords(words: Map<String, List<String>>) {
        prefs.edit().putString(SEARCH_WORDS, SearchWords.encode(words)).apply()
    }

    /** Websites added by hand. */
    fun websites(): List<Website> = Websites.decode(prefs.getString(WEBSITES, "").orEmpty())

    fun setWebsites(sites: List<Website>) {
        prefs.edit().putString(WEBSITES, Websites.encode(sites)).apply()
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
        const val FOLDERS = "folders"
        const val OPEN_FOLDER = "open_folder"
        const val SHOW_EVENTS = "show_events"
        const val HIDDEN = "hidden"
        const val SEARCH_WORDS = "search_words"
        const val WEBSITES = "websites"
        const val USES = "uses:"
    }
}
