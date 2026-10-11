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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
 * choices. Tapping outside it, Cancel, or Back closes it.
 */
@Composable
private fun MenuSheet(
    onDismiss: () -> Unit,
    header: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
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
                    for (shortcut in shortcuts) {
                        MenuOption(
                            shortcut.label,
                            shortcut.description ?: "In ${app.name}",
                            { onOpenShortcut(shortcut) },
                        )
                    }
                    MenuHeading("Options")
                }
                if (isFavorite) {
                    MenuOption("Remove from favorites", "It stays in All apps", onToggleFavorite)
                    if (canMoveUp) MenuOption("Move up", "Higher in your favorites", { onMove(-1) })
                    if (canMoveDown) MenuOption("Move down", "Lower in your favorites", { onMove(1) })
                } else {
                    MenuOption("Add to favorites", "Puts it on your home screen", onToggleFavorite)
                }
                MenuOption(
                    "Move to folder",
                    if (currentFolder != null) "Now in $currentFolder/" else "Not in a folder yet",
                    { page = AppMenuPage.PICK_FOLDER },
                )
                MenuOption(
                    if (words.isEmpty()) "Add search words" else "Search words",
                    if (words.isEmpty()) "Your own words that find it, like bank or maps" else words.joinToString(", "),
                    { page = AppMenuPage.WORDS },
                )
                if (isHidden) {
                    MenuOption("Show in lists", "Back in All apps and its folder", onToggleHidden)
                } else {
                    MenuOption("Hide from lists", "Leaves All apps and folders. Search still finds it", onToggleHidden)
                }
                val from = app.from
                val url = app.url
                if (url != null) {
                    MenuOption("Remove website", "Takes it off Newt Home. Opens $url", onRemoveShortcut, danger = true)
                } else if (from != null) {
                    MenuOption(
                        "Remove shortcut",
                        "Takes it off Newt Home. You can add it again from $from",
                        onRemoveShortcut,
                        danger = true,
                    )
                    MenuOption("$from info", "Android's page for $from, which made this shortcut", onAppInfo)
                } else {
                    MenuOption("App info", "Android's page for this app", onAppInfo)
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
                    MenuOption("$name/", if (name == currentFolder) "It's here now" else "Move it here", { onMoveToFolder(name) })
                }
                if (currentFolder != null) {
                    MenuOption("No folder", "Take it out of $currentFolder/", { onMoveToFolder(null) })
                }
                MenuOption("New folder", "Make a folder and put it there", { page = AppMenuPage.NEW_FOLDER })
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
            SheetTitle("$name/")
        },
    ) {
        when (page) {
            FolderMenuPage.MAIN -> {
                MenuOption("Rename", "Its apps stay in it", { page = FolderMenuPage.RENAME })
                if (canMoveUp) MenuOption("Move up", "Higher in your folders", { onMove(-1) })
                if (canMoveDown) MenuOption("Move down", "Lower in your folders", { onMove(1) })
                MenuOption("Delete folder", "Its apps stay in All apps", { page = FolderMenuPage.DELETE })
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
                    "Delete $name/? $apps",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                )
                MenuOption("Delete $name/", "This can't be undone", onDelete, danger = true)
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
            fontFamily = NewtFonts.Mono,
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
            fontFamily = NewtFonts.Mono,
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
        fontFamily = NewtFonts.Mono,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier
            .padding(start = 10.dp, top = 8.dp, bottom = 2.dp)
            .semantics { heading() },
    )
}

@Composable
private fun MenuOption(title: String, explanation: String, onClick: () -> Unit, danger: Boolean = false) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            title,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        Text(explanation, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        events.forEach { event ->
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
        MenuOption("Open calendar", "Opens your calendar app at today", onClick = onOpenCalendar)
        MenuOption(
            "Stop showing events",
            "Hides them from the home screen. Press and hold the date to bring them back.",
            onClick = onStop,
        )
        SheetButton("Close", onDismiss)
    }
}

/**
 * `help`: every command with an example. Tapping one puts it at the prompt to edit or run,
 * so nothing runs from this list by itself.
 */
@Composable
fun HelpSheet(commands: List<CommandHelp>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    MenuSheet(onDismiss = onDismiss, header = { SheetTitle("Commands") }) {
        Text(
            "Type one at the prompt and press Enter. Capitals don't matter, and app names always work too.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
        commands.forEach { MenuOption("~$ ${it.example}", it.does, onClick = { onPick(it.example) }) }
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
            fontFamily = NewtFonts.Mono,
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
            fontFamily = NewtFonts.Mono,
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
