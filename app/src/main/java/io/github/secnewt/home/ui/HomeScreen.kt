package io.github.secnewt.home.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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

/** What the output area above the prompt is showing. */
sealed interface HomeView {
    /** The resting home screen: favorites. */
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
    allApps: List<LauncherApp>,
    query: String,
    opening: LauncherApp?,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onOpen: (LauncherApp) -> Unit,
    onHold: (LauncherApp) -> Unit,
    onClock: () -> Unit,
    onDate: () -> Unit,
    onShowAll: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                item { PromptLine("date") }
                item { ClockAndDate(clock, date, onClock, onDate) }
                when (view) {
                    HomeView.Favorites -> {
                        item { PromptLine("ls favs") }
                        if (favorites.isEmpty()) {
                            item { Hint("# no favorites yet. press and hold any app to add it here.") }
                        }
                        items(favorites, key = { it.key }) { AppRow(it, onOpen = onOpen, onHold = onHold) }
                        item { AllAppsRow(onShowAll) }
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
                            AppRow(r.item, note = note, highlighted = i == 0, onOpen = onOpen, onHold = onHold)
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
private fun PromptLine(command: String) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = LocalTerminalColors.current.prompt, fontWeight = FontWeight.Bold)) {
                append("newt@home:~$ ")
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
private fun ClockAndDate(clock: String, date: String, onClock: () -> Unit, onDate: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClickLabel = "Open Clock", onClick = onClock)
                .padding(horizontal = 4.dp)
                .semantics { heading() },
            contentAlignment = Alignment.Center,
        ) {
            Text(clock, fontFamily = NewtFonts.Mono, fontWeight = FontWeight.Bold, fontSize = 52.sp)
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
private fun Hint(text: String) {
    Text(
        text,
        fontFamily = NewtFonts.Mono,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp),
    )
}

/** The blue-to-purple outline used across Newt apps. */
@Composable
private fun neonOutline(width: Dp = 1.5.dp) = BorderStroke(
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
) {
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(shape)
            .then(if (highlighted) Modifier.border(neonOutline(), shape) else Modifier)
            .combinedClickable(
                onClickLabel = "Open",
                onLongClickLabel = "More options",
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onHold(app)
                },
                onClick = { onOpen(app) },
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(app.name, fontFamily = NewtFonts.Mono, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            if (note != null) {
                Text(note, fontFamily = NewtFonts.Mono, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
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
    Row(
        Modifier
            .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 10.dp)
            .fillMaxWidth()
            .heightIn(min = 60.dp)
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

/**
 * The press-and-hold menu: every choice is a word with one plain line saying what it does.
 * Tapping outside it, Cancel, or Back closes it.
 */
@Composable
fun HoldMenu(
    app: LauncherApp,
    isFavorite: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggleFavorite: () -> Unit,
    onMove: (Int) -> Unit,
    onAppInfo: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                // Taps on the menu itself don't close it.
                .pointerInput(Unit) { detectTapGestures { } }
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                AppIcon(app, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    app.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
            }
            if (isFavorite) {
                MenuOption("Remove from favorites", "It stays in All apps", onToggleFavorite)
                if (canMoveUp) MenuOption("Move up", "Higher in your favorites", { onMove(-1) })
                if (canMoveDown) MenuOption("Move down", "Lower in your favorites", { onMove(1) })
            } else {
                MenuOption("Add to favorites", "Puts it on your home screen", onToggleFavorite)
            }
            MenuOption("App info", "Android's page for this app", onAppInfo)
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(top = 4.dp),
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun MenuOption(title: String, explanation: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(explanation, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
