package io.github.secnewt.home.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
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
import io.github.secnewt.home.apps.DeviceStatus
import io.github.secnewt.home.apps.LauncherApp
import io.github.secnewt.home.ui.theme.LocalTerminalColors
import io.github.secnewt.home.ui.theme.NewtFonts
import kotlin.math.absoluteValue

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
 * up for all apps; the buttons above the prompt do the same, so swiping is never required.
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
        GadgetBackground()
        Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            StatusLine(status)
            Box(Modifier.weight(1f)) {
                HorizontalPager(
                    state = pager,
                    userScrollEnabled = !appsOpen,
                    // Hidden behind all apps: screen readers shouldn't reach it.
                    modifier = if (appsOpen) Modifier.clearAndSetSemantics { } else Modifier,
                ) { page ->
                    if (page == 0) {
                        FoldersPage(folders, openFolder, onOpen, onHold, onToggleFolder, onHoldFolder, onNewFolder) {
                            onScreen(Screen.HOME)
                        }
                    } else {
                        HomePage(
                            clock, date, view, favorites, query, opening, onQueryChange, onSubmit, onOpen, onHold,
                            onClock, onDate, folderOf, bootKey, animateBoot, onScreen,
                            agenda, now, time, onAgendaChoice, onFixCalendar, onDateHold, isHidden,
                        )
                    }
                }
                // The plain version, not the Column one (this sits inside a Box inside a Column).
                androidx.compose.animation.AnimatedVisibility(
                    visible = appsOpen,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    AppsPage(allApps, hiddenApps, onOpen, onHold) { onScreen(Screen.HOME) }
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
) {
    val pullUp = rememberEdgePull(up = true) { onScreen(Screen.APPS) }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .nestedScroll(pullUp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item {
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
                            )
                            AgendaLine(agenda, now, time, onDate, onAgendaChoice, onFixCalendar)
                        }
                    }
                }
            }
            when (view) {
                HomeView.Favorites -> {
                    item { PromptLine("ls favs", short = true) }
                    if (favorites.isEmpty()) {
                        item { Hint("# press and hold any app to add it here") }
                    }
                    items(favorites, key = { it.key }) { AppRow(it, onOpen = onOpen, onHold = onHold) }
                }
                is HomeView.Results -> {
                    val command = view.command
                    if (command != null) {
                        item(key = "command") { CommandRow(view.query.trim(), command, onSubmit) }
                    }
                    item { PromptLine("find ${view.query.trim()}") }
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
            }
        }
        NavRow {
            NavButton("← Folders", "Folders", Modifier.weight(1f)) { onScreen(Screen.FOLDERS) }
            NavButton("All apps ↑", "All apps", Modifier.weight(1f)) { onScreen(Screen.APPS) }
        }
        Prompt(query, onQueryChange, onSubmit)
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
    onHome: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item { PromptLine("tree", short = true) }
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
        NavRow { NavButton("Home →", "Home", Modifier.weight(1f), onHome) }
    }
}

