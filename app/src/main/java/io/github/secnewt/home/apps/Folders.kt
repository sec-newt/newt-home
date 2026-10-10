package io.github.secnewt.home.apps

import androidx.compose.runtime.Immutable

/** A folder on the home screen: a name and the keys of the apps in it, in the order added. */
@Immutable
data class Folder(val name: String, val keys: List<String> = emptyList())

/**
 * The home screen's folders, Niagara style: each app is in at most one folder, and folder
 * names are unique (ignoring capitals). Every change returns a new list.
 */
object Folders {

    /** The categories set up in Niagara, so the tree isn't empty the first time. */
    val STARTERS = listOf("AI", "Blind tools", "Games", "Productivity").map { Folder(it) }

    private const val MAX_NAME = 30

    /** A folder name as typed, tidied: no line breaks or tabs, single spaces, at most 30 characters. */
    fun cleanName(raw: String): String =
        raw.replace(Regex("\\s+"), " ").trim().take(MAX_NAME).trim()

    private fun List<Folder>.named(name: String) = indexOfFirst { it.name.equals(name, ignoreCase = true) }

    fun exists(folders: List<Folder>, name: String): Boolean = folders.named(cleanName(name)) >= 0

    /** Adds an empty folder at the end. A blank or already-used name changes nothing. */
    fun create(folders: List<Folder>, name: String): List<Folder> {
        val clean = cleanName(name)
        if (clean.isEmpty() || folders.named(clean) >= 0) return folders
        return folders + Folder(clean)
    }

    /** Renames a folder, keeping its apps. Changing only capitals is allowed. */
    fun rename(folders: List<Folder>, from: String, to: String): List<Folder> {
        val clean = cleanName(to)
        val i = folders.named(from)
        val clash = folders.named(clean)
        if (i < 0 || clean.isEmpty() || (clash >= 0 && clash != i)) return folders
        return folders.toMutableList().apply { this[i] = this[i].copy(name = clean) }
    }

    /** Deletes a folder. Its apps aren't touched; they just aren't in a folder any more. */
    fun delete(folders: List<Folder>, name: String): List<Folder> {
        val i = folders.named(name)
        return if (i < 0) folders else folders.toMutableList().apply { removeAt(i) }
    }

    /** Moves a folder up (-1) or down (+1) in the tree; stays put at either end. */
    fun move(folders: List<Folder>, name: String, by: Int): List<Folder> {
        val from = folders.named(name)
        val to = from + by
        if (from < 0 || to !in folders.indices) return folders
        return folders.toMutableList().apply { add(to, removeAt(from)) }
    }

    /** Puts an app in a folder (taking it out of any other), or in no folder when [to] is null. */
    fun moveApp(folders: List<Folder>, key: String, to: String?): List<Folder> {
        val target = to?.let { folders.named(it) } ?: -1
        return folders.mapIndexed { i, f ->
            val without = f.keys - key
            if (i == target) f.copy(keys = without + key) else f.copy(keys = without)
        }
    }

    fun folderOf(folders: List<Folder>, key: String): String? = folders.firstOrNull { key in it.keys }?.name

    /** One folder per line: its name, then its app keys, separated by tabs (names can't hold either). */
    fun encode(folders: List<Folder>): String =
        folders.joinToString("\n") { (listOf(it.name) + it.keys).joinToString("\t") }

    fun decode(text: String): List<Folder> =
        text.split('\n').filter { it.isNotBlank() }.map { line ->
            val parts = line.split('\t')
            Folder(parts.first(), parts.drop(1).filter { it.isNotEmpty() })
        }
}
