package io.github.secnewt.home.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

/**
 * Watches the battery and the connection type for the status line, while the home screen
 * is showing. The battery needs no permission; the connection type needs "view network
 * connections", which only says whether the phone is on Wi-Fi or mobile data. The launcher
 * still has no internet access of its own.
 */
class DeviceWatcher(private val context: Context, private val onChange: (DeviceStatus) -> Unit) {

    private var status = DeviceStatus()
    private val main = Handler(Looper.getMainLooper())
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    private val battery = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
            if (level >= 0 && scale > 0) update(status.copy(battery = level * 100 / scale, charging = plugged))
        }
    }

    private val network = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            val type = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Connection.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Connection.MOBILE
                // A VPN or anything else still counts as connected; show nothing rather than guess.
                else -> Connection.UNKNOWN
            }
            update(status.copy(connection = type))
        }

        override fun onLost(network: Network) = update(status.copy(connection = Connection.OFFLINE))
    }

    private var watching = false

    fun start() {
        if (watching) return
        watching = true
        // The battery broadcast is sent by Android itself; NOT_EXPORTED keeps other apps from faking it.
        ContextCompat.registerReceiver(
            context,
            battery,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        if (connectivity.activeNetwork == null) update(status.copy(connection = Connection.OFFLINE))
        connectivity.registerDefaultNetworkCallback(network, main)
    }

    fun stop() {
        if (!watching) return
        watching = false
        context.unregisterReceiver(battery)
        connectivity.unregisterNetworkCallback(network)
    }

    private fun update(new: DeviceStatus) {
        if (new == status) return
        status = new
        main.post { onChange(new) }
    }
}