@Composable
private fun AppsPage(
    allApps: List<LauncherApp>,
    hiddenApps: List<LauncherApp>,
    onOpen: (LauncherApp) -> Unit,
    onHold: (LauncherApp) -> Unit,
    onHome: () -> Unit,
) {
    val pullDown = rememberEdgePull(up = false, onPull = onHome)
    var showHidden by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        GadgetBackground()
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .nestedScroll(pullDown),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                item { PromptLine("ls") }
                var letter = ""
                for (app in allApps) {
                    // Grouped the way the list is sorted (accents ignored), so each letter appears once.
                    val first = AppSearch.normalize(app.name).take(1).uppercase()
                    if (first != letter) {
                        letter = first
                        item(key = "letter:$first") { LetterHeader(first) }
                    }
                    item(key = app.key) { AppRow(app, onOpen = onOpen, onHold = onHold) }
                }
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
            }
            NavRow { NavButton("↓ Home", "Home", Modifier.weight(1f), onHome) }
        }
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
            fontFamily = NewtFonts.Mono,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun LetterHeader(letter: String) {
    Text(
        letter,
        fontFamily = NewtFonts.Mono,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier
            .padding(start = 4.dp, top = 12.dp, bottom = 2.dp)
            .semantics { heading() },
    )
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
    val shape = RoundedCornerShape(12.dp)
    val interactions = remember { MutableInteractionSource() }
    Box(
        modifier
            .heightIn(min = 52.dp)
            .glow(pressed(interactions), MaterialTheme.colorScheme.primary, neonOutline(), shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(interactionSource = interactions, indication = LocalIndication.current, onClick = onClick)
            .semantics { contentDescription = spoken },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontFamily = NewtFonts.Mono,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

/**
 * `[bat 54%] [wifi ●]` at the very top, in place of Android's status bar (hidden on the
 * home screen; swipe down from the top edge to bring it and notifications back).
 */
@Composable
private fun StatusLine(status: DeviceStatus) {
    val parts = status.parts()
    if (parts.isEmpty()) return
    val label = LocalTerminalColors.current.prompt
    val warn = MaterialTheme.colorScheme.error
    Text(
        buildAnnotatedString {
            parts.forEachIndexed { i, (name, value) ->
                if (i > 0) append(" ")
                append("[")
                withStyle(SpanStyle(color = label, fontWeight = FontWeight.Bold)) { append(name) }
                append(" ")
                if (name == "bat" && status.batteryLow) {
                    withStyle(SpanStyle(color = warn, fontWeight = FontWeight.Bold)) { append(value) }
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
            .padding(start = 20.dp, end = 20.dp, top = 10.dp)
            .clearAndSetSemantics { contentDescription = status.spoken() },
    )
}

/** `newt@home:~$ date`: the terminal line above each section, for the look only. */
@Composable
private fun PromptLine(command: String, short: Boolean = false) {
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
) {
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
    }
}

@Composable
private fun Hint(text: String, top: Dp = 12.dp) {
    Text(
        text,
        fontFamily = NewtFonts.Mono,
        fontSize = 14.sp,
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
                "show today's events here?",
                fontFamily = NewtFonts.Mono,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NavButton("Yes", "Yes, show today's events", Modifier.weight(1f)) { onChoice(true) }
                NavButton("No thanks", "No thanks", Modifier.weight(1f)) { onChoice(false) }
            }
        }
        Agenda.NeedsPermission -> AgendaRow(
            "calendar access is off · tap to fix",
            MaterialTheme.colorScheme.error,
            "Calendar access is off. Opens Newt Home's settings to turn it on.",
            onFix,
        )
        is Agenda.Today -> AgendaRow(
            AgendaText.line(agenda.events, now, time),
            LocalTerminalColors.current.prompt,
            null,
            onOpen,
        )
    }
}

@Composable
private fun AgendaRow(text: String, color: Color, spoken: String?, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClickLabel = if (spoken == null) "Today's events" else null, onClick = onClick)
            .padding(horizontal = 4.dp)
            .then(if (spoken != null) Modifier.semantics { contentDescription = spoken } else Modifier),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text, fontFamily = NewtFonts.Mono, fontSize = 16.sp, color = color)
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
    Box(
        Modifier
            .size(size)
            .border(1.5.dp, color, RoundedCornerShape(8.dp)),
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
                fontSize = 16.sp,
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
    val shape = RoundedCornerShape(10.dp)
    val interactions = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
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
        if (branch != null) Branch(branch)
        AppIcon(app, size = if (compact) 30.dp else 34.dp)
        Spacer(Modifier.width(if (compact) 8.dp else 12.dp))
        Column {
            Text(
                app.name,
                fontFamily = NewtFonts.Mono,
                fontSize = if (compact) 17.sp else 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (note != null) {
                Text(note, fontFamily = NewtFonts.Mono, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

/** Material's folder shape. */
private val FolderShape: ImageVector by lazy {
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
    Box(
        Modifier
            .size(size)
            .border(1.5.dp, color, RoundedCornerShape(8.dp)),
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
    val shape = RoundedCornerShape(10.dp)
    val interactions = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .glow(pressed(interactions), color, neonOutline(), shape)
            .clip(shape)
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
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
private fun NewFolderRow(onNewFolder: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onNewFolder)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) { append("+ ") }
                append("New folder")
            },
            fontFamily = NewtFonts.Mono,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** The `~$` prompt. Capitals and autocorrect are off: it's for app names, not sentences. */
@Composable
private fun Prompt(query: String, onQueryChange: (String) -> Unit, onSubmit: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 10.dp)
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .glow(focused, MaterialTheme.colorScheme.primary, neonOutline(), shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(neonOutline(), shape)
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "~$",
            fontFamily = NewtFonts.Mono,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = LocalTerminalColors.current.prompt,
            modifier = Modifier.clearAndSetSemantics { },
        )
        val style = TextStyle(
            fontFamily = NewtFonts.Mono,
            fontWeight = FontWeight.Bold,
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
                            "type an app, or help…",
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
        }
    }
}
