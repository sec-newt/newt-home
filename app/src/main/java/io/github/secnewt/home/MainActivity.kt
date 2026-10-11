package io.github.secnewt.home

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.database.ContentObserver
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import io.github.secnewt.home.apps.Agenda
import io.github.secnewt.home.apps.AppRepository
import io.github.secnewt.home.apps.CalendarRepository
import io.github.secnewt.home.apps.Command
import io.github.secnewt.home.apps.Commands
import io.github.secnewt.home.apps.DayEvent
import io.github.secnewt.home.apps.AppSearch
import io.github.secnewt.home.apps.DeviceStatus
import io.github.secnewt.home.apps.DeviceWatcher
import io.github.secnewt.home.apps.Folder
import io.github.secnewt.home.apps.HomeStore
import io.github.secnewt.home.apps.InstalledApp
import io.github.secnewt.home.apps.LauncherApp
import io.github.secnewt.home.apps.AppShortcut
import io.github.secnewt.home.apps.Website
import java.util.UUID
import io.github.secnewt.home.ui.HomeRoute
import io.github.secnewt.home.ui.theme.NewtTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The most of an app's own shortcuts shown on its press-and-hold menu. */
private const val MAX_APP_SHORTCUTS = 5

/** Extra on a dial intent with a name to look up, for Newt Phone. Other phone apps ignore it. */
const val EXTRA_CONTACT_QUERY = "io.github.secnewt.extra.CONTACT_QUERY"

class MainActivity : ComponentActivity() {

    private lateinit var launcherApps: LauncherApps
    private lateinit var store: HomeStore

    private var installed by mutableStateOf<List<InstalledApp>>(emptyList())
    private var favoriteKeys by mutableStateOf<List<String>>(emptyList())
    private var folders by mutableStateOf<List<Folder>>(emptyList())
    private var openFolder by mutableStateOf<String?>(null)
    private var uses by mutableStateOf<Map<String, Int>>(emptyMap())
    private var hidden by mutableStateOf<Set<String>>(emptySet())
    private var searchWords by mutableStateOf<Map<String, List<String>>>(emptyMap())
    private var websites by mutableStateOf<List<Website>>(emptyList())
    private var resetSignal by mutableIntStateOf(0)

    // Goes up each time the home screen comes back into view, which replays the boot type-out.
    private var bootKey by mutableIntStateOf(0)
    private var animateBoot by mutableStateOf(true)

    private var status by mutableStateOf(DeviceStatus())

