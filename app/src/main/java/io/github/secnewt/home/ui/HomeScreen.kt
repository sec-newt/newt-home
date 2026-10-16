package io.github.secnewt.home.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Popup
import io.github.secnewt.home.apps.Letters
import io.github.secnewt.home.apps.ProfileKind
import io.github.secnewt.home.apps.ProfileView
import io.github.secnewt.home.apps.Websites
import kotlinx.coroutines.launch
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shadow
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.secnewt.home.apps.Agenda
import io.github.secnewt.home.apps.AgendaText
import io.github.secnewt.home.apps.AppSearch
import io.github.secnewt.home.apps.Connection
import io.github.secnewt.home.apps.DeviceStatus
import io.github.secnewt.home.apps.LauncherApp
import io.github.secnewt.home.ui.theme.LocalTerminalColors
import io.github.secnewt.ui.NewtFonts
import kotlin.math.absoluteValue
import androidx.compose.ui.res.painterResource
import io.github.secnewt.ui.NewtWeather
import io.github.secnewt.home.apps.WeatherNow
import io.github.secnewt.home.apps.WeatherText

/** A folder as the tree shows it: its name and its apps, A–Z. */
@Immutable
data class FolderView(val name: String, val apps: List<LauncherApp>)

/** The three screens: home in the middle, folders a swipe away to the left, all apps below. */
enum class Screen { FOLDERS, HOME, APPS }

/** What home shows above the prompt. */
sealed interface HomeView {
    /** The resting home screen: favorites. */
    data object Favorites : HomeView

    /** Search results for what's typed at the prompt, with a command first if it is one. */
    data class Results(
        val query: String,
        val results: List<AppSearch.Result<LauncherApp>>,
        val command: CommandView? = null,
    ) : HomeView
}

/**
 * A command at the top of the results. [ready] commands run on Enter or a tap; otherwise
 * [text] says what's still needed.
 */
@Immutable
data class CommandView(val text: String, val ready: Boolean)

/** The All apps list, for tests. */
internal const val ALL_APPS_LIST = "all-apps"

/** How far past the end of a list a drag has to go to switch screens. */
private val EDGE_PULL = 96.dp

/**
 * Calls [onPull] when a drag keeps going past the end of a list: upward past the bottom
 * (`up`), or downward past the top. If the list is short and doesn't scroll, any drag that
 * far counts. Each drag counts once; lifting the finger starts over.
 */
@Composable
private fun rememberEdgePull(up: Boolean, onPull: () -> Unit): NestedScrollConnection {
    val threshold = with(LocalDensity.current) { EDGE_PULL.toPx() }
    val latest = rememberUpdatedState(onPull)
    return remember(up, threshold) {
        object : NestedScrollConnection {
            var pulled = 0f
            var fired = false

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || fired) return Offset.Zero
                val toward = if (up) -available.y else available.y
                if (toward > 0f) pulled += toward else if (consumed.y != 0f) pulled = 0f
                if (pulled >= threshold) {
                    fired = true
                    latest.value()
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                pulled = 0f
                fired = false
                return Velocity.Zero
            }
        }
    }
}

/**
 * The launcher: a terminal you can tap, on three screens. Home has the time and date (each
 * opens its app), favorites or search results, and the `~$` prompt. Swipe right for folders,
 * up for all apps; the buttons above the prompt do the same, so swiping is never required
 * (unless [showButtons] is off: `buttons off`, for people who swipe). Folders has the prompt
 * too, and search results show on whichever screen you typed on.
 */
@Composable
fun HomeScreen(
    clock: String,
    date: String,
    view: HomeView,
    favorites: List<LauncherApp>,
    folders: List<FolderView>,
    openFolder: String?,
    allApps: List<LauncherApp>,
    query: String,
    opening: LauncherApp?,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onOpen: (LauncherApp) -> Unit,
    onHold: (LauncherApp) -> Unit,
    onToggleFolder: (String) -> Unit,
    onHoldFolder: (String) -> Unit,
    onNewFolder: () -> Unit,
    onClock: () -> Unit,
    onDate: () -> Unit,
    folderOf: (LauncherApp) -> String? = { null },
    bootKey: Int = 0,
    animateBoot: Boolean = false,
    status: DeviceStatus = DeviceStatus(),
    screen: Screen = Screen.HOME,
    onScreen: (Screen) -> Unit = {},
    agenda: Agenda = Agenda.Off,
    now: Long = 0L,
    time: (Long) -> String = { "" },
    onAgendaChoice: (Boolean) -> Unit = {},
    onFixCalendar: () -> Unit = {},
    onDateHold: () -> Unit = {},
    hiddenApps: List<LauncherApp> = emptyList(),
    isHidden: (LauncherApp) -> Boolean = { false },
    onAddWebsite: () -> Unit = {},
    profiles: List<ProfileView> = emptyList(),
    onToggleProfileLock: (ProfileKind) -> Unit = {},
    onStatusTap: () -> Unit = {},
    onVoice: () -> Unit = {},
    weather: WeatherNow? = null,
    onWeather: () -> Unit = {},
    headsUp: String? = null,
    onHearWeather: () -> Unit = {},
    showButtons: Boolean = true,
) {
    // Folders and home sit side by side; all apps slides up over them.
    val pager = rememberPagerState(initialPage = if (screen == Screen.FOLDERS) 0 else 1) { 2 }
    LaunchedEffect(screen) {
        val page = if (screen == Screen.FOLDERS) 0 else 1
        if (screen != Screen.APPS && pager.currentPage != page) pager.animateScrollToPage(page)
    }
    val latestOnScreen = rememberUpdatedState(onScreen)
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { page ->
            latestOnScreen.value(if (page == 0) Screen.FOLDERS else Screen.HOME)
        }
    }
    val appsOpen = screen == Screen.APPS

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        if (!cleanLook()) GadgetBackground()
        Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            StatusLine(status, onStatusTap)
            Box(Modifier.weight(1f)) {
                HorizontalPager(
                    state = pager,
                    userScrollEnabled = !appsOpen,
                    // Hidden behind all apps: screen readers shouldn't reach it.
                    modifier = if (appsOpen) Modifier.clearAndSetSemantics { } else Modifier,
                ) { page ->
                    if (page == 0) {
                        FoldersPage(
                            folders, openFolder, onOpen, onHold, onToggleFolder, onHoldFolder, onNewFolder,
                            view, query, opening, onQueryChange, onSubmit, onVoice, folderOf, isHidden, showButtons,
                        ) {
                            onScreen(Screen.HOME)
                        }
                    } else {
                        HomePage(
                            clock, date, view, favorites, query, opening, onQueryChange, onSubmit, onOpen, onHold,
                            onClock, onDate, folderOf, bootKey, animateBoot, onScreen,
                            agenda, now, time, onAgendaChoice, onFixCalendar, onDateHold, isHidden, onVoice,
                            weather, onWeather, headsUp, onHearWeather, showButtons,
                        )
                    }
                }
                // The plain version, not the Column one (this sits inside a Box inside a Column).
                androidx.compose.animation.AnimatedVisibility(
                    visible = appsOpen,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    AppsPage(allApps, hiddenApps, profiles, onOpen, onHold, onAddWebsite, onToggleProfileLock) { onScreen(Screen.HOME) }
                }
            }
        }
    }
}

