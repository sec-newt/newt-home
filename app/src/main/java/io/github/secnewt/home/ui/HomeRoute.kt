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
import io.github.secnewt.home.apps.AppSearch
import io.github.secnewt.home.apps.Favorites
import io.github.secnewt.home.apps.LauncherApp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** How long the only match waits before opening, so you can see it and keep typing to cancel. */
private const val AUTO_OPEN_DELAY_MS = 700L

/**
 * The home screen with its behavior: search, opening the only match, the press-and-hold
 * menu, and Back. [resetSignal] changes when Home is pressed, which clears the prompt.
 */
@Composable
fun HomeRoute(
    apps: List<LauncherApp>,
    favoriteKeys: List<String>,
    uses: Map<String, Int>,
    resetSignal: Int,
    onOpen: (LauncherApp) -> Unit,
    onFavoritesChange: (List<String>) -> Unit,
    onAppInfo: (LauncherApp) -> Unit,
    onClock: () -> Unit,
    onDate: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var showAll by rememberSaveable { mutableStateOf(false) }
    var menuFor by rememberSaveable { mutableStateOf<String?>(null) }

    fun reset() {
        query = ""
        showAll = false
        menuFor = null
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
    val isList = AppSearch.normalize(query) == "ls"
    val results = remember(query, apps, uses) {
        if (query.isBlank() || isList) emptyList<AppSearch.Result<LauncherApp>>()
        else AppSearch.search(apps, query, name = { it.name }, uses = { uses[it.key] ?: 0 })
    }
    val opening = if (query.isBlank() || isList) null else AppSearch.onlyMatch(results, query)

    // Restarts with every letter typed, so typing more cancels the countdown.
    LaunchedEffect(opening?.key, query) {
        if (opening != null) {
            delay(AUTO_OPEN_DELAY_MS)
            open(opening)
        }
    }

    BackHandler(enabled = query.isNotEmpty() || showAll || menuFor != null) {
        if (menuFor != null) menuFor = null else reset()
    }

    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000 - value % 60_000)
        }
    }
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val clock = SimpleDateFormat(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm", locale).format(Date(now))
    val date = SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEdMMM"), locale).format(Date(now))

    val view = when {
        query.isNotBlank() && !isList -> HomeView.Results(query, results)
        showAll || isList -> HomeView.AllApps
        else -> HomeView.Favorites
    }

    val held = menuFor?.let { key -> apps.firstOrNull { it.key == key } }
    val menu: (@Composable () -> Unit)? = if (held == null) null else @Composable {
        val app: LauncherApp = held
        val index = favoriteKeys.indexOf(app.key)
        HoldMenu(
            app = app,
            isFavorite = index >= 0,
            canMoveUp = index > 0,
            canMoveDown = index >= 0 && index < favoriteKeys.lastIndex,
            onToggleFavorite = {
                onFavoritesChange(Favorites.toggle(favoriteKeys, app.key))
                menuFor = null
            },
            onMove = { by -> onFavoritesChange(Favorites.move(favoriteKeys, app.key, by)) },
            onAppInfo = {
                menuFor = null
                onAppInfo(app)
            },
            onDismiss = { menuFor = null },
        )
    }

    WithHoldMenu(menu) {
        HomeScreen(
            clock = clock,
            date = date,
            view = view,
            favorites = favorites,
            allApps = sorted,
            query = query,
            opening = opening,
            onQueryChange = { query = it },
            onSubmit = { results.firstOrNull()?.let { open(it.item) } },
            onOpen = ::open,
            onHold = { menuFor = it.key },
            onClock = onClock,
            onDate = onDate,
            onShowAll = { showAll = true },
        )
    }
}