    // The flashlight, for the `flashlight` command. Needs no permission (not the camera's).
    private lateinit var cameras: CameraManager
    private var torchCamera: String? = null
    private var torchOn by mutableStateOf(false)
    private val torchChanges = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchCamera) torchOn = enabled
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            if (cameraId == torchCamera) torchOn = false
        }
    }
    private lateinit var watcher: DeviceWatcher

    // Today's events under the date: off until you say yes, then read only from the phone.
    private var agenda by mutableStateOf<Agenda>(Agenda.Off)
    private var watchingCalendar = false
    private val calendarChanges = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = refreshAgenda()
    }
    private val askCalendar = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) say("Calendar access is off. Turn it on under Settings → Apps → Newt Home → Permissions.")
        refreshAgenda()
    }

    /** Android tells the home app when apps are installed, updated or removed. */
    private val packageChanges = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = reload()
        override fun onPackageAdded(packageName: String, user: UserHandle) = reload()
        override fun onPackageChanged(packageName: String, user: UserHandle) = reload()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()

        // A website added from the browser, or a shortcut removed or renamed.
        override fun onShortcutsChanged(packageName: String, shortcuts: List<ShortcutInfo>, user: UserHandle) = reload()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        hideStatusBar()
        launcherApps = getSystemService(LauncherApps::class.java)
        watcher = DeviceWatcher(this) { status = it }
        cameras = getSystemService(CameraManager::class.java)
        torchCamera = findTorch()
        cameras.registerTorchCallback(torchChanges, Handler(Looper.getMainLooper()))
        store = HomeStore(this)
        favoriteKeys = store.favorites()
        folders = store.folders()
        openFolder = store.openFolder()
        uses = store.uses()
        hidden = store.hidden()
        searchWords = store.searchWords()
        websites = store.websites()
        launcherApps.registerCallback(packageChanges, Handler(Looper.getMainLooper()))
        reload()

        setContent {
            NewtTheme {
                HomeRoute(
                    apps = installed.map { it.app } + websites.map { it.toApp() },
                    favoriteKeys = favoriteKeys,
                    folders = folders,
                    openFolder = openFolder,
                    uses = uses,
                    resetSignal = resetSignal,
                    bootKey = bootKey,
                    animateBoot = animateBoot,
                    status = status.copy(flashlight = torchOn),
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
                    onRemoveShortcut = ::removeShortcut,
                    appShortcuts = ::shortcutsFor,
                    onOpenAppShortcut = ::openAppShortcut,
                    onAddWebsite = ::addWebsite,
                    onClock = ::openClock,
                    onDate = ::openCalendar,
                    agenda = agenda,
                    onAgendaChoice = ::chooseEvents,
                    onFixCalendar = ::showOwnSettings,
                    onDateHold = ::toggleEvents,
                    onOpenEvent = ::openEvent,
                    onStopEvents = {
                        setShowEvents(false)
                        say("Today's events are hidden. Press and hold the date to bring them back.")
                    },
                    onNewDay = ::refreshAgenda,
                    onCommand = ::runCommand,
                    hidden = hidden,
                    searchWords = searchWords,
                    onHiddenChange = {
                        hidden = it
                        store.setHidden(it)
                    },
                    onSearchWordsChange = {
                        searchWords = it
                        store.setSearchWords(it)
                    },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        watcher.start()
    }

    override fun onStop() {
        watcher.stop()
        stopWatchingCalendar()
        super.onStop()
    }

    /**
     * The home screen shows its own terminal status line, so Android's status bar is hidden
     * here (only here). Swiping down from the top edge brings it back for a moment, and
     * notifications with it.
     */
    private fun hideStatusBar() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.statusBars())
        }
    }

    override fun onResume() {
        super.onResume()
        hideStatusBar()
        // Android's "Remove animations" sets the animator scale to 0; then nothing types out.
        animateBoot = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
        bootKey++
        // Calendar access can be turned off in Settings while we're away.
        refreshAgenda()
    }

    override fun onDestroy() {
        launcherApps.unregisterCallback(packageChanges)
        cameras.unregisterTorchCallback(torchChanges)
        super.onDestroy()
    }

    /** Pressing Home while the home screen is showing returns it to its resting state. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        resetSignal++
    }

    private fun canReadCalendar(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** Works out what goes under the date, reading today's events if they're turned on. */
    private fun refreshAgenda() {
        when (store.showEvents()) {
            null -> agenda = Agenda.Offer
            false -> agenda = Agenda.Off
            true -> if (!canReadCalendar()) {
                agenda = Agenda.NeedsPermission
            } else {
                watchCalendar()
                lifecycleScope.launch {
                    val events = withContext(Dispatchers.IO) {
                        try {
                            CalendarRepository(this@MainActivity).today()
                        } catch (e: RuntimeException) {
                            // Access was taken away mid-read, or no calendar storage on the phone.
                            emptyList()
                        }
                    }
                    if (store.showEvents() == true) agenda = Agenda.Today(events)
                }
            }
        }
        if (agenda == Agenda.Off || agenda == Agenda.Offer || agenda == Agenda.NeedsPermission) stopWatchingCalendar()
    }

    /** Reads again when an event is added, changed or synced, but only while it's shown. */
    private fun watchCalendar() {
        if (watchingCalendar) return
        try {
            contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, calendarChanges)
            watchingCalendar = true
        } catch (e: SecurityException) {
            watchingCalendar = false
        }
    }

    private fun stopWatchingCalendar() {
        if (!watchingCalendar) return
        contentResolver.unregisterContentObserver(calendarChanges)
        watchingCalendar = false
    }

    private fun setShowEvents(show: Boolean?) {
        store.setShowEvents(show)
        refreshAgenda()
    }

    /** "Yes" or "No thanks" on the offer under the date. Yes asks Android for calendar access. */
    private fun chooseEvents(yes: Boolean) {
        setShowEvents(yes)
        if (yes && !canReadCalendar()) askCalendar.launch(Manifest.permission.READ_CALENDAR)
    }

    /** Press and hold the date: hides events when they're shown, or offers them again. */
    private fun toggleEvents() {
        if (store.showEvents() == true) {
            setShowEvents(false)
            say("Today's events are hidden. Press and hold the date to bring them back.")
        } else {
            setShowEvents(null)
        }
    }

    /** Newt Home's own page in Settings, to turn calendar access back on. */
    private fun showOwnSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        if (tryStart(intent)) say("Tap Permissions, then Calendar, then Allow.")
    }

    /** One event in the calendar app; if no app opens it, the calendar at today. */
    private fun openEvent(event: DayEvent) {
        val intent = Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id))
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.begin)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end)
            .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, event.allDay)
        if (!tryStart(intent)) openCalendar()
    }

    /** A command from the prompt (`help` and `ls` are handled on the home screen itself). */
    private fun runCommand(command: Command) {
        when (command) {
            is Command.Timer -> {
                val intent = Intent(AlarmClock.ACTION_SET_TIMER)
                    .putExtra(AlarmClock.EXTRA_LENGTH, command.seconds)
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                if (!tryStart(intent)) say("Your clock app can't start timers")
            }
            is Command.Alarm -> {
                val intent = Intent(AlarmClock.ACTION_SET_ALARM)
                    .putExtra(AlarmClock.EXTRA_HOUR, command.hour)
                    .putExtra(AlarmClock.EXTRA_MINUTES, command.minute)
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                if (!tryStart(intent)) say("Your clock app can't set alarms")
            }
            is Command.Flashlight -> setTorch(command.on ?: !torchOn)
            is Command.Call -> call(command)
            Command.Wifi -> {
                if (!tryStart(Intent(Settings.Panel.ACTION_WIFI)) && !tryStart(Intent(Settings.ACTION_WIFI_SETTINGS))) {
                    say("Couldn't open Wi-Fi settings")
                }
            }
            Command.Bluetooth -> if (!tryStart(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))) say("Couldn't open Bluetooth settings")
            Command.Files -> {
                val files = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_FILES)
                if (!tryStart(files) && !openByName("Files")) say("No files app found")
            }
            Command.Help -> Unit
        }
    }

    /**
     * A number opens the phone app with it dialed (you still press call). A name opens Newt
     * Phone's Contacts searched for it (other phone apps just open). Nothing calls by itself.
     */
    private fun call(command: Command.Call) {
        val intent = if (command.who.isEmpty()) {
            Intent(Intent.ACTION_DIAL)
        } else if (command.isNumber) {
            Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", command.who.filter { it.isDigit() || it in "+*#" }, null))
        } else {
            Intent(Intent.ACTION_DIAL).putExtra(EXTRA_CONTACT_QUERY, command.who)
        }
        if (!tryStart(intent)) say("No phone app found")
    }

    /** The back camera's flashlight, or any one there is. */
    private fun findTorch(): String? = try {
        val withFlash = cameras.cameraIdList.filter {
            cameras.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
        withFlash.firstOrNull {
            cameras.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: withFlash.firstOrNull()
    } catch (e: CameraAccessException) {
        null
    }

    private fun setTorch(on: Boolean) {
        val id = torchCamera ?: return say("This phone has no flashlight")
        try {
            cameras.setTorchMode(id, on)
            torchOn = on
            say(if (on) "Flashlight on" else "Flashlight off")
        } catch (e: CameraAccessException) {
            // Another app is using the camera.
            say("The flashlight is busy. Close the camera app and try again.")
        } catch (e: IllegalArgumentException) {
            say("This phone has no flashlight")
        }
    }

    private fun reload() {
        lifecycleScope.launch {
            installed = withContext(Dispatchers.IO) { AppRepository(this@MainActivity).load() }
        }
    }

    private fun find(app: LauncherApp): InstalledApp? = installed.firstOrNull { it.app.key == app.key }

    private fun open(app: LauncherApp) {
        app.url?.let { url ->
            // A website added by hand: opened like any link, in the default browser.
            if (tryStart(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE))) {
                store.recordUse(app.key)
                uses = store.uses()
            } else {
                say("No browser found to open ${app.name}")
            }
            return
        }
        val target = find(app) ?: return
        try {
            val shortcut = target.shortcut
            if (shortcut != null) {
                launcherApps.startShortcut(shortcut, null, null)
            } else {
                launcherApps.startMainActivity(target.component, target.user, null, null)
            }
            store.recordUse(app.key)
            uses = store.uses()
        } catch (e: RuntimeException) {
            // The app was removed or disabled a moment ago, or refuses to open.
            say("Couldn't open ${app.name}")
        }
    }

    /**
     * Unpins a shortcut, like a website added from the browser. Android only lets the home
     * app set the whole list of its pins for a package, so the others are kept.
     */
    private fun removeShortcut(app: LauncherApp) {
        if (app.url != null) {
            websites = websites.filter { it.key != app.key }
            store.setWebsites(websites)
            say("Removed ${app.name}")
            return
        }
        val target = find(app)?.shortcut ?: return
        val keep = installed.mapNotNull { it.shortcut }
            .filter { it.`package` == target.`package` && it.id != target.id }
            .map { it.id }
        try {
            launcherApps.pinShortcuts(target.`package`, keep, target.userHandle)
            say("Removed ${app.name}")
        } catch (e: RuntimeException) {
            // No longer the default home app, so Android won't let it change pins.
            say("Couldn't remove ${app.name}")
        }
        reload()
    }

    private fun addWebsite(name: String, url: String) {
        websites = websites + Website(UUID.randomUUID().toString(), name, url)
        store.setWebsites(websites)
        say("Added $name to All apps")
    }

    /**
     * An app's own shortcuts (the ones it offers to home screens), best first. Android only
     * shares these with the default home app; otherwise there are none.
     */
    private fun shortcutsFor(app: LauncherApp): List<AppShortcut> {
        val target = find(app) ?: return emptyList()
        if (target.shortcut != null) return emptyList()
        return try {
            if (!launcherApps.hasShortcutHostPermission()) return emptyList()
            val query = LauncherApps.ShortcutQuery()
                .setPackage(target.component.packageName)
                .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST)
            launcherApps.getShortcuts(query, target.user).orEmpty()
                .filter { it.isEnabled }
                .sortedWith(compareBy({ !it.isDeclaredInManifest }, { it.rank }))
                .take(MAX_APP_SHORTCUTS)
                .mapNotNull { info ->
                    val label = (info.shortLabel ?: info.longLabel)?.toString()?.trim().orEmpty()
                    if (label.isEmpty()) return@mapNotNull null
                    val long = info.longLabel?.toString()?.trim()?.takeIf { it.isNotEmpty() && it != label }
                    AppShortcut(info.`package`, info.id, label, long)
                }
        } catch (e: RuntimeException) {
            // No longer the default home app, or the app's user is locked.
            emptyList()
        }
    }

    private fun openAppShortcut(app: LauncherApp, shortcut: AppShortcut) {
        val user = find(app)?.user ?: return
        try {
            launcherApps.startShortcut(shortcut.packageName, shortcut.id, null, null, user)
            store.recordUse(app.key)
            uses = store.uses()
        } catch (e: RuntimeException) {
            say("Couldn't open ${shortcut.label}")
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