@Composable
private fun HomePage(
    clock: String,
    date: String,
    view: HomeView,
    favorites: List<LauncherApp>,
    query: String,
    opening: LauncherApp?,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onOpen: (LauncherApp) -> Unit,
    onHold: (LauncherApp) -> Unit,
    onClock: () -> Unit,
    onDate: () -> Unit,
    folderOf: (LauncherApp) -> String?,
    bootKey: Int,
    animateBoot: Boolean,
    onScreen: (Screen) -> Unit,
    agenda: Agenda,
    now: Long,
    time: (Long) -> String,
    onAgendaChoice: (Boolean) -> Unit,
    onFixCalendar: () -> Unit,
    onDateHold: () -> Unit,
    isHidden: (LauncherApp) -> Boolean,
    onVoice: () -> Unit,
    weather: WeatherNow?,
    onWeather: () -> Unit,
    headsUp: String?,
    onHearWeather: () -> Unit,
    showButtons: Boolean,
) {
    // While typing, the results get all the room the keyboard leaves: the clock, weather,
    // events and buttons step aside until the prompt is cleared.
    val typing = view is HomeView.Results
    val pullUp = rememberEdgePull(up = true) { onScreen(Screen.APPS) }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .nestedScroll(pullUp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (!typing) item {
                Column(Modifier.padding(top = 12.dp)) {
                    BootLine("date", bootKey, animateBoot) { alpha ->
                        Column(Modifier.bootFade(alpha)) {
                            ClockAndDate(
                                clock,
                                date,
                                onClock,
                                onDate,
                                dateLabel = if (agenda is Agenda.Today) "Today's events" else "Open Calendar",
                                onDateHold = onDateHold,
                                weather = weather?.let { w -> { WeatherLine(w, onWeather, onHearWeather) } },
                            )
                            weather?.let { WarningLine(it, onWeather) }
                            if (weather != null && headsUp != null) HeadsUpLine(headsUp, onWeather)
                            AgendaLine(agenda, now, time, onDate, onAgendaChoice, onFixCalendar)
                        }
                    }
                }
            }
            when (view) {
                HomeView.Favorites -> {
                    item { PromptLine("ls favs", short = true, title = "Favorites") }
                    if (favorites.isEmpty()) {
                        item { Hint("# press and hold any app to add it here") }
                    }
                    items(favorites, key = { it.key }) { AppRow(it, onOpen = onOpen, onHold = onHold) }
                }
                is HomeView.Results -> resultItems(view, opening, onSubmit, onOpen, onHold, folderOf, isHidden)
            }
        }
        if (showButtons && !typing) {
            NavRow {
                NavButton("← Folders", "Folders", Modifier.weight(1f)) { onScreen(Screen.FOLDERS) }
                NavButton("All apps ↑", "All apps", Modifier.weight(1f)) { onScreen(Screen.APPS) }
            }
        }
        Prompt(query, onQueryChange, onSubmit, onVoice)
    }
}

/** What's typed at the prompt: a command first if it is one, then the matching apps. */
private fun LazyListScope.resultItems(
    view: HomeView.Results,
    opening: LauncherApp?,
    onSubmit: () -> Unit,
    onOpen: (LauncherApp) -> Unit,
    onHold: (LauncherApp) -> Unit,
    folderOf: (LauncherApp) -> String?,
    isHidden: (LauncherApp) -> Boolean,
) {
    val command = view.command
    if (command != null) {
        item(key = "command") { CommandRow(view.query.trim(), command, onSubmit) }
    }
    item { PromptLine("find ${view.query.trim()}", title = if (view.results.isEmpty()) null else "Apps") }
    if (view.results.isEmpty() && command == null) {
        item { Hint("# no app matches “${view.query.trim()}”") }
    }
    itemsIndexed(view.results, key = { _, r -> r.item.key }) { i, r ->
        val note = when {
            i > 0 || command?.ready == true -> null
            r.item == opening -> "only match · opening…"
            else -> "Enter opens"
        }
        val why = when (r.match) {
            AppSearch.Match.IN_FOLDER -> folderOf(r.item)?.let { "in $it/" }
            AppSearch.Match.YOUR_WORD -> r.word?.let { "your word: $it" }
            else -> null
        }
        val hidden = if (isHidden(r.item)) "hidden" else null
        AppRow(
            r.item,
            note = listOfNotNull(why, hidden, note).joinToString(" · ").ifEmpty { null },
            highlighted = i == 0 && command?.ready != true,
            onOpen = onOpen,
            onHold = onHold,
        )
    }
}

