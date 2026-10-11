package io.github.secnewt.home.ui

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import io.github.secnewt.home.apps.Agenda
import io.github.secnewt.home.apps.AppSearch
import io.github.secnewt.home.apps.Command
import io.github.secnewt.home.apps.CommandMatch
import io.github.secnewt.home.apps.Commands
import io.github.secnewt.home.apps.DayEvent
import io.github.secnewt.home.apps.DeviceStatus
import io.github.secnewt.home.apps.Favorites
import io.github.secnewt.home.apps.Folder
import io.github.secnewt.home.apps.Folders
import io.github.secnewt.home.apps.LauncherApp
import io.github.secnewt.home.apps.SearchWords
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

/** How long the only match waits before opening, so you can see it and keep typing to cancel. */
private const val AUTO_OPEN_DELAY_MS = 700L

/**
 * The home screen with its behavior: search, opening the only match, folders, the
 * press-and-hold menus, and Back. [resetSignal] changes when Home is pressed, which clears
 * the prompt (the open folder stays open).
 */
@Composable
fun HomeRoute(
    apps: List<LauncherApp>,
    favoriteKeys: List<String>,
    folders: List<Folder>,
    openFolder: String?,
    uses: Map<String, Int>,
    resetSignal: Int,
    bootKey: Int,
    animateBoot: Boolean,
    status: DeviceStatus,
    onOpen: (LauncherApp) -> Unit,
    onFavoritesChange: (List<String>) -> Unit,
    onFoldersChange: (List<Folder>) -> Unit,
    onOpenFolderChange: (String?) -> Unit,
    onAppInfo: (LauncherApp) -> Unit,
    onRemoveShortcut: (LauncherApp) -> Unit = {},
    onClock: () -> Unit,
    onDate: () -> Unit,
    agenda: Agenda = Agenda.Off,
    onAgendaChoice: (Boolean) -> Unit = {},
    onFixCalendar: () -> Unit = {},
    onDateHold: () -> Unit = {},
    onOpenEvent: (DayEvent) -> Unit = {},
    onStopEvents: () -> Unit = {},
    onNewDay: () -> Unit = {},
    onCommand: (Command) -> Unit = {},
    hidden: Set<String> = emptySet(),
    searchWords: Map<String, List<String>> = emptyMap(),
    onHiddenChange: (Set<String>) -> Unit = {},
    onSearchWordsChange: (Map<String, List<String>>) -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    // At most one sheet is open: an app's menu, a folder's menu, or New folder.
    var menuFor by rememberSaveable { mutableStateOf<String?>(null) }
    var folderMenuFor by rememberSaveable { mutableStateOf<String?>(null) }
    var newFolder by rememberSaveable { mutableStateOf(false) }
    var agendaOpen by rememberSaveable { mutableStateOf(false) }
    var helpOpen by rememberSaveable { mutableStateOf(false) }

    fun closeSheets() {
        menuFor = null
        folderMenuFor = null
        newFolder = false
        agendaOpen = false
        helpOpen = false
    }

    fun reset() {
        query = ""
        screen = Screen.HOME
        closeSheets()
    }
    LaunchedEffect(resetSignal) { reset() }

    fun open(app: LauncherApp) {
        onOpen(app)
        reset()
    }

    val sorted = remember(apps) { apps.sortedBy { AppSearch.normalize(it.name) } }
    // Hidden apps leave All apps and folders (they fold away at the end of All apps).
    val (hiddenApps, shownApps) = remember(sorted, hidden) { sorted.partition { it.key in hidden } }
    val favorites = remember(apps, favoriteKeys) {
        val byKey = apps.associateBy { it.key }
        favoriteKeys.mapNotNull { byKey[it] }
    }
    val folderViews = remember(apps, folders, hidden) {
        val byKey = apps.associateBy { it.key }
        folders.map { f ->
            FolderView(f.name, f.keys.filter { it !in hidden }.mapNotNull { byKey[it] }.sortedBy { AppSearch.normalize(it.name) })
        }
    }
    val isList = AppSearch.normalize(query) == "ls"
    val results = remember(query, apps, uses, folders, searchWords) {
        if (query.isBlank() || isList) emptyList<AppSearch.Result<LauncherApp>>()
        else AppSearch.search(
            apps,
            query,
            name = { it.name },
            uses = { uses[it.key] ?: 0 },
            folder = { Folders.folderOf(folders, it.key) },
            words = { searchWords[it.key].orEmpty() },
        )
    }
    val command = if (isList) null else Commands.parse(query)
    // A command word, or the start of one, never opens an app by itself (typing "hel" on the
    // way to "help" mustn't launch an app named Hello); commands only run on Enter or a tap.
    val opening = if (query.isBlank() || isList || command != null || Commands.mightBecomeCommand(query)) {
        null
    } else {
        AppSearch.onlyMatch(results, query)
    }

    fun runCommand(ready: Command) {
        reset()
        if (ready == Command.Help) helpOpen = true else onCommand(ready)
    }

    // Enter: a finished command runs; otherwise the top app opens, so an app named like a
    // command (a "Timer" app, say) is still one Enter away.
    fun submit() {
        if (command is CommandMatch.Ready) runCommand(command.command) else results.firstOrNull()?.let { open(it.item) }
    }

    // Restarts with every letter typed, so typing more cancels the countdown.
    LaunchedEffect(opening?.key, query) {
        if (opening != null) {
            delay(AUTO_OPEN_DELAY_MS)
            open(opening)
        }
    }

    val today = agenda as? Agenda.Today
    val sheetOpen = menuFor != null || folderMenuFor != null || newFolder || (agendaOpen && today != null) || helpOpen
    // Typing ls is the command for the all-apps screen.
    LaunchedEffect(isList) {
        if (isList) {
            query = ""
            screen = Screen.APPS
        }
    }

    BackHandler(enabled = query.isNotEmpty() || screen != Screen.HOME || sheetOpen) {
        if (sheetOpen) closeSheets() else reset()
    }

    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000 - value % 60_000)
        }
    }
    val context = LocalContext.current
    // Follows the phone's language, and updates if it changes.
    val locale = LocalLocale.current.platformLocale
    val clock = SimpleDateFormat(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm", locale).format(Date(now))
    val date = SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEdMMM"), locale).format(Date(now))
    val timeFormat = SimpleDateFormat(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a", locale)
    val time: (Long) -> String = { timeFormat.format(Date(it)) }
    val clockTime: (Int, Int) -> String = { hour, minute ->
        val at = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        timeFormat.format(at.time)
    }
    // Reads the day's events again when the date turns over at midnight.
    LaunchedEffect(date) { onNewDay() }

    val view = when {
        query.isNotBlank() && !isList -> HomeView.Results(
            query,
            results,
            when (command) {
                is CommandMatch.Ready -> CommandView(Commands.describe(command.command, clockTime), ready = true)
                is CommandMatch.Incomplete -> CommandView(command.hint, ready = false)
                null -> null
            },
        )
        else -> HomeView.Favorites
    }

    val held = menuFor?.let { key -> apps.firstOrNull { it.key == key } }
    val heldFolder = folderMenuFor?.let { name -> folderViews.firstOrNull { it.name == name } }
    val folderNames = folders.map { it.name }
    val menu: @Composable () -> Unit = {
        if (held != null) {
            val app = held
            val index = favoriteKeys.indexOf(app.key)
            HoldMenu(
                app = app,
                isFavorite = index >= 0,
                canMoveUp = index > 0,
                canMoveDown = index >= 0 && index < favoriteKeys.lastIndex,
                folderNames = folderNames,
                currentFolder = Folders.folderOf(folders, app.key),
                onToggleFavorite = {
                    onFavoritesChange(Favorites.toggle(favoriteKeys, app.key))
                    closeSheets()
                },
                onMove = { by -> onFavoritesChange(Favorites.move(favoriteKeys, app.key, by)) },
                onMoveToFolder = { to ->
                    onFoldersChange(Folders.moveApp(folders, app.key, to))
                    closeSheets()
                },
                onMoveToNewFolder = { name ->
                    val made = Folders.create(folders, name)
                    onFoldersChange(Folders.moveApp(made, app.key, Folders.cleanName(name)))
                    onOpenFolderChange(Folders.cleanName(name))
                    closeSheets()
                },
                onAppInfo = {
                    closeSheets()
                    onAppInfo(app)
                },
                onDismiss = ::closeSheets,
                onRemoveShortcut = {
                    closeSheets()
                    onRemoveShortcut(app)
                },
                isHidden = app.key in hidden,
                words = searchWords[app.key].orEmpty(),
                onToggleHidden = {
                    onHiddenChange(if (app.key in hidden) hidden - app.key else hidden + app.key)
                    closeSheets()
                },
                onSetWords = { list ->
                    onSearchWordsChange(SearchWords.set(searchWords, app.key, list))
                    closeSheets()
                },
            )
        } else if (heldFolder != null) {
            val folder = heldFolder
            val index = folderNames.indexOf(folder.name)
            FolderMenu(
                name = folder.name,
                appCount = folder.apps.size,
                otherNames = folderNames - folder.name,
                canMoveUp = index > 0,
                canMoveDown = index < folderNames.lastIndex,
                onRename = { to ->
                    onFoldersChange(Folders.rename(folders, folder.name, to))
                    if (openFolder == folder.name) onOpenFolderChange(Folders.cleanName(to))
                    closeSheets()
                },
                onMove = { by -> onFoldersChange(Folders.move(folders, folder.name, by)) },
                onDelete = {
                    onFoldersChange(Folders.delete(folders, folder.name))
                    if (openFolder == folder.name) onOpenFolderChange(null)
                    closeSheets()
                },
                onDismiss = ::closeSheets,
            )
        } else if (agendaOpen && today != null) {
            AgendaSheet(
                date = date,
                events = today.events,
                now = now,
                time = time,
                onOpenEvent = {
                    closeSheets()
                    onOpenEvent(it)
                },
                onOpenCalendar = {
                    closeSheets()
                    onDate()
                },
                onStop = {
                    closeSheets()
                    onStopEvents()
                },
                onDismiss = ::closeSheets,
            )
        } else if (helpOpen) {
            HelpSheet(
                commands = Commands.HELP,
                onPick = { example ->
                    closeSheets()
                    query = example
                },
                onDismiss = ::closeSheets,
            )
        } else if (newFolder) {
            NewFolderSheet(
                existingNames = folderNames,
                onCreate = { name ->
                    onFoldersChange(Folders.create(folders, name))
                    onOpenFolderChange(Folders.cleanName(name))
                    closeSheets()
                },
                onDismiss = ::closeSheets,
            )
        }
    }

    WithHoldMenu(menuOpen = held != null || heldFolder != null || newFolder || (agendaOpen && today != null) || helpOpen, menu = menu) {
        HomeScreen(
            clock = clock,
            date = date,
            view = view,
            favorites = favorites,
            folders = folderViews,
            openFolder = openFolder,
            allApps = shownApps,
            hiddenApps = hiddenApps,
            isHidden = { it.key in hidden },
            query = query,
            opening = opening,
            onQueryChange = { query = it },
            onSubmit = ::submit,
            onOpen = ::open,
            onHold = {
                closeSheets()
                menuFor = it.key
            },
            onToggleFolder = { name -> onOpenFolderChange(if (openFolder == name) null else name) },
            onHoldFolder = { name ->
                closeSheets()
                folderMenuFor = name
            },
            onNewFolder = {
                closeSheets()
                newFolder = true
            },
            folderOf = { Folders.folderOf(folders, it.key) },
            bootKey = bootKey,
            animateBoot = animateBoot,
            status = status,
            onClock = onClock,
            // With events on, the date opens the day's list (which has "Open calendar").
            onDate = {
                if (today != null) {
                    closeSheets()
                    agendaOpen = true
                } else {
                    onDate()
                }
            },
            screen = screen,
            onScreen = { screen = it },
            agenda = agenda,
            now = now,
            time = time,
            onAgendaChoice = onAgendaChoice,
            onFixCalendar = onFixCalendar,
            onDateHold = onDateHold,
        )
    }
}
