package io.github.secnewt.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import io.github.secnewt.home.apps.SystemInfoText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.secnewt.home.apps.AgendaText
import io.github.secnewt.home.apps.AppShortcut
import io.github.secnewt.home.apps.CommandHelp
import io.github.secnewt.home.apps.DayEvent
import io.github.secnewt.home.apps.Folders
import io.github.secnewt.home.apps.LauncherApp
import io.github.secnewt.home.apps.SearchWords
import io.github.secnewt.home.apps.SystemInfo
import io.github.secnewt.home.apps.Websites
import io.github.secnewt.home.ui.theme.NewtFonts

/**
 * The home screen with a [menu] over it while [menuOpen]. While a menu is open, everything
 * behind it is hidden from screen readers, so they can't wander onto apps under the dimmed
 * background.
 */
@Composable
fun WithHoldMenu(menuOpen: Boolean, menu: @Composable () -> Unit, content: @Composable () -> Unit) {
    Box {
        Box(if (menuOpen) Modifier.clearAndSetSemantics { } else Modifier) { content() }
        if (menuOpen) menu()
    }
}

/**
 * A sheet rising from the bottom over a dimmed screen, with a [header] and the menu's
 * choices. Swiping it down, tapping outside it, Cancel, or Back closes it.
 */
@Composable
private fun MenuSheet(
    onDismiss: () -> Unit,
    header: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val swipe = rememberSwipeDismiss(onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .offset { IntOffset(0, swipe.offset.roundToInt()) }
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                // Taps on the menu itself don't close it.
                .pointerInput(Unit) { detectTapGestures { } }
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .nestedScroll(swipe.connection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            // The grab bar: a hint that it swipes down. For the look only.
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 10.dp)
                    .width(44.dp)
                    .heightIn(min = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
                    .clearAndSetSemantics { },
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                header()
            }
            content()
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun SheetButton(text: String, onClick: () -> Unit) {
    if (cleanLook()) {
        // A solid button, like Newt Phone's Cancel and Close.
        val shape = RoundedCornerShape(14.dp)
        Box(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        return
    }
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(top = 4.dp),
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

private enum class AppMenuPage { MAIN, PICK_FOLDER, NEW_FOLDER, WORDS }

/**
 * The press-and-hold menu for an app: every choice is a word with one plain line saying
 * what it does. "Move to folder" opens a list of folders, with "New folder" at the end.
 */
@Composable
fun HoldMenu(
    app: LauncherApp,
    isFavorite: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    folderNames: List<String>,
    currentFolder: String?,
    onToggleFavorite: () -> Unit,
    onMove: (Int) -> Unit,
    onMoveToFolder: (String?) -> Unit,
    onMoveToNewFolder: (String) -> Unit,
    onAppInfo: () -> Unit,
    onDismiss: () -> Unit,
    isHidden: Boolean = false,
    words: List<String> = emptyList(),
    onToggleHidden: () -> Unit = {},
    onSetWords: (List<String>) -> Unit = {},
    onRemoveShortcut: () -> Unit = {},
    shortcuts: List<AppShortcut> = emptyList(),
    onOpenShortcut: (AppShortcut) -> Unit = {},
) {
    var page by rememberSaveable { mutableStateOf(AppMenuPage.MAIN) }
    MenuSheet(
        onDismiss = onDismiss,
        header = {
            AppIcon(app, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            SheetTitle(app.name)
        },
    ) {
        when (page) {
            AppMenuPage.MAIN -> {
                // The app's own shortcuts first: they're what you'd hold an app to reach.
                if (shortcuts.isNotEmpty()) {
                    MenuHeading("Jump to")
                    if (cleanLook()) {
                        ShortcutChips(shortcuts, app.name, onOpenShortcut)
                    } else {
                        for (shortcut in shortcuts) {
                            MenuOption(
                                shortcut.label,
                                shortcut.description ?: "In ${app.name}",
                                { onOpenShortcut(shortcut) },
                            )
                        }
                    }
                    MenuHeading("Options")
                }
                if (isFavorite) {
                    MenuOption("Remove from favorites", "It stays in All apps", onToggleFavorite, icon = Icons.Filled.Star)
                    if (canMoveUp) MenuOption("Move up", "Higher in your favorites", { onMove(-1) }, icon = Icons.Filled.KeyboardArrowUp)
                    if (canMoveDown) MenuOption("Move down", "Lower in your favorites", { onMove(1) }, icon = Icons.Filled.KeyboardArrowDown)
                } else {
                    MenuOption("Add to favorites", "Puts it on your home screen", onToggleFavorite, icon = Icons.Filled.Star)
                }
                MenuOption(
                    "Move to folder",
                    if (currentFolder != null) "Now in ${folderLabel(currentFolder)}" else "Not in a folder yet",
                    { page = AppMenuPage.PICK_FOLDER },
                    icon = FolderShape,
                )
                MenuOption(
                    if (words.isEmpty()) "Add search words" else "Search words",
                    if (words.isEmpty()) "Your own words that find it, like bank or maps" else words.joinToString(", "),
                    { page = AppMenuPage.WORDS },
                    icon = Icons.Filled.Search,
                )
                if (isHidden) {
                    MenuOption("Show in lists", "Back in All apps and its folder", onToggleHidden, icon = Icons.Filled.Check)
                } else {
                    MenuOption("Hide from lists", "Leaves All apps and folders. Search still finds it", onToggleHidden, icon = Icons.Filled.Close)
                }
                val from = app.from
                val url = app.url
                if (url != null) {
                    MenuOption("Remove website", "Takes it off Newt Home. Opens $url", onRemoveShortcut, danger = true, icon = Icons.Filled.Delete)
                } else if (from != null) {
                    MenuOption(
                        "Remove shortcut",
                        "Takes it off Newt Home. You can add it again from $from",
                        onRemoveShortcut,
                        danger = true,
                        icon = Icons.Filled.Delete,
                    )
                    MenuOption("$from info", "Android's page for $from, which made this shortcut", onAppInfo, icon = Icons.Filled.Info)
                } else {
                    MenuOption("App info", "Android's page for this app", onAppInfo, icon = Icons.Filled.Info)
                }
                SheetButton("Cancel", onDismiss)
            }
            AppMenuPage.WORDS -> WordsField(
                appName = app.name,
                initial = words,
                onSave = onSetWords,
                onBack = { page = AppMenuPage.MAIN },
            )
            AppMenuPage.PICK_FOLDER -> {
                for (name in folderNames) {
                    MenuOption(
                        folderLabel(name),
                        if (name == currentFolder) "It's here now" else "Move it here",
                        { onMoveToFolder(name) },
                        icon = FolderShape,
                    )
                }
                if (currentFolder != null) {
                    MenuOption("No folder", "Take it out of ${folderLabel(currentFolder)}", { onMoveToFolder(null) }, icon = Icons.Filled.Close)
                }
                MenuOption("New folder", "Make a folder and put it there", { page = AppMenuPage.NEW_FOLDER }, icon = Icons.Filled.Add)
                SheetButton("Back", { page = AppMenuPage.MAIN })
            }
            AppMenuPage.NEW_FOLDER -> {
                NameField(
                    label = "New folder's name",
                    initial = "",
                    taken = { new -> folderNames.any { it.equals(Folders.cleanName(new), ignoreCase = true) } },
                    saveLabel = "Make folder and move ${app.name}",
                    onSave = onMoveToNewFolder,
                    onBack = { page = AppMenuPage.PICK_FOLDER },
                )
            }
        }
    }
}

private enum class FolderMenuPage { MAIN, RENAME, DELETE }

/** The press-and-hold menu for a folder: rename, move it in the tree, or delete it. */
@Composable
fun FolderMenu(
    name: String,
    appCount: Int,
    otherNames: List<String>,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRename: (String) -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var page by rememberSaveable { mutableStateOf(FolderMenuPage.MAIN) }
    MenuSheet(
        onDismiss = onDismiss,
        header = {
            FolderIcon(size = 44.dp)
            Spacer(Modifier.width(12.dp))
            SheetTitle(folderLabel(name))
        },
    ) {
        when (page) {
            FolderMenuPage.MAIN -> {
                MenuOption("Rename", "Its apps stay in it", { page = FolderMenuPage.RENAME }, icon = Icons.Filled.Edit)
                if (canMoveUp) MenuOption("Move up", "Higher in your folders", { onMove(-1) }, icon = Icons.Filled.KeyboardArrowUp)
                if (canMoveDown) MenuOption("Move down", "Lower in your folders", { onMove(1) }, icon = Icons.Filled.KeyboardArrowDown)
                MenuOption("Delete folder", "Its apps stay in All apps", { page = FolderMenuPage.DELETE }, icon = Icons.Filled.Delete)
                SheetButton("Cancel", onDismiss)
            }
            FolderMenuPage.RENAME -> NameField(
                label = "Folder's new name",
                initial = name,
                taken = { new -> otherNames.any { it.equals(Folders.cleanName(new), ignoreCase = true) } },
                saveLabel = "Rename",
                onSave = onRename,
                onBack = { page = FolderMenuPage.MAIN },
            )
            FolderMenuPage.DELETE -> {
                val apps = when (appCount) {
                    0 -> "It's empty."
                    1 -> "Its 1 app stays in All apps."
                    else -> "Its $appCount apps stay in All apps."
                }
                Text(
                    "Delete ${folderLabel(name)}? $apps",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                )
                MenuOption("Delete ${folderLabel(name)}", "This can't be undone", onDelete, danger = true, icon = Icons.Filled.Delete)
                SheetButton("Keep it", { page = FolderMenuPage.MAIN })
            }
        }
    }
}

/** "+ New folder" from the tree: just a name. */
@Composable
fun NewFolderSheet(existingNames: List<String>, onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    MenuSheet(
        onDismiss = onDismiss,
        header = {
            FolderIcon(size = 44.dp)
            Spacer(Modifier.width(12.dp))
            SheetTitle("New folder")
        },
    ) {
        NameField(
            label = "Folder's name",
            initial = "",
            taken = { new -> existingNames.any { it.equals(Folders.cleanName(new), ignoreCase = true) } },
            saveLabel = "Make folder",
            onSave = onCreate,
            onBack = onDismiss,
            backLabel = "Cancel",
        )
    }
}

/** A labeled text field for a folder name, with Save (off until the name is usable) and Back. */
@Composable
private fun NameField(
    label: String,
    initial: String,
    taken: (String) -> Boolean,
    saveLabel: String,
    onSave: (String) -> Unit,
    onBack: () -> Unit,
    backLabel: String = "Back",
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    var triedEmpty by rememberSaveable { mutableStateOf(false) }
    val clean = Folders.cleanName(text)
    val isTaken = clean.isNotEmpty() && !clean.equals(Folders.cleanName(initial), ignoreCase = true) && taken(clean)
    val canSave = clean.isNotEmpty() && !isTaken
    // Save stays tappable (a greyed-out button is hard to read); tapping it too early explains why.
    fun save() {
        if (canSave) onSave(clean) else triedEmpty = clean.isEmpty()
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Text(
        label,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .clearAndSetSemantics { },
    )
    val shape = RoundedCornerShape(14.dp)
    BasicTextField(
        value = text,
        onValueChange = {
            text = it
            triedEmpty = false
        },
        singleLine = true,
        textStyle = TextStyle(
            fontFamily = rowFont(),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { save() }),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .focusRequester(focus)
            .semantics { contentDescription = label },
        decorationBox = { field ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(neonOutline(), shape)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) { field() }
        },
    )
    val problem = when {
        isTaken -> "There's already a folder called $clean."
        triedEmpty -> "Type a name first."
        else -> null
    }
    if (problem != null) {
        Text(
            problem,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        )
    }
    SheetButton(saveLabel, ::save)
    SheetButton(backLabel, onBack)
}

/**
 * The words that find an app at the prompt, typed as one line ("bank, money"). Save with
 * nothing typed removes them all.
 */
@Composable
private fun WordsField(appName: String, initial: List<String>, onSave: (List<String>) -> Unit, onBack: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial.joinToString(", ")) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val label = "Words that find $appName"
    Text(
        label,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .clearAndSetSemantics { },
    )
    val shape = RoundedCornerShape(14.dp)
    BasicTextField(
        value = text,
        onValueChange = { text = it },
        singleLine = true,
        textStyle = TextStyle(
            fontFamily = rowFont(),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSave(SearchWords.parse(text)) }),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .focusRequester(focus)
            .semantics { contentDescription = label },
        decorationBox = { field ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(neonOutline(), shape)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) { field() }
        },
    )
    Text(
        "Separate words with commas or spaces. Capitals don't matter.",
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
    )
    val parsed = SearchWords.parse(text)
    SheetButton(if (parsed.isEmpty() && initial.isNotEmpty()) "Remove all words" else "Save words") { onSave(parsed) }
    SheetButton("Back", onBack)
}

@Composable
private fun MenuHeading(text: String) {
    Text(
        text,
        fontFamily = if (cleanLook()) NewtFonts.Display else NewtFonts.Mono,
        fontWeight = FontWeight.Bold,
        fontSize = if (cleanLook()) 17.sp else 15.sp,
        color = if (cleanLook()) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
        modifier = Modifier
            .padding(start = 10.dp, top = 8.dp, bottom = 2.dp)
            .semantics { heading() },
    )
}

@Composable
private fun MenuOption(
    title: String,
    explanation: String,
    onClick: () -> Unit,
    danger: Boolean = false,
    icon: ImageVector? = null,
) {
    // The clean look puts an icon by each choice, like the mock and Newt Phone.
    val shown = icon.takeIf { cleanLook() }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (shown != null) {
            Icon(
                shown,
                contentDescription = null,
                tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(16.dp))
        }
        Column {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = if (cleanLook()) 20.sp else 19.sp,
                color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Text(explanation, fontSize = if (cleanLook()) 16.sp else 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A folder's name as menus show it: `Games/` in the terminal look, `Games` in the clean one. */
@Composable
private fun folderLabel(name: String): String = if (cleanLook()) name else "$name/"

/**
 * The clean look's "Jump to": an app's shortcuts as round outlined buttons that wrap onto
 * more lines as needed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShortcutChips(shortcuts: List<AppShortcut>, appName: String, onOpen: (AppShortcut) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
        for (shortcut in shortcuts) {
            val shape = RoundedCornerShape(24.dp)
            Box(
                Modifier
                    .heightIn(min = 48.dp)
                    .clip(shape)
                    .border(neonOutline(), shape)
                    .clickable(onClickLabel = shortcut.description ?: "Open in $appName") { onOpen(shortcut) }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(shortcut.label, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

/**
 * Today's events, opened from the date or the line under it. Each event opens in the
 * calendar app. Past events say "done" in words rather than being dimmed, so they stay
 * readable.
 */
@Composable
fun AgendaSheet(
    date: String,
    events: List<DayEvent>,
    now: Long,
    time: (Long) -> String,
    onOpenEvent: (DayEvent) -> Unit,
    onOpenCalendar: () -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit,
) {
    MenuSheet(onDismiss = onDismiss, header = { SheetTitle("Today · $date") }) {
        if (events.isEmpty()) {
            Text(
                "No events today.",
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
        if (cleanLook()) {
            CleanDay(events, now, time, onOpenEvent)
        } else events.forEach { event ->
            val done = !event.allDay && event.end <= now
            val status = when {
                done -> " · done"
                AgendaText.isNow(event, now) -> " · now"
                else -> ""
            }
            val detail = buildString {
                append(
                    if (event.allDay) "All day" else "${time(event.begin)} to ${time(event.end)}",
                )
                append(status)
                if (event.location.isNotEmpty()) append(" · ${event.location}")
            }
            MenuOption(event.title, detail, onClick = { onOpenEvent(event) })
        }
        MenuOption("Open calendar", "Opens your calendar app at today", onClick = onOpenCalendar, icon = Icons.Filled.DateRange)
        MenuOption(
            "Stop showing events",
            "Hides them from the home screen. Press and hold the date to bring them back.",
            onClick = onStop,
            icon = Icons.Filled.Close,
        )
        SheetButton("Close", onDismiss)
    }
}

/**
 * The clean look's day: times down the left, the event happening now (or next) outlined,
 * and finished ones crossed out with "Done" in words, so it's never color alone.
 */
@Composable
private fun CleanDay(events: List<DayEvent>, now: Long, time: (Long) -> String, onOpenEvent: (DayEvent) -> Unit) {
    val current = events.firstOrNull { AgendaText.isNow(it, now) }
    val focus = current ?: events.firstOrNull { !it.allDay && it.begin > now }
    for (event in events) {
        val done = !event.allDay && event.end <= now
        val lit = event == focus
        val shape = RoundedCornerShape(16.dp)
        val note = listOfNotNull(
            when {
                done -> "Done"
                event == current -> "Now"
                lit -> "Next"
                else -> null
            },
            if (event.allDay) null else "until ${time(event.end)}",
            event.location.ifEmpty { null },
        ).joinToString(" · ")
        Row(
            Modifier
                .padding(vertical = 3.dp)
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clip(shape)
                .then(
                    if (lit) {
                        Modifier
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(neonOutline(), shape)
                    } else {
                        Modifier
                    },
                )
                .clickable(onClickLabel = "Open in calendar") { onOpenEvent(event) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (event.allDay) "All day" else time(event.begin),
                fontFamily = NewtFonts.Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(92.dp),
            )
            Column {
                Text(
                    event.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                )
                if (note.isNotEmpty()) {
                    Text(note, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * `help`: every command with an example. Tapping one puts it at the prompt to edit or run,
 * so nothing runs from this list by itself.
 */
@Composable
fun HelpSheet(
    commands: List<CommandHelp>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    onSwitchLook: (() -> Unit)? = null,
) {
    val clean = cleanLook()
    MenuSheet(onDismiss = onDismiss, header = { SheetTitle("Commands") }) {
        Text(
            "Type one at the prompt and press Enter. Capitals don't matter, and app names always work too.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
        commands.forEach { MenuOption(if (clean) it.example else "~$ ${it.example}", it.does, onClick = { onPick(it.example) }) }
        if (onSwitchLook != null) {
            MenuOption(
                if (clean) "Switch to the terminal look" else "Switch to the clean look",
                if (clean) "The ~$ prompt, scanlines and the newt" else "Plain cards and words, like Newt Phone",
                onClick = onSwitchLook,
            )
        }
        SheetButton("Close", onDismiss)
    }
}

/**
 * "Add to Newt Home?": shown when a browser's "Add to home screen" (or another app) asks
 * to pin a shortcut. Nothing is added until you say so.
 */
@Composable
fun PinShortcutSheet(name: String, from: String, onAdd: () -> Unit, onCancel: () -> Unit) {
    MenuSheet(onDismiss = onCancel, header = { SheetTitle("Add to Newt Home?") }) {
        Text(
            name,
            fontFamily = rowFont(),
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
        Text(
            "A shortcut from $from. It goes in All apps and search; press and hold it to add it to favorites or a folder.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
        SheetButton("Add", onAdd)
        SheetButton("Cancel", onCancel)
    }
}

/**
 * "Add a website": a name and an address, for sites where the browser's "Add to home
 * screen" doesn't work. It opens in your default browser, like a link.
 */
@Composable
fun AddWebsiteSheet(onAdd: (name: String, url: String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var tried by rememberSaveable { mutableStateOf(false) }
    val url = Websites.normalize(address)
    fun save() {
        if (url != null) onAdd(Websites.cleanName(name, url), url) else tried = true
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    MenuSheet(onDismiss = onDismiss, header = { SheetTitle("Add a website") }) {
        NeonField(
            label = "Address, like weather.gov",
            value = address,
            onValueChange = {
                address = it
                tried = false
            },
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Next,
            onAction = {},
            modifier = Modifier.focusRequester(focus),
        )
        if (tried) {
            Text(
                if (address.isBlank()) "Type the site's address first." else "That doesn't look like a web address.",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            )
        }
        NeonField(
            label = "Name (optional)",
            value = name,
            onValueChange = { name = it },
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
            onAction = ::save,
        )
        Text(
            "It opens in your browser and shows in All apps and search.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        )
        SheetButton("Add website", ::save)
        SheetButton("Cancel", onDismiss)
    }
}

/** A labeled one-line text box with the neon outline. */
@Composable
private fun NeonField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        label,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .clearAndSetSemantics { },
    )
    val shape = RoundedCornerShape(14.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(
            fontFamily = rowFont(),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
        keyboardOptions = KeyboardOptions(
            capitalization = if (keyboardType == KeyboardType.Uri) KeyboardCapitalization.None else KeyboardCapitalization.Sentences,
            autoCorrectEnabled = keyboardType != KeyboardType.Uri,
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(onDone = { onAction() }),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics { contentDescription = label },
        decorationBox = { field ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(neonOutline(), shape)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) { field() }
        },
    )
}

/**
 * `sys`: this phone's system info in neofetch style, `label  value` one per line in large
 * text, with the Newt color blocks underneath. Opened by the command or by tapping the
 * status line.
 */
@Composable
fun SysSheet(info: SystemInfo, onDismiss: () -> Unit) {
    if (cleanLook()) {
        CleanSysSheet(info, onDismiss)
        return
    }
    MenuSheet(onDismiss = onDismiss, header = { SheetTitle("${info.user}@${info.device.lowercase().replace(' ', '-')}") }) {
        val label = io.github.secnewt.home.ui.theme.LocalTerminalColors.current.prompt
        for ((name, value) in info.rows()) {
            Text(
                androidx.compose.ui.text.buildAnnotatedString {
                    withStyle(androidx.compose.ui.text.SpanStyle(color = label, fontWeight = FontWeight.Bold)) {
                        append(name.padEnd(8))
                    }
                    append(value)
                },
                fontFamily = NewtFonts.Mono,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 3.dp)
                    .semantics { contentDescription = "$name: $value" },
            )
        }
        // neofetch's color blocks, in Newt colors; for the look only.
        Row(
            Modifier
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .clearAndSetSemantics { },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val blocks = listOf(
                MaterialTheme.colorScheme.error,
                label,
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.onSurface,
            )
            for (color in blocks) {
                Box(
                    Modifier
                        .width(28.dp)
                        .heightIn(min = 14.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color),
                )
            }
        }
        SheetButton("Close", onDismiss)
    }
}

/**
 * The clean look's system info: the phone's name, then battery and uptime as big tiles,
 * storage and memory as bars, and the rest as plain rows.
 */
@Composable
private fun CleanSysSheet(info: SystemInfo, onDismiss: () -> Unit) {
    MenuSheet(onDismiss = onDismiss, header = { SheetTitle(info.device) }) {
        Text(
            "${info.android} · ${info.build}",
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )
        Row(
            Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            info.battery?.let { SysTile("Battery", it, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f)) }
            SysTile("Up for", SystemInfoText.uptime(info.uptimeMillis), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        }
        SysBar("Storage", info.storageUsed, info.storageTotal)
        SysBar("Memory", info.memoryUsed, info.memoryTotal)
        for ((name, value) in listOf("Apps" to info.apps.toString(), "Kernel" to info.kernel, "Home app" to info.launcher)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp)
                    .semantics(mergeDescendants = true) { },
            ) {
                Text(name, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(value, fontFamily = NewtFonts.Mono, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        SheetButton("Close", onDismiss)
    }
}

@Composable
private fun SysTile(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(14.dp)
            .semantics(mergeDescendants = true) { },
    ) {
        Text(label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontFamily = NewtFonts.Mono, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = color)
    }
}

/** "Storage  157G / 228G (69%)" over a bar filled to match. */
@Composable
private fun SysBar(label: String, used: Long, total: Long) {
    val fraction = if (total > 0) (used.toFloat() / total).coerceIn(0f, 1f) else 0f
    Column(
        Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(14.dp)
            .semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(
                SystemInfoText.usage(used, total),
                fontFamily = NewtFonts.Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.outlineVariant)
                .clearAndSetSemantics { },
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .heightIn(min = 12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
            )
        }
    }
}