@Composable
private fun FoldersPage(
    folders: List<FolderView>,
    openFolder: String?,
    onOpen: (LauncherApp) -> Unit,
    onHold: (LauncherApp) -> Unit,
    onToggleFolder: (String) -> Unit,
    onHoldFolder: (String) -> Unit,
    onNewFolder: () -> Unit,
    view: HomeView,
    query: String,
    opening: LauncherApp?,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onVoice: () -> Unit,
    folderOf: (LauncherApp) -> String?,
    isHidden: (LauncherApp) -> Boolean,
    showButtons: Boolean,
    onHome: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Typing here searches the same as on home; the tree comes back when it's cleared.
            if (view is HomeView.Results) {
                resultItems(view, opening, onSubmit, onOpen, onHold, folderOf, isHidden)
                return@LazyColumn
            }
            item { PromptLine("tree", short = true, title = "Folders") }
            for ((index, folder) in folders.withIndex()) {
                val open = folder.name == openFolder
                item(key = "folder:${folder.name}") {
                    FolderRow(folder, open, folderColor(index), onToggleFolder, onHoldFolder)
                }
                if (open) {
                    if (folder.apps.isEmpty()) {
                        item { Hint("└ empty. press and hold an app, then Move to folder", top = 0.dp) }
                    }
                    itemsIndexed(folder.apps, key = { _, a -> "in:${folder.name}:${a.key}" }) { j, app ->
                        val branch = if (j == folder.apps.lastIndex) "└" else "├"
                        AppRow(app, onOpen = onOpen, onHold = onHold, branch = branch)
                    }
                }
            }
            item { NewFolderRow(onNewFolder) }
            item { Hint("# press and hold a folder to rename, move or delete it") }
        }
        if (showButtons && view !is HomeView.Results) NavRow { NavButton("Home →", "Home", Modifier.weight(1f), onHome) }
        Prompt(query, onQueryChange, onSubmit, onVoice)
    }
}

@Composable
private fun AppsPage(
    allApps: List<LauncherApp>,
    hiddenApps: List<LauncherApp>,
    profiles: List<ProfileView>,
    onOpen: (LauncherApp) -> Unit,
    onHold: (LauncherApp) -> Unit,
    onAddWebsite: () -> Unit,
    onToggleProfileLock: (ProfileKind) -> Unit,
    onHome: () -> Unit,
) {
    // Swiping down past the top of the list slides All apps down with your finger and closes it.
    val pullDown = rememberSwipeDismiss(onHome)
    var showHidden by remember { mutableStateOf(false) }
    // Grouped the way the list is sorted (accents ignored), so each letter appears once.
    // The prompt line is row 0, so the first header is row 1.
    val groups = remember(allApps) { Letters.groups(allApps, { it.name }, offset = 1) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // The letter grid, and the one letter picked from it (only its apps are listed then).
    var gridOpen by remember { mutableStateOf(false) }
    var only by remember { mutableStateOf<String?>(null) }
    val onlyGroup = groups.firstOrNull { it.letter == only }
    BackHandler(enabled = gridOpen || only != null) {
        if (gridOpen) gridOpen = false else only = null
    }
    Box(
        Modifier
            .graphicsLayer { translationY = pullDown.offset }
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (!cleanLook()) GadgetBackground()
        Column(Modifier.fillMaxSize()) {
            if (gridOpen) {
                LetterGrid(
                    groups.map { it.letter to it.items.size },
                    modifier = Modifier
                        .weight(1f)
                        .nestedScroll(pullDown.connection),
                    onPick = { letter ->
                        only = letter
                        gridOpen = false
                    },
                )
            } else if (onlyGroup != null) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .nestedScroll(pullDown.connection),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    item { PromptLine("ls ${onlyGroup.letter.lowercase()}*") }
                    item { LetterHeader(onlyGroup.letter) { gridOpen = true } }
                    items(onlyGroup.items, key = { it.key }) { app -> AppRow(app, onOpen = onOpen, onHold = onHold) }
                }
            } else Row(Modifier.weight(1f)) {
                LazyColumn(
                    state = list,
                    modifier = Modifier
                        .testTag(ALL_APPS_LIST)
                        .weight(1f)
                        .fillMaxHeight()
                        .nestedScroll(pullDown.connection),
                    contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                ) {
                    item { PromptLine("ls", title = "All apps") }
                    for (group in groups) {
                        item(key = "letter:${group.letter}") { LetterHeader(group.letter) { gridOpen = true } }
                        items(group.items, key = { it.key }) { app -> AppRow(app, onOpen = onOpen, onHold = onHold) }
                    }
                    item(key = "add-website") { AddRow("Add a website", onAddWebsite) }
                    if (hiddenApps.isNotEmpty()) {
                        // Hidden apps stay findable: a word at the end of the list, folded away.
                        item(key = "hidden") {
                            HiddenToggle(hiddenApps.size, showHidden) { showHidden = !showHidden }
                        }
                        if (showHidden) {
                            items(hiddenApps, key = { "hidden:" + it.key }) {
                                AppRow(it, onOpen = onOpen, onHold = onHold, note = "hidden")
                            }
                        }
                    }
                    // Work profile and Private space, each in its own section at the end.
                    for (profile in profiles) {
                        item(key = "profile:${profile.kind}") {
                            ProfileHeader(profile) { onToggleProfileLock(profile.kind) }
                        }
                        if (!profile.locked) {
                            items(profile.apps, key = { "profile:" + it.key }) { AppRow(it, onOpen = onOpen, onHold = onHold) }
                        }
                    }
                }
                if (groups.size > 1) {
                    LetterStrip(groups.map { it.letter }) { i -> scope.launch { list.scrollToItem(groups[i].index) } }
                }
            }
            NavRow {
                NavButton("↓ Home", "Home", Modifier.weight(1f), onHome)
                when {
                    gridOpen -> NavButton("All A–Z", "Back to all apps", Modifier.weight(1f)) { gridOpen = false }
                    only != null -> NavButton("All A–Z", "Show all apps", Modifier.weight(1f)) { only = null }
                    groups.size > 1 -> NavButton("Letters", "Letter grid", Modifier.weight(1f)) { gridOpen = true }
                }
            }
        }
    }
}

/**
 * Every letter that has apps, as big buttons: tap S and only the S apps are listed, so
 * there's nothing to scroll through. Opened from the Letters button or any letter header.
 */
