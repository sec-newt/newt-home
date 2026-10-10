package io.github.secnewt.home.apps

import androidx.compose.runtime.Immutable

/** How the phone is connected, for the status line. */
enum class Connection { WIFI, MOBILE, OFFLINE, UNKNOWN }

/** What the terminal status line shows in place of Android's status bar. */
@Immutable
data class DeviceStatus(
    /** Battery percent, or null until Android has reported it. */
    val battery: Int? = null,
    val charging: Boolean = false,
    val connection: Connection = Connection.UNKNOWN,
    /** Shown while the flashlight is on, so it's never left on by accident. */
    val flashlight: Boolean = false,
) {
    val batteryLow: Boolean get() = battery != null && battery <= LOW_BATTERY && !charging

    /** The pieces of the line, like `[bat 54%]` and `[wifi ●]`, label first. */
    fun parts(): List<Pair<String, String>> = buildList {
        if (battery != null) {
            val detail = when {
                charging -> " charging"
                batteryLow -> " low"
                else -> ""
            }
            add("bat" to "$battery%$detail")
        }
        when (connection) {
            Connection.WIFI -> add("wifi" to "●")
            Connection.MOBILE -> add("mobile" to "●")
            Connection.OFFLINE -> add("net" to "offline")
            Connection.UNKNOWN -> Unit
        }
        if (flashlight) add("light" to "on")
    }

    /** The same, as a screen reader should say it. */
    fun spoken(): String = buildList {
        if (battery != null) {
            add(
                "Battery $battery percent" + when {
                    charging -> ", charging"
                    batteryLow -> ", low"
                    else -> ""
                },
            )
        }
        when (connection) {
            Connection.WIFI -> add("on Wi-Fi")
            Connection.MOBILE -> add("on mobile data")
            Connection.OFFLINE -> add("offline")
            Connection.UNKNOWN -> Unit
        }
        if (flashlight) add("flashlight on")
    }.joinToString(", ")

    companion object {
        const val LOW_BATTERY = 15
    }
}
