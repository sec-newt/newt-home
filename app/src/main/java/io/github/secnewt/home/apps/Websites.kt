package io.github.secnewt.home.apps

import java.net.URI

/** A website added by hand: a name and an address, opened in your browser. */
data class Website(val id: String, val name: String, val url: String) {

    val key: String get() = Websites.key(id)

    /** "example.org" from "https://www.example.org/path", for the line under its name. */
    val host: String get() = Websites.host(url) ?: url

    fun toApp(): LauncherApp = LauncherApp(key = key, name = name, url = url)
}

/** Websites added by hand, kept as one line each: id, name and address, separated by tabs. */
object Websites {

    private const val PREFIX = "web:"
    const val MAX_NAME = 40

    fun key(id: String): String = "$PREFIX$id"

    fun isKey(key: String): Boolean = key.startsWith(PREFIX)

    /**
     * A usable web address from what was typed, or null: "example.org" becomes
     * "https://example.org". Only http and https, and the name must look like a real site.
     */
    fun normalize(typed: String): String? {
        val text = typed.trim()
        if (text.isEmpty() || text.any { it.isWhitespace() }) return null
        val withScheme = if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(text)) text else "https://$text"
        val uri = try {
            URI(withScheme)
        } catch (e: Exception) {
            return null
        }
        val scheme = uri.scheme?.lowercase()
        if (scheme != "https" && scheme != "http") return null
        val host = uri.host ?: return null
        if ('.' !in host || host.startsWith('.') || host.endsWith('.')) return null
        return withScheme
    }

    fun host(url: String): String? = try {
        URI(url).host?.removePrefix("www.")
    } catch (e: Exception) {
        null
    }

    /** A name that fits on one line; the site's host when nothing was typed. */
    fun cleanName(typed: String, url: String): String =
        typed.trim().replace(Regex("\\s+"), " ").take(MAX_NAME).ifEmpty { host(url) ?: url }

    fun encode(sites: List<Website>): String =
        sites.joinToString("\n") { listOf(it.id, it.name, it.url).joinToString("\t") }

    fun decode(text: String): List<Website> =
        text.split('\n').mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size != 3 || parts.any { it.isBlank() }) return@mapNotNull null
            val url = normalize(parts[2]) ?: return@mapNotNull null
            Website(parts[0], parts[1], url)
        }
}