@Composable
private fun LetterGrid(letters: List<Pair<String, Int>>, modifier: Modifier = Modifier, onPick: (String) -> Unit) {
    Column(modifier.fillMaxWidth()) {
        PromptLine("ls -d [a-z]", title = "Jump to a letter")
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 76.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            gridItems(letters, key = { it.first }) { (letter, count) ->
                val shape = RoundedCornerShape(14.dp)
                val interactions = remember { MutableInteractionSource() }
                Column(
                    Modifier
                        .heightIn(min = 76.dp)
                        .glow(pressed(interactions), MaterialTheme.colorScheme.primary, neonOutline(), shape)
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                        .clickable(interactionSource = interactions, indication = LocalIndication.current) { onPick(letter) }
                        .padding(vertical = 6.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "$letter, " + if (count == 1) "1 app" else "$count apps"
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        letter,
                        fontFamily = NewtFonts.Mono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                    Text(
                        "$count",
                        fontFamily = NewtFonts.Mono,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
            }
        }
    }
}

/**
 * The letter strip down the right of All apps: touch or slide a thumb along it and the
 * list jumps to that letter, with the letter shown big in the middle of the screen while
 * your thumb is down. Its letters are sized to fit, so they don't grow with the text size.
 * Screen readers skip it; the letter headers are headings, which they can already jump by.
 */
@Composable
private fun LetterStrip(letters: List<String>, onLetter: (Int) -> Unit) {
    var active by remember { mutableStateOf<Int?>(null) }
    val haptics = LocalHapticFeedback.current
    val latest = rememberUpdatedState(onLetter)
    fun touch(y: Float, height: Float) {
        val i = Letters.pick(y, height, letters.size)
        if (i != active) {
            active = i
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            latest.value(i)
        }
    }
    BoxWithConstraints(
        Modifier
            .width(40.dp)
            .fillMaxHeight()
            .padding(vertical = 8.dp)
            .clearAndSetSemantics { },
    ) {
        val height = constraints.maxHeight.toFloat()
        val density = LocalDensity.current
        // Each letter gets an equal share of the height; the text fills about two thirds.
        val size = with(density) { (maxHeight / letters.size * 0.66f).coerceAtMost(18.dp).toPx() / fontScale / this.density }.sp
        Column(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = if (active != null) 0.95f else 0.6f))
                .pointerInput(letters, height) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        touch(down.position.y, height)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            change.consume()
                            touch(change.position.y, height)
                        }
                        active = null
                    }
                },
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            letters.forEachIndexed { i, letter ->
                Text(
                    letter,
                    fontFamily = NewtFonts.Mono,
                    fontWeight = FontWeight.Bold,
                    fontSize = size,
                    lineHeight = size,
                    color = if (i == active) LocalTerminalColors.current.prompt else MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
    val shown = active
    if (shown != null) {
        Popup(alignment = Alignment.Center) {
            Box(
                Modifier
                    .size(120.dp)
                    .glow(true, MaterialTheme.colorScheme.secondary, neonOutline(2.dp), RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    letters[shown],
                    fontFamily = NewtFonts.Mono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 72.sp,
                    color = LocalTerminalColors.current.prompt,
                )
            }
        }
    }
}

/**
 * "Work" or "Private space", with a lock button. A locked Private space hides its apps;
 * unlocking asks for your PIN or fingerprint (Android does that part).
 */
@Composable
private fun ProfileHeader(profile: ProfileView, onToggleLock: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (cleanLook()) profile.kind.title else "~/" + profile.kind.title.lowercase().replace(' ', '-') + "/",
            fontFamily = if (cleanLook()) NewtFonts.Display else NewtFonts.Mono,
            fontWeight = FontWeight.Bold,
            fontSize = if (cleanLook()) 21.sp else 18.sp,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .semantics {
                    heading()
                    contentDescription = profile.kind.title + if (profile.locked) ", locked" else ""
                },
        )
        TextButton(onClick = onToggleLock, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(if (profile.locked) "Unlock" else "Lock", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
    }
    if (profile.locked) {
        Hint("# locked · its apps are hidden until you unlock it", top = 2.dp)
    }
}

/** "▸ 3 hidden apps": tap to show or fold them away. */
@Composable
private fun HiddenToggle(count: Int, open: Boolean, onToggle: () -> Unit) {
    val noun = if (count == 1) "1 hidden app" else "$count hidden apps"
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClickLabel = if (open) "Hide them" else "Show them", onClick = onToggle)
            .padding(horizontal = 4.dp)
            .semantics { stateDescription = if (open) "Showing" else "Folded away" },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            (if (open) "▾ " else "▸ ") + noun,
            fontFamily = rowFont(),
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun LetterHeader(letter: String, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(top = 6.dp)
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClickLabel = "Letter grid", onClick = onClick)
            .padding(horizontal = 4.dp)
            .semantics { heading() },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            letter,
            fontFamily = if (cleanLook()) NewtFonts.Display else NewtFonts.Mono,
            fontWeight = FontWeight.Bold,
            fontSize = if (cleanLook()) 22.sp else 18.sp,
            color = if (cleanLook()) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun NavRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** A screen switch with words, so swiping is never the only way. [spoken] drops the arrows. */
@Composable
private fun NavButton(label: String, spoken: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val clean = cleanLook()
    val shape = RoundedCornerShape(if (clean) 14.dp else 12.dp)
    val interactions = remember { MutableInteractionSource() }
    Box(
        modifier
            .heightIn(min = if (clean) 56.dp else 52.dp)
            .glow(pressed(interactions), MaterialTheme.colorScheme.primary, neonOutline(), shape)
            .clip(shape)
            .background(if (clean) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceContainerLow)
            .then(
                if (clean) Modifier.border(neonOutline(), shape)
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
            )
            .clickable(interactionSource = interactions, indication = LocalIndication.current, onClick = onClick)
            .semantics { contentDescription = spoken },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            // The clean look drops the arrows: "Folders", "All apps", "Home".
            if (clean) label.filterNot { it in "←→↑↓" }.trim() else label,
            fontFamily = rowFont(),
            fontWeight = FontWeight.Bold,
            fontSize = if (clean) 19.sp else 16.sp,
            color = if (clean) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

/**
 * `[bat 54%] [wifi ●]` at the very top, in place of Android's status bar (hidden on the
 * home screen; swipe down from the top edge to bring it and notifications back).
 */
@Composable
private fun StatusLine(status: DeviceStatus, onTap: () -> Unit) {
    val parts = status.parts()
    if (parts.isEmpty()) return
    if (cleanLook()) {
        CleanStatus(status, onTap)
        return
    }
    val label = LocalTerminalColors.current.prompt
    Text(
        buildAnnotatedString {
            parts.forEachIndexed { i, (name, value) ->
                if (i > 0) append(" ")
                append("[")
                withStyle(SpanStyle(color = label, fontWeight = FontWeight.Bold)) { append(name) }
                append(" ")
                val level = status.batteryLevel
                if (name == "bat" && level != null) {
                    // Green, amber getting low, red low, like Android's battery.
                    withStyle(SpanStyle(color = batteryColor(level), fontWeight = FontWeight.Bold)) { append(value) }
                } else {
                    append(value)
                }
                append("]")
            }
        },
        fontFamily = NewtFonts.Mono,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(start = 12.dp, end = 12.dp, top = 2.dp)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            // Tapping it opens the full system card (the same as typing `sys`).
            .clickable(onClickLabel = "System info", onClick = onTap)
            .padding(horizontal = 8.dp, vertical = 14.dp)
            .clearAndSetSemantics {
                contentDescription = status.spoken()
                onClick(label = "System info") {
                    onTap()
                    true
                }
            },
    )
}

/** The clean look's status: "Wi-Fi · 82%" in a pill. Tap it for the system info. */
@Composable
private fun CleanStatus(status: DeviceStatus, onTap: () -> Unit) {
    val network = when (status.connection) {
        Connection.WIFI -> "Wi-Fi"
        Connection.MOBILE -> "Mobile data"
        Connection.OFFLINE -> "Offline"
        Connection.UNKNOWN -> null
    }
    val battery = status.battery?.let { percent ->
        "$percent%" + when {
            status.charging -> " charging"
            status.batteryLow -> " low"
            else -> ""
        }
    }
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 6.dp)
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClickLabel = "System info", onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clearAndSetSemantics {
                contentDescription = status.spoken()
                onClick(label = "System info") {
                    onTap()
                    true
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val text = MaterialTheme.colorScheme.onSurfaceVariant
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (network != null) {
                Text(network, fontFamily = NewtFonts.Body, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = text)
            }
            val level = status.batteryLevel
            if (battery != null && level != null) {
                if (network != null) Text("·", fontSize = 17.sp, color = text)
                BatteryBar(status.battery ?: 0, level)
                Text(
                    battery,
                    fontFamily = NewtFonts.Body,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (status.batteryLow) MaterialTheme.colorScheme.error else text,
                )
            }
            if (status.flashlight) {
                Text("· Flashlight on", fontFamily = NewtFonts.Body, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = text)
            }
        }
    }
}

/**
 * `newt@home:~$ date`: the terminal line above each section, for the look only. The clean
 * look shows [title] instead, as a heading, or nothing if there's none.
 */
@Composable
private fun PromptLine(command: String, short: Boolean = false, title: String? = null) {
    if (cleanLook()) {
        if (title != null) {
            Text(
                title,
                fontFamily = NewtFonts.Display,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 4.dp, top = 16.dp, bottom = 6.dp)
                    .semantics { heading() },
            )
        }
        return
    }
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = LocalTerminalColors.current.prompt, fontWeight = FontWeight.Bold)) {
                append(if (short) "~$ " else "newt@home:~$ ")
            }
            append(command)
        },
        fontFamily = NewtFonts.Mono,
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(top = 12.dp, bottom = 4.dp)
            .clearAndSetSemantics { },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClockAndDate(
    clock: String,
    date: String,
    onClock: () -> Unit,
    onDate: () -> Unit,
    modifier: Modifier = Modifier,
    dateLabel: String = "Open Calendar",
    onDateHold: () -> Unit = {},
    weather: (@Composable () -> Unit)? = null,
) {
    if (cleanLook()) {
        CleanClockAndDate(clock, date, onClock, onDate, modifier, dateLabel, onDateHold, weather)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClickLabel = "Open Clock", onClick = onClock)
                .padding(horizontal = 4.dp)
                .semantics { heading() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                clock,
                fontFamily = NewtFonts.Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 52.sp,
                // A soft neon halo, like a lit display.
                style = TextStyle(shadow = Shadow(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), blurRadius = 36f)),
            )
        }
        Spacer(Modifier.width(8.dp))
        // The date, with the weather under it when Newt Weather is sharing.
        Column {
        Box(
            Modifier
                .heightIn(min = 48.dp)
                .widthIn(min = 48.dp)
                .clip(RoundedCornerShape(10.dp))
                .combinedClickable(
                    onClickLabel = dateLabel,
                    onLongClickLabel = "Show or hide today's events",
                    onLongClick = onDateHold,
                    onClick = onDate,
                )
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(date, fontFamily = NewtFonts.Mono, fontSize = 20.sp, color = MaterialTheme.colorScheme.tertiary)
        }
        weather?.invoke()
        }
    }
}

