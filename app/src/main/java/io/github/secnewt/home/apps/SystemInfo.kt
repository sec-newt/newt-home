package io.github.secnewt.home.apps

import androidx.compose.runtime.Immutable

/**
 * What `sys` shows, like the Linux `neofetch` command: the phone, its software and how full
 * it is. All of it comes from Android without any permission.
 */
@Immutable
data class SystemInfo(
    val user: String,
    val device: String,
    val android: String,
    val build: String,
    val kernel: String,
    val uptimeMillis: Long,
    val apps: Int,
    val storageUsed: Long,
    val storageTotal: Long,
    val memoryUsed: Long,
    val memoryTotal: Long,
    val battery: String?,
    val launcher: String,
) {
    /** The rows, label first, in neofetch's order. */
    fun rows(): List<Pair<String, String>> = listOfNotNull(
        "os" to android,
        "build" to build,
        "host" to device,
        "kernel" to kernel,
        "uptime" to SystemInfoText.uptime(uptimeMillis),
        "apps" to apps.toString(),
        "storage" to SystemInfoText.usage(storageUsed, storageTotal),
        "memory" to SystemInfoText.usage(memoryUsed, memoryTotal),
        battery?.let { "battery" to it },
        "theme" to "Tokyo Night",
        "launcher" to launcher,
    )
}

object SystemInfoText {

    /** "20d 11h", "3h 5m", "12m". */
    fun uptime(millis: Long): String {
        val minutes = (millis / 60_000).coerceAtLeast(0)
        val days = minutes / (24 * 60)
        val hours = minutes / 60 % 24
        val mins = minutes % 60
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${mins}m"
            else -> "${mins}m"
        }
    }

    /** "157G / 228G (69%)", in whole gigabytes (megabytes below 1 GB). */
    fun usage(used: Long, total: Long): String {
        if (total <= 0) return "unknown"
        val percent = (used * 100 / total).coerceIn(0, 100)
        return "${size(used)} / ${size(total)} ($percent%)"
    }

    private fun size(bytes: Long): String {
        val gb = bytes / 1_000_000_000.0
        return if (gb >= 1) "${Math.round(gb)}G" else "${bytes / 1_000_000}M"
    }
}
