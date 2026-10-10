package io.github.secnewt.home.ui

import androidx.compose.foundation.BorderStroke
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
import io.github.secnewt.home.apps.AppSearch
import io.github.secnewt.home.apps.LauncherApp
import io.github.secnewt.home.ui.theme.LocalTerminalColors
import io.github.secnewt.home.ui.theme.NewtFonts
import kotlin.math.absoluteValue

/** A folder as the tree shows it: its name and its apps, A–Z. */
@Immutable
data class FolderView(val name: String, val apps: List<LauncherApp>)

/** What the output area above the prompt is showing. */
sealed interface HomeView {
    /** The resting home screen: favorites and the folder tree. */
    data object Favorites : HomeView

    /** Every app, A–Z (`ls`). */
    data object AllApps : HomeView

    /** Search results for what's typed at the prompt. */
    data class Results(val query: String, val results: List<AppSearch.Result<LauncherApp>>) : HomeView
}

/**
 * The home screen: a terminal session you can tap. The time and date sit at the top (each
 * opens its app), the output in the middle, and the `~$` prompt at the bottom.
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
    onShowAll: () -> Unit,
    folderOf: (LauncherApp) -> String? = { null },
    bootKey: Int = 0,
    animateBoot: Boolean = false,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        GadgetBackground()
        Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                item {
                    Column(Modifier.padding(top = 12.dp)) {
                        BootLine("date", bootKey, animateBoot) { alpha ->
                            ClockAndDate(clock, date, onClock, onDate, Modifier.bootFade(alpha))
                        }
                    }
                }
                when (view) {
                    HomeView.Favorites -> {
                        item {
                            HomeColumns(
                                favorites = {
                                    PromptLine("ls favs", short = true)
                                    if (favorites.isEmpty()) Hint("# press and hold any app to add it here")
                                    for (app in favorites) AppRow(app, onOpen = onOpen, onHold = onHold, compact = true)
                                    AllAppsRow(onShowAll)
                                },
                                tree = {
                                    PromptLine("tree", short = true)
                                    for ((index, folder) in folders.withIndex()) {
                                        val open = folder.name == openFolder
                                        FolderRow(folder, open, folderColor(index), onToggleFolder, onHoldFolder)
                                        if (open) {
                                            if (folder.apps.isEmpty()) {
                                                Hint("└ empty. press and hold an app, then Move to folder", top = 0.dp)
                                            }
                                            for ((j, app) in folder.apps.withIndex()) {
                                                val branch = if (j == folder.apps.lastIndex) "└" else "├"
                                                AppRow(app, onOpen = onOpen, onHold = onHold, compact = true, branch = branch)
                                            }
                                        }
                                    }
                                    NewFolderRow(onNewFolder)
                                },
                            )
                        }
                        item { Hint("# or type a name below") }
                    }
                    HomeView.AllApps -> {
                        item { PromptLine("ls") }
                        items(allApps, key = { it.key }) { AppRow(it, onOpen = onOpen, onHold = onHold) }
                    }
                    is HomeView.Results -> {
                        item { PromptLine("find ${view.query.trim()}") }
                        if (view.results.isEmpty()) {
                            item { Hint("# no app matches “${view.query.trim()}”") }
                        }
                        itemsIndexed(view.results, key = { _, r -> r.item.key }) { i, r ->
                            val note = when {
                                i > 0 -> null
                                r.item == opening -> "only match · opening…"
                                else -> "Enter opens"
                            }
                            val why = if (r.match == AppSearch.Match.IN_FOLDER) folderOf(r.item)?.let { "in $it/" } else null
                            AppRow(
                                r.item,
                                note = listOfNotNull(why, note).joinToString(" · ").ifEmpty { null },
                                highlighted = i == 0,
                                onOpen = onOpen,
                                onHold = onHold,
                            )
                        }
                    }
                }
            }
            Prompt(query, onQueryChange, onSubmit)
        }
    }
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

@Composable
private fun ClockAndDate(clock: String, date: String, onClock: () -> Unit, onDate: () -> Unit, modifier: Modifier = Modifier) {
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
                .clickable(onClickLabel = "Open Calendar", onClick = onDate)
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
 * Favorites on the left and the folder tree on the right. With large text (150% and up) the
 * columns would be too narrow to read, so they stack: favorites first, then folders.
 */
@Composable
private fun HomeColumns(favorites: @Composable () -> Unit, tree: @Composable () -> Unit) {
    if (LocalDensity.current.fontScale >= 1.5f) {
        Column {
            favorites()
            tree()
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.weight(0.9f)) { favorites() }
            Column(Modifier.weight(1.1f)) { tree() }
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

/** `› ls` — every app, for when you don't want to type. */
@Composable
private fun AllAppsRow(onShowAll: () -> Unit) {
    Row(
        Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onShowAll)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) { append("› ") }
                append("All apps")
            },
            fontFamily = NewtFonts.Mono,
            fontSize = 18.sp,
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
                            "type an app…",
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