/** The clean look's clock: big and plain, with the date under it. Each still opens its app. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CleanClockAndDate(
    clock: String,
    date: String,
    onClock: () -> Unit,
    onDate: () -> Unit,
    modifier: Modifier,
    dateLabel: String,
    onDateHold: () -> Unit,
    weather: (@Composable () -> Unit)?,
) {
    Column(modifier) {
        Box(
            Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClickLabel = "Open Clock", onClick = onClock)
                .padding(horizontal = 4.dp)
                .semantics { heading() },
        ) {
            Text(clock, fontFamily = NewtFonts.Display, fontWeight = FontWeight.Bold, fontSize = 72.sp, lineHeight = 76.sp)
        }
        Box(
            Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClickLabel = dateLabel,
                    onLongClickLabel = "Show or hide today's events",
                    onLongClick = onDateHold,
                    onClick = onDate,
                )
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(date, fontFamily = NewtFonts.Body, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        weather?.invoke()
    }
}

/**
 * The weather by the date: an icon, the temperature now, and today's high (↑, warm) or
 * tonight's low (↓, cool). Tapping it opens the hourly strip. Reads as one sentence.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WeatherLine(weather: WeatherNow, onClick: () -> Unit, onHear: () -> Unit) {
    val clean = cleanLook()
    val font = if (clean) NewtFonts.Body else NewtFonts.Mono
    val size = if (clean) 22.sp else 20.sp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClickLabel = "Hourly forecast",
                onLongClickLabel = "Hear the weather",
                onLongClick = onHear,
                onClick = onClick,
            )
            .semantics { contentDescription = WeatherText.spoken(weather) }
            .padding(horizontal = 4.dp),
    ) {
        Icon(
            painterResource(NewtWeather.icon(weather.condition, weather.isDaytime)),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(if (clean) 30.dp else 26.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            WeatherText.temperature(weather),
            fontFamily = font,
            fontSize = size,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.clearAndSetSemantics { },
        )
        WeatherText.highLow(weather)?.let {
            Spacer(Modifier.width(10.dp))
            Text(
                it,
                fontFamily = font,
                fontSize = size,
                fontWeight = FontWeight.Bold,
                color = if (weather.isHigh) NewtWeather.highColor() else NewtWeather.lowColor(),
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

/** "Rain likely around 3 PM": a heads-up from the hourly forecast. Opens the hourly strip. */
@Composable
private fun HeadsUpLine(text: String, onClick: () -> Unit) {
    val clean = cleanLook()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClickLabel = "Hourly forecast", onClick = onClick)
            .padding(horizontal = 4.dp),
    ) {
        Icon(
            painterResource(NewtWeather.icon("RAIN", true)),
            contentDescription = null,
            tint = NewtWeather.lowColor(),
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (clean) text else text.lowercase(),
            fontFamily = rowFont(),
            fontSize = if (clean) 19.sp else 17.sp,
            fontWeight = FontWeight.Bold,
            color = NewtWeather.lowColor(),
        )
    }
}

