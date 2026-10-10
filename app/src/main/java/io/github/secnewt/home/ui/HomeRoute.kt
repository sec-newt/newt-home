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
import io.github.secnewt.home.apps.AppSearch
import io.github.secnewt.home.apps.Favorites
import io.github.secnewt.home.apps.Folder
import io.github.secnewt.home.apps.Folders
import io.github.secnewt.home.apps.LauncherApp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
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
    onOpen: (LauncherApp) -> Unit,
    onFavoritesChange: (List<String>) -> Unit,
    onFoldersChange: (List<Folder>) -> Unit,
    onOpenFolderChange: (String?) -> Unit,
    onAppInfo: (LauncherApp) -> Unit,
    onClock: () -> Unit,
    onDate: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var showAll by rememberSaveable { mutableStateOf(false) }
    // At most one sheet is open: an app's menu, a folder's menu, or New folder.
    var menuFor by rememberSaveable { mutableStateOf<String?>(null) }
    var folderMenuFor by rememberSaveable { mutableStateOf<String?>(null) }
    var newFolder by rememberSaveable { mutableStateOf(false) }

    fun closeSheets() {
        menuFor = null
        folderMenuFor = null
        newFolder = false
    }

    fun reset() {
        query = ""
        showAll = false
        closeSheets()
    }
    LaunchedEffect(resetSignal) { reset() }

    fun open(app: LauncherApp) {
        onOpen(app)
        reset()
    }

    val sorted = remember(apps) { apps.sortedBy { AppSearch.normalize(it.name) } }
    val favorites = remember(apps, favoriteKeys) {
        val byKey = apps.associateBy { it.key }
        favoriteKeys.mapNotNull { byKey[it] }
    }
    val folderViews = remember(apps, folders) {
        val byKey = apps.associateBy { it.key }
        folders.map { f -> FolderView(f.name, f.keys.mapNotNull { byKey[it] }.sortedBy { AppSearch.normalize(it.name) }) }
    }
    val isList = AppSearch.normalize(query) == "ls"
    val results = remember(query, apps, uses, folders) {
        if (query.isBlank() || isList) emptyList<AppSearch.Result<LauncherApp>>()
        else AppSearch.search(
            apps,
            query,
            name = { it.name },
            uses = { uses[it.key] ?: 0 },
            folder = { Folders.folderOf(folders, it.key) },
        )
    }
    val opening = if (query.isBlank() || isList) null else AppSearch.onlyMatch(results, query)

    // Restarts with every letter typed, so typing more cancels the countdown.
    LaunchedEffect(opening?.key, query) {
        if (opening != null) {
            delay(AUTO_OPEN_DELAY_MS)
            open(opening)
        }
    }

    val sheetOpen = menuFor != null || folderMenuFor != null || newFolder
    BackHandler(enabled = query.isNotEmpty() || showAll || sheetOpen) {
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

    val view = when {
        query.isNotBlank() && !isList -> HomeView.Results(query, results)
        showAll || isList -> HomeView.AllApps
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

    WithHoldMenu(menuOpen = held != null || heldFolder != null || newFolder, menu = menu) {
        HomeScreen(
            clock = clock,
            date = date,
            view = view,
            favorites = favorites,
            folders = folderViews,
            openFolder = openFolder,
            allApps = sorted,
            query = query,
            opening = opening,
            onQueryChange = { query = it },
            onSubmit = { results.firstOrNull()?.let { open(it.item) } },
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
            onClock = onClock,
            onDate = onDate,
            onShowAll = { showAll = true },
        )
    }
}
