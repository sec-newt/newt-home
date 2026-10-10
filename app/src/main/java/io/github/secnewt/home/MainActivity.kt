package io.github.secnewt.home

import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import io.github.secnewt.home.apps.AppRepository
import io.github.secnewt.home.apps.AppSearch
import io.github.secnewt.home.apps.Folder
import io.github.secnewt.home.apps.HomeStore
import io.github.secnewt.home.apps.InstalledApp
import io.github.secnewt.home.apps.LauncherApp
import io.github.secnewt.home.ui.HomeRoute
import io.github.secnewt.home.ui.theme.NewtTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var launcherApps: LauncherApps
    private lateinit var store: HomeStore

    private var installed by mutableStateOf<List<InstalledApp>>(emptyList())
    private var favoriteKeys by mutableStateOf<List<String>>(emptyList())
    private var folders by mutableStateOf<List<Folder>>(emptyList())
    private var openFolder by mutableStateOf<String?>(null)
    private var uses by mutableStateOf<Map<String, Int>>(emptyMap())
    private var resetSignal by mutableIntStateOf(0)

    // Goes up each time the home screen comes back into view, which replays the boot type-out.
    private var bootKey by mutableIntStateOf(0)
    private var animateBoot by mutableStateOf(true)

    /** Android tells the home app when apps are installed, updated or removed. */
    private val packageChanges = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = reload()
        override fun onPackageAdded(packageName: String, user: UserHandle) = reload()
        override fun onPackageChanged(packageName: String, user: UserHandle) = reload()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        launcherApps = getSystemService(LauncherApps::class.java)
        store = HomeStore(this)
        favoriteKeys = store.favorites()
        folders = store.folders()
        openFolder = store.openFolder()
        uses = store.uses()
        launcherApps.registerCallback(packageChanges, Handler(Looper.getMainLooper()))
        reload()

        setContent {
            NewtTheme {
                HomeRoute(
                    apps = installed.map { it.app },
                    favoriteKeys = favoriteKeys,
                    folders = folders,
                    openFolder = openFolder,
                    uses = uses,
                    resetSignal = resetSignal,
                    bootKey = bootKey,
                    animateBoot = animateBoot,
                    onOpen = ::open,
                    onFavoritesChange = {
                        favoriteKeys = it
                        store.setFavorites(it)
                    },
                    onFoldersChange = {
                        folders = it
                        store.setFolders(it)
                    },
                    onOpenFolderChange = {
                        openFolder = it
                        store.setOpenFolder(it)
                    },
                    onAppInfo = ::showAppInfo,
                    onClock = ::openClock,
                    onDate = ::openCalendar,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Android's "Remove animations" sets the animator scale to 0; then nothing types out.
        animateBoot = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
        bootKey++
    }

    override fun onDestroy() {
        launcherApps.unregisterCallback(packageChanges)
        super.onDestroy()
    }

    /** Pressing Home while the home screen is showing returns it to its resting state. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        resetSignal++
    }

    private fun reload() {
        lifecycleScope.launch {
            installed = withContext(Dispatchers.IO) { AppRepository(this@MainActivity).load() }
        }
    }

    private fun find(app: LauncherApp): InstalledApp? = installed.firstOrNull { it.app.key == app.key }

    private fun open(app: LauncherApp) {
        val target = find(app) ?: return
        try {
            launcherApps.startMainActivity(target.component, target.user, null, null)
            store.recordUse(app.key)
            uses = store.uses()
        } catch (e: RuntimeException) {
            // The app was removed or disabled a moment ago, or refuses to open.
            say("Couldn't open ${app.name}")
        }
    }

    private fun showAppInfo(app: LauncherApp) {
        val target = find(app) ?: return
        try {
            launcherApps.startAppDetailsActivity(target.component, target.user, null, null)
        } catch (e: RuntimeException) {
            say("Couldn't open app info for ${app.name}")
        }
    }

    /** The phone's clock app; if it won't open its alarms page, the app named Clock. */
    private fun openClock() {
        if (tryStart(Intent(AlarmClock.ACTION_SHOW_ALARMS))) return
        if (!openByName("Clock")) say("No clock app found")
    }

    /**
     * The calendar at today. GrapheneOS has no calendar app built in, so this needs one
     * installed (for example Etar from F-Droid).
     */
    private fun openCalendar() {
        val today: Uri = CalendarContract.CONTENT_URI.buildUpon()
            .appendPath("time")
            .appendPath(System.currentTimeMillis().toString())
            .build()
        if (tryStart(Intent(Intent.ACTION_VIEW, today))) return
        if (!openByName("Calendar") && !openByName("Etar")) say("No calendar app found. Etar on F-Droid is one option.")
    }

    private fun tryStart(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (e: RuntimeException) {
        // No app handles it (ActivityNotFoundException), or it needs a permission we don't have.
        false
    }

    private fun openByName(name: String): Boolean {
        val app = installed.firstOrNull { AppSearch.normalize(it.app.name) == AppSearch.normalize(name) } ?: return false
        open(app.app)
        return true
    }

    private fun say(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