/** An active NOAA Warning, in the warning red under the clock; it opens the hourly sheet too. */
@Composable
private fun WarningLine(weather: WeatherNow, onClick: () -> Unit) {
    val text = WeatherText.warning(weather) ?: return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClickLabel = "Weather details", onClick = onClick)
            .padding(horizontal = 4.dp),
    ) {
        Icon(
            painterResource(io.github.secnewt.ui.R.drawable.ic_wx_warning),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, fontFamily = rowFont(), fontSize = if (cleanLook()) 19.sp else 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun Hint(text: String, top: Dp = 12.dp) {
    val clean = cleanLook()
    Text(
        // The clean look drops the `# ` and `└ ` that make a hint look like a terminal comment.
        if (clean) text.removePrefix("# ").removePrefix("└ ").replaceFirstChar { it.uppercase() } else text,
        fontFamily = rowFont(),
        fontSize = if (clean) 17.sp else 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = top, start = 4.dp),
    )
}

/**
 * The line under the date. Off: nothing. Offer: asks first, since it needs calendar access.
 * Turned on: "next: 3:00 Dentist · 2 more today", which opens the day's list like the date.
 */
@Composable
private fun AgendaLine(
    agenda: Agenda,
    now: Long,
    time: (Long) -> String,
    onOpen: () -> Unit,
    onChoice: (Boolean) -> Unit,
    onFix: () -> Unit,
) {
    when (agenda) {
        Agenda.Off -> Unit
        Agenda.Offer -> Column(Modifier.padding(top = 4.dp)) {
            Text(
                if (cleanLook()) "Show today's events here?" else "show today's events here?",
                fontFamily = rowFont(),
                fontSize = if (cleanLook()) 18.sp else 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NavButton("Yes", "Yes, show today's events", Modifier.weight(1f)) { onChoice(true) }
                NavButton("No thanks", "No thanks", Modifier.weight(1f)) { onChoice(false) }
            }
        }
        Agenda.NeedsPermission -> AgendaRow(
            if (cleanLook()) "Calendar access is off · tap to fix" else "calendar access is off · tap to fix",
            MaterialTheme.colorScheme.error,
            "Calendar access is off. Opens Newt Home's settings to turn it on.",
            onFix,
        )
        is Agenda.Today -> AgendaRow(
            AgendaText.line(agenda.events, now, time).let { if (cleanLook()) it.replaceFirstChar { c -> c.uppercase() } else it },
            if (cleanLook()) MaterialTheme.colorScheme.onSurface else LocalTerminalColors.current.prompt,
            null,
            onOpen,
            // The color of the calendar the next event is in.
            marker = AgendaText.next(AgendaText.upcoming(agenda.events, now), now)?.color,
        )
    }
}

@Composable
private fun AgendaRow(text: String, color: Color, spoken: String?, onClick: () -> Unit, marker: Int? = null) {
    val clean = cleanLook()
    val card = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .fillMaxWidth()
            // The clean look puts it on a card, like Newt Phone's.
            .then(
                if (clean) {
                    Modifier
                        .padding(top = 10.dp)
                        .clip(card)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(neonOutline(), card)
                } else {
                    Modifier
                },
            )
            .heightIn(min = if (clean) 60.dp else 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClickLabel = if (spoken == null) "Today's events" else null, onClick = onClick)
            .padding(horizontal = 4.dp)
            .then(if (spoken != null) Modifier.semantics { contentDescription = spoken } else Modifier),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = if (clean) Modifier.padding(horizontal = 12.dp, vertical = 10.dp) else Modifier,
        ) {
            if (marker != null) {
                CalendarDot(marker)
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text,
                fontFamily = rowFont(),
                fontSize = if (clean) 19.sp else 16.sp,
                fontWeight = if (clean) FontWeight.Bold else null,
                color = color,
            )
        }
    }
}

/** The blue-to-purple outline used across Newt apps. */
@Composable
internal fun neonOutline(width: Dp = 1.5.dp) = BorderStroke(
    width,
    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)),
)

/** Each app keeps the same accent color, picked from its name. */
@Composable
private fun accentFor(name: String): Color {
    val accents = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        LocalTerminalColors.current.prompt,
    )
    return accents[name.hashCode().absoluteValue % accents.size]
}

/** The app's one-color icon in its accent color, or its first letter if it has none. */
@Composable
fun AppIcon(app: LauncherApp, size: Dp = 34.dp) {
    val color = accentFor(app.name)
    // Terminal: an outlined square. Clean: a soft filled tile, like Newt Phone's.
    val tile = if (cleanLook()) {
        Modifier
            .clip(RoundedCornerShape(size * 0.3f))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    } else {
        Modifier.border(1.5.dp, color, RoundedCornerShape(8.dp))
    }
    Box(
        Modifier
            .size(size)
            .then(tile),
        contentAlignment = Alignment.Center,
    ) {
        val icon = app.icon
        if (icon != null) {
            Image(icon, contentDescription = null, colorFilter = ColorFilter.tint(color), modifier = Modifier.size(size * 0.7f))
        } else {
            Text(
                app.name.take(1).uppercase(),
                fontFamily = NewtFonts.Mono,
                fontWeight = FontWeight.Bold,
                fontSize = if (size > 40.dp) 19.sp else 16.sp,
                color = color,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
    app: LauncherApp,
    onOpen: (LauncherApp) -> Unit,
    onHold: (LauncherApp) -> Unit,
    note: String? = null,
    highlighted: Boolean = false,
    compact: Boolean = false,
    branch: String? = null,
) {
    val haptics = LocalHapticFeedback.current
    val clean = cleanLook()
    val shape = RoundedCornerShape(if (clean) 14.dp else 10.dp)
    val interactions = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = if (clean) 60.dp else 48.dp)
            .glow(pressed(interactions), MaterialTheme.colorScheme.primary, neonOutline(), shape)
            .clip(shape)
            .then(if (highlighted) Modifier.border(neonOutline(), shape) else Modifier)
            .combinedClickable(
                interactionSource = interactions,
                indication = LocalIndication.current,
                onClickLabel = "Open",
                onLongClickLabel = "More options",
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onHold(app)
                },
                onClick = { onOpen(app) },
            )
            .padding(horizontal = if (compact) 4.dp else 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (branch != null) {
            // Apps inside an open folder: tree lines in the terminal look, an indent in the clean one.
            if (clean) Spacer(Modifier.width(16.dp)) else Branch(branch)
        }
        AppIcon(app, size = if (clean) 44.dp else if (compact) 30.dp else 34.dp)
        Spacer(Modifier.width(if (clean) 16.dp else if (compact) 8.dp else 12.dp))
        Column {
            Text(
                app.name,
                fontFamily = rowFont(),
                fontWeight = if (clean) FontWeight.Bold else null,
                fontSize = if (clean) 21.sp else if (compact) 17.sp else 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            // A shortcut says where it came from, like "Vanadium shortcut", so it's not mistaken for an app.
            val source = app.url?.let { "website · " + (Websites.host(it) ?: it) }
                ?: app.from?.let { "$it shortcut" }
                ?: app.profile.takeIf { it != ProfileKind.MAIN }?.title?.lowercase()
            val line = listOfNotNull(source, note).joinToString(" · ").ifEmpty { null }
            val alert = app.alert
            if (alert != null) {
                // A word, not just a red dot, so it's never color alone.
                Text(alert, fontFamily = rowFont(), fontSize = if (clean) 17.sp else 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
            if (line != null) {
                Text(line, fontFamily = rowFont(), fontSize = if (clean) 16.sp else 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * `> timer 10` with what it will do, like "start a 10 min timer · Enter runs it". A
 * command not finished yet says what it needs instead, and does nothing when tapped.
 */
@Composable
private fun CommandRow(typed: String, command: CommandView, onRun: () -> Unit) {
    if (cleanLook()) {
        CleanCommandRow(typed, command, onRun)
        return
    }
    val shape = RoundedCornerShape(10.dp)
    val interactions = remember { MutableInteractionSource() }
    val prompt = LocalTerminalColors.current.prompt
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .heightIn(min = 52.dp)
            .glow(command.ready && pressed(interactions), prompt, neonOutline(), shape)
            .clip(shape)
            .then(if (command.ready) Modifier.border(neonOutline(), shape) else Modifier)
            .then(
                if (command.ready) {
                    Modifier.clickable(
                        interactionSource = interactions,
                        indication = LocalIndication.current,
                        onClickLabel = "Run",
                        onClick = onRun,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            ">_",
            fontFamily = NewtFonts.Mono,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = prompt,
            modifier = Modifier
                .widthIn(min = 34.dp)
                .clearAndSetSemantics { },
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                typed,
                fontFamily = NewtFonts.Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                if (command.ready) "${command.text} · Enter runs it" else "# ${command.text}",
                fontFamily = NewtFonts.Mono,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (command.ready) prompt else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The clean look's command: a card that lights up with the blue-to-purple gradient when it's
 * ready ("Start a 10 min timer · Tap, or press Enter"), or says what's still needed.
 */
@Composable
private fun CleanCommandRow(typed: String, command: CommandView, onRun: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .heightIn(min = 64.dp)
            .clip(shape)
            .then(
                if (command.ready) {
                    Modifier
                        .background(Brush.linearGradient(listOf(colors.primary, colors.secondary)))
                        .clickable(onClickLabel = "Run", onClick = onRun)
                } else {
                    Modifier
                        .background(colors.surfaceContainerLow)
                        .border(1.dp, colors.outlineVariant, shape)
                },
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        val text = if (command.ready) colors.onPrimary else colors.onSurface
        Text(
            if (command.ready) command.text.replaceFirstChar { it.uppercase() } else typed,
            fontFamily = NewtFonts.Body,
            fontWeight = FontWeight.Bold,
            fontSize = 21.sp,
            color = text,
        )
        Text(
            if (command.ready) "Tap, or press Enter" else command.text.replaceFirstChar { it.uppercase() },
            fontFamily = NewtFonts.Body,
            fontSize = 17.sp,
            color = if (command.ready) text else colors.onSurfaceVariant,
        )
    }
}

/** The tree's `├`/`└` lines, for the look only. */
@Composable
private fun Branch(symbol: String) {
    Text(
        symbol,
        fontFamily = NewtFonts.Mono,
        fontSize = 17.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(start = 6.dp, end = 4.dp)
            .clearAndSetSemantics { },
    )
}

/** Material's microphone shape. */
private val MicShape: ImageVector by lazy {
    ImageVector.Builder(
        name = "Mic",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(
            "M12,14c1.66,0 2.99,-1.34 2.99,-3L15,5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5v6c0,1.66 1.34,3 3,3z" +
                "M17.3,11c0,3 -2.54,5.1 -5.3,5.1S6.7,14 6.7,11H5c0,3.41 2.72,6.23 6,6.72V21h2v-3.28c3.28,-0.48 6,-3.3 6,-6.72h-1.7z",
        ),
        fill = SolidColor(Color.Black),
    ).build()
}

/** Material's folder shape. */
internal val FolderShape: ImageVector by lazy {
    ImageVector.Builder(
        name = "Folder",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(
            "M10,4H4c-1.1,0 -1.99,0.9 -1.99,2L2,18c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2h-8l-2,-2z",
        ),
        fill = SolidColor(Color.Black),
    ).build()
}

@Composable
fun FolderIcon(size: Dp = 30.dp, color: Color = MaterialTheme.colorScheme.primary) {
    // Terminal: outlined like the app icons. Clean: a soft filled tile.
    val tile = if (cleanLook()) {
        Modifier
            .clip(RoundedCornerShape(size * 0.3f))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    } else {
        Modifier.border(1.5.dp, color, RoundedCornerShape(8.dp))
    }
    Box(
        Modifier
            .size(size)
            .then(tile),
        contentAlignment = Alignment.Center,
    ) {
        Icon(FolderShape, contentDescription = null, tint = color, modifier = Modifier.size(size * 0.6f))
    }
}

/** A folder in the tree. Tap to open or close it; press and hold for its menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderRow(
    folder: FolderView,
    open: Boolean,
    color: Color,
    onToggle: (String) -> Unit,
    onHold: (String) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val clean = cleanLook()
    val shape = RoundedCornerShape(if (clean) 16.dp else 10.dp)
    val interactions = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            // The clean look makes each folder a card; the open one gets the neon outline.
            .then(if (clean) Modifier.padding(top = 10.dp) else Modifier)
            .heightIn(min = if (clean) 64.dp else 48.dp)
            .glow(pressed(interactions), color, neonOutline(), shape)
            .clip(shape)
            .then(
                when {
                    !clean -> Modifier
                    open -> Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow).border(neonOutline(), shape)
                    else -> Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                },
            )
            .combinedClickable(
                interactionSource = interactions,
                indication = LocalIndication.current,
                onClickLabel = if (open) "Close folder" else "Open folder",
                onLongClickLabel = "Folder options",
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onHold(folder.name)
                },
                onClick = { onToggle(folder.name) },
            )
            .semantics {
                stateDescription = if (open) "open, ${folder.apps.size} apps" else "closed, ${folder.apps.size} apps"
            }
            .padding(horizontal = if (clean) 16.dp else 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (clean) {
            Icon(FolderShape, contentDescription = null, tint = color, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(14.dp))
            Text(
                folder.name,
                fontFamily = NewtFonts.Body,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (folder.apps.size == 1) "1 app" else "${folder.apps.size} apps",
                fontFamily = NewtFonts.Body,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clearAndSetSemantics { },
            )
            return@Row
        }
        Text(
            if (open) "▾" else "▸",
            fontFamily = NewtFonts.Mono,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(end = 4.dp)
                .clearAndSetSemantics { },
        )
        FolderIcon(color = color)
        Spacer(Modifier.width(8.dp))
        Text(
            "${folder.name}/",
            fontFamily = NewtFonts.Mono,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = color,
        )
    }
}

/** `+ New folder` at the end of the tree. */
@Composable
private fun NewFolderRow(onNewFolder: () -> Unit) = AddRow("New folder", onNewFolder)

/** "+ New folder", "+ Add a website": a row that adds something. */
@Composable
private fun AddRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) { append("+ ") }
                append(label)
            },
            fontFamily = rowFont(),
            fontWeight = if (cleanLook()) FontWeight.Bold else null,
            fontSize = if (cleanLook()) 19.sp else 17.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** The `~$` prompt. Capitals and autocorrect are off: it's for app names, not sentences. */
@Composable
private fun Prompt(query: String, onQueryChange: (String) -> Unit, onSubmit: () -> Unit, onVoice: () -> Unit) {
    val clean = cleanLook()
    // Clean: a rounded search field, purple at rest and blue while typing, like Newt Phone's.
    val shape = RoundedCornerShape(if (clean) 30.dp else 16.dp)
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 10.dp)
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .glow(focused && !clean, MaterialTheme.colorScheme.primary, neonOutline(), shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .then(
                if (clean) {
                    Modifier.border(2.dp, if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, shape)
                } else {
                    Modifier.border(neonOutline(), shape)
                },
            )
            .padding(start = if (clean) 16.dp else 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (clean) 10.dp else 8.dp),
    ) {
        if (clean) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(26.dp))
        } else {
            Text(
                "~$",
                fontFamily = NewtFonts.Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = LocalTerminalColors.current.prompt,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
        val style = TextStyle(
            fontFamily = rowFont(),
            fontWeight = if (clean) FontWeight.Normal else FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Go,
            ),
            keyboardActions = KeyboardActions(onGo = { onSubmit() }),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .onFocusChanged { focused = it.isFocused }
                .semantics { contentDescription = "Type an app name" },
            decorationBox = { field ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            if (clean) "Search, or type help" else "type an app, or help…",
                            style = style.copy(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant),
                        )
                    }
                    field()
                }
            },
        )
        if (query.isNotEmpty()) {
            TextButton(onClick = { onQueryChange("") }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Clear", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        } else {
            // Speak instead of typing: the phone's voice input app (like FUTO Voice Input)
            // listens and hands back the words; Newt Home never uses the microphone itself.
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClickLabel = "Speak", onClick = onVoice)
                    .semantics { contentDescription = "Speak instead of typing" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(MicShape, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
            }
        }
    }
}
