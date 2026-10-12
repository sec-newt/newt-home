package io.github.secnewt.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityViewCheckResult
import com.google.android.apps.common.testing.accessibility.framework.checks.TextContrastCheck
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.checkRoboAccessibility
import io.github.secnewt.home.apps.Agenda
import io.github.secnewt.home.apps.AppSearch
import io.github.secnewt.home.apps.AppShortcut
import io.github.secnewt.home.apps.Commands
import io.github.secnewt.home.apps.DayEvent
import io.github.secnewt.home.apps.Connection
import io.github.secnewt.home.apps.DeviceStatus
import io.github.secnewt.home.apps.LauncherApp
import io.github.secnewt.home.apps.SystemInfo
import io.github.secnewt.home.apps.ProfileKind
import io.github.secnewt.home.apps.ProfileView
import io.github.secnewt.home.ui.theme.NewtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Screenshots (light, dark, 200% text) and accessibility checks for the home screen and its menus. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
@OptIn(ExperimentalRoborazziApi::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val apps = listOf(
        "Phone", "Messages", "Signal", "Camera", "Vanadium", "Settings", "Sudoku", "Magnifier", "Proton Mail",
    ).map { LauncherApp(key = it, name = it) } + LauncherApp(key = "shortcut:vanadium/weather", name = "Weather.gov", from = "Vanadium") +
        LauncherApp(key = "web:1", name = "Bank", url = "https://www.bank.example/login")
    private val favorites = apps.take(5)
    private fun app(name: String) = apps.first { it.name == name }

    private val folders = listOf(
        FolderView("AI", emptyList()),
        FolderView("Blind tools", listOf(app("Magnifier"))),
        FolderView("Games", listOf(app("Sudoku"))),
        FolderView("Productivity", listOf(app("Proton Mail"), app("Settings"))),
    )

    private fun render(
        name: String,
        dark: Boolean,
        fontScale: Float = 1f,
        before: () -> Unit = {},
        content: @Composable () -> Unit,
    ) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                NewtTheme(darkTheme = dark) { content() }
            }
        }
        before()
        composeRule.onRoot().captureRoboImage("build/screenshots/$name.png")
        composeRule.onRoot().checkRoboAccessibility(
            RoborazziATFAccessibilityCheckOptions(
                checker = RoborazziATFAccessibilityChecker(suppressions = clippedAtScrollEdge),
                failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Warning,
            )
        )
    }

    /**
     * A row cut off by the bottom of a scrolling list shows only a sliver of its text, and the
     * contrast check measures that sliver. ATF marks these itself ("may be only partially
     * visible within a scrollable container"); every fully shown row is still checked.
     */
    private val clippedAtScrollEdge = object : TypeSafeMatcher<AccessibilityViewCheckResult>() {
        override fun matchesSafely(result: AccessibilityViewCheckResult): Boolean =
            result.sourceCheckClass == TextContrastCheck::class.java &&
                result.message.toString().contains("partially visible within a scrollable container")

        override fun describeTo(description: Description) {
            description.appendText("text contrast measured on a row clipped at a scroll edge")
        }
    }

    private fun scrollAllAppsTo(text: String) {
        composeRule.onNodeWithTag(ALL_APPS_LIST).performScrollToNode(hasText(text))
    }

    @Composable
    private fun Home(
        view: HomeView = HomeView.Favorites,
        query: String = "",
        opening: LauncherApp? = null,
        favs: List<LauncherApp> = favorites,
        openFolder: String? = "Productivity",
        onOpen: (LauncherApp) -> Unit = {},
        screen: Screen = Screen.HOME,
        onScreen: (Screen) -> Unit = {},
        onToggleFolder: (String) -> Unit = {},
        agenda: Agenda = Agenda.Off,
        onAgendaChoice: (Boolean) -> Unit = {},
        onSubmit: () -> Unit = {},
        hidden: List<LauncherApp> = emptyList(),
        profiles: List<ProfileView> = emptyList(),
        onToggleProfileLock: (ProfileKind) -> Unit = {},
        onStatusTap: () -> Unit = {},
        onVoice: () -> Unit = {},
    ) = HomeScreen(
        clock = "13:22",
        date = "Fri, Oct 9",
        view = view,
        favorites = favs,
        folders = folders,
        openFolder = openFolder,
        allApps = apps.filter { it !in hidden }.sortedBy { it.name },
        query = query,
        opening = opening,
        onQueryChange = {},
        onSubmit = onSubmit,
        hiddenApps = hidden,
        profiles = profiles,
        onToggleProfileLock = onToggleProfileLock,
        onStatusTap = onStatusTap,
        onVoice = onVoice,
        isHidden = { it in hidden },
        onOpen = onOpen,
        onHold = {},
        onToggleFolder = onToggleFolder,
        onHoldFolder = {},
        onNewFolder = {},
        onClock = {},
        onDate = {},
        screen = screen,
        onScreen = onScreen,
        folderOf = { a -> folders.firstOrNull { a in it.apps }?.name },
        status = DeviceStatus(battery = 54, connection = Connection.WIFI),
        agenda = agenda,
        now = 12 * hour,
        time = time,
        onAgendaChoice = onAgendaChoice,
    )

    private val hour = 60L * 60 * 1000
    private val time: (Long) -> String = { "${it / hour}:00" }
    private val events = listOf(
        DayEvent(1, "Trash day", 0, 24 * hour, allDay = true),
        DayEvent(2, "Standup", 9 * hour, 10 * hour),
        DayEvent(3, "Dentist", 15 * hour, 16 * hour, location = "Main St clinic"),
        DayEvent(4, "Dinner with Sam", 18 * hour, 19 * hour),
    )

    @Composable
    private fun DaySheet() = AgendaSheet(
        date = "Fri, Oct 9",
        events = events,
        now = 12 * hour,
        time = time,
        onOpenEvent = {},
        onOpenCalendar = {},
        onStop = {},
        onDismiss = {},
    )

    private fun results(query: String) = HomeView.Results(
        query,
        AppSearch.search(apps, query, name = { it.name }, folder = { a -> folders.firstOrNull { a in it.apps }?.name }),
    )

    @Composable
    private fun AppMenu(app: LauncherApp, isFavorite: Boolean, folder: String?) = HoldMenu(
        app = app,
        isFavorite = isFavorite,
        canMoveUp = isFavorite,
        canMoveDown = isFavorite,
        folderNames = folders.map { it.name },
        currentFolder = folder,
        onToggleFavorite = {},
        onMove = {},
        onMoveToFolder = {},
        onMoveToNewFolder = {},
        onAppInfo = {},
        onDismiss = {},
    )

    @Test
    fun homeDark() = render("home_dark", dark = true) { Home() }

    @Test
    fun homeLight() = render("home_light", dark = false) { Home() }

    @Test
    fun homeLargeText() = render("home_200_percent", dark = true, fontScale = 2f) { Home() }

    @Test
    fun foldersDark() = render("folders_dark", dark = true) { Home(screen = Screen.FOLDERS) }

    @Test
    fun foldersLargeText() = render("folders_200_percent", dark = false, fontScale = 2f) { Home(screen = Screen.FOLDERS) }

    @Test
    fun foldersEmptyFolderOpen() = render("folders_empty_folder_open", dark = false) {
        Home(screen = Screen.FOLDERS, openFolder = "AI")
    }

    @Test
    fun homeNoFavoritesYet() = render("home_no_favorites", dark = true) { Home(favs = emptyList(), openFolder = null) }

    @Test
    fun searchOpeningTheOnlyMatch() = render("search_only_match", dark = true) {
        Home(view = results("sign"), query = "sign", opening = app("Signal"))
    }

    @Test
    fun searchSeveralMatches() = render("search_several", dark = false) { Home(view = results("s"), query = "s") }

    @Test
    fun searchByFolderName() = render("search_by_folder", dark = true) { Home(view = results("blind"), query = "blind") }

    @Test
    fun searchNoMatch() = render("search_no_match", dark = true) { Home(view = results("zz"), query = "zz") }

    @Test
    fun allApps() = render("all_apps", dark = true) { Home(screen = Screen.APPS) }

    @Test
    fun allAppsLargeText() = render("all_apps_200_percent", dark = false, fontScale = 2f) { Home(screen = Screen.APPS) }

    @Test
    fun holdMenuOnAFavorite() = render("hold_menu_favorite", dark = true) {
        WithHoldMenu(menuOpen = true, menu = { AppMenu(app("Signal"), isFavorite = true, folder = null) }) { Home() }
    }

    @Test
    fun holdMenuLargeText() = render("hold_menu_200_percent", dark = false, fontScale = 2f) {
        WithHoldMenu(menuOpen = true, menu = { AppMenu(app("Proton Mail"), isFavorite = false, folder = "Productivity") }) { Home() }
    }

    @Test
    fun holdMenuPickingAFolder() = render(
        "hold_menu_move_to_folder",
        dark = true,
        before = { composeRule.onNodeWithText("Move to folder").performClick() },
    ) {
        WithHoldMenu(menuOpen = true, menu = { AppMenu(app("Magnifier"), isFavorite = false, folder = "Blind tools") }) { Home() }
    }

    @Test
    fun folderMenu() = render("folder_menu", dark = true) {
        WithHoldMenu(
            menuOpen = true,
            menu = {
                FolderMenu(
                    name = "Games",
                    appCount = 1,
                    otherNames = listOf("AI", "Blind tools", "Productivity"),
                    canMoveUp = true,
                    canMoveDown = true,
                    onRename = {},
                    onMove = {},
                    onDelete = {},
                    onDismiss = {},
                )
            },
        ) { Home() }
    }

    @Test
    fun folderMenuDeleting() = render(
        "folder_menu_delete",
        dark = false,
        before = { composeRule.onNodeWithText("Delete folder").performClick() },
    ) {
        WithHoldMenu(
            menuOpen = true,
            menu = {
                FolderMenu(
                    name = "Productivity",
                    appCount = 2,
                    otherNames = listOf("AI", "Blind tools", "Games"),
                    canMoveUp = true,
                    canMoveDown = false,
                    onRename = {},
                    onMove = {},
                    onDelete = {},
                    onDismiss = {},
                )
            },
        ) { Home() }
    }

    @Test
    fun newFolderSheet() = render("new_folder", dark = true) {
        WithHoldMenu(
            menuOpen = true,
            menu = { NewFolderSheet(existingNames = folders.map { it.name }, onCreate = {}, onDismiss = {}) },
        ) { Home() }
    }

    @Test
    fun eventsNextLine() = render("events_next_dark", dark = true) { Home(agenda = Agenda.Today(events)) }

    @Test
    fun eventsNextLineLargeText() = render("events_next_200_percent", dark = false, fontScale = 2f) {
        Home(agenda = Agenda.Today(events))
    }

    @Test
    fun eventsOffer() = render("events_offer_light", dark = false) { Home(agenda = Agenda.Offer) }

    @Test
    fun eventsOfferLargeText() = render("events_offer_200_percent", dark = true, fontScale = 2f) { Home(agenda = Agenda.Offer) }

    @Test
    fun eventsNeedPermission() = render("events_need_permission", dark = true) { Home(agenda = Agenda.NeedsPermission) }

    @Test
    fun eventsDaySheet() = render("events_day_list_dark", dark = true) {
        WithHoldMenu(menuOpen = true, menu = { DaySheet() }) { Home(agenda = Agenda.Today(events)) }
    }

    @Test
    fun eventsDaySheetLargeText() = render("events_day_list_200_percent", dark = false, fontScale = 2f) {
        WithHoldMenu(menuOpen = true, menu = { DaySheet() }) { Home(agenda = Agenda.Today(events)) }
    }

    @Test
    fun eventsOfferAnswers() {
        var choice: Boolean? = null
        composeRule.setContent {
            NewtTheme(darkTheme = true) { Home(agenda = Agenda.Offer, onAgendaChoice = { choice = it }) }
        }
        composeRule.onNodeWithContentDescription("Yes, show today's events").performClick()
        assertEquals(true, choice)
        composeRule.onNodeWithContentDescription("No thanks").performClick()
        assertEquals(false, choice)
    }

    @Test
    fun eventsLineShowsTheNextEvent() {
        composeRule.setContent { NewtTheme(darkTheme = true) { Home(agenda = Agenda.Today(events)) } }
        composeRule.onNodeWithText("next: 15:00 Dentist · 2 more today").assertIsDisplayed()
    }

    private fun commandResults(query: String, text: String, ready: Boolean) =
        HomeView.Results(query, results(query).results, CommandView(text, ready))

    @Test
    fun commandReady() = render("command_ready_dark", dark = true) {
        Home(view = commandResults("timer 10", "start a 10 min timer", ready = true), query = "timer 10")
    }

    @Test
    fun commandReadyLargeText() = render("command_ready_200_percent", dark = false, fontScale = 2f) {
        Home(view = commandResults("alarm 7:30", "set an alarm for 7:30 AM", ready = true), query = "alarm 7:30")
    }

    @Test
    fun commandIncomplete() = render("command_incomplete_light", dark = false) {
        Home(view = commandResults("timer", "how long? like timer 10, timer 90s or timer 1h 30m", ready = false), query = "timer")
    }

    @Test
    fun helpSheet() = render("help_dark", dark = true) {
        WithHoldMenu(menuOpen = true, menu = { HelpSheet(Commands.HELP, onPick = {}, onDismiss = {}) }) { Home() }
    }

    @Test
    fun helpSheetLargeText() = render("help_200_percent", dark = false, fontScale = 2f) {
        WithHoldMenu(menuOpen = true, menu = { HelpSheet(Commands.HELP, onPick = {}, onDismiss = {}) }) { Home() }
    }

    @Test
    fun tappingAReadyCommandRunsIt() {
        var ran = 0
        composeRule.setContent {
            NewtTheme(darkTheme = true) {
                Home(
                    view = commandResults("flashlight", "switch the flashlight on or off", ready = true),
                    query = "flashlight",
                    onSubmit = { ran++ },
                )
            }
        }
        composeRule.onNodeWithText("switch the flashlight on or off · Enter runs it").performClick()
        assertEquals(1, ran)
    }

    @Test
    fun holdMenuWithWordsAndHidden() = render("hold_menu_words_hidden", dark = true) {
        WithHoldMenu(
            menuOpen = true,
            menu = {
                HoldMenu(
                    app = app("Proton Mail"),
                    isFavorite = false,
                    canMoveUp = false,
                    canMoveDown = false,
                    folderNames = folders.map { it.name },
                    currentFolder = "Productivity",
                    onToggleFavorite = {},
                    onMove = {},
                    onMoveToFolder = {},
                    onMoveToNewFolder = {},
                    onAppInfo = {},
                    onDismiss = {},
                    isHidden = true,
                    words = listOf("email", "work"),
                )
            },
        ) { Home() }
    }

    @Test
    fun searchWordsPage() = render(
        "search_words_200_percent",
        dark = false,
        fontScale = 2f,
        before = { composeRule.onNodeWithText("Add search words").performClick() },
    ) {
        WithHoldMenu(menuOpen = true, menu = { AppMenu(app("Signal"), isFavorite = true, folder = null) }) { Home() }
    }

    @Test
    fun searchByYourWord() = render("search_your_word", dark = true) {
        val view = HomeView.Results(
            "chat",
            AppSearch.search(apps, "chat", name = { it.name }, words = { if (it.name == "Signal") listOf("chat") else emptyList() }),
        )
        Home(view = view, query = "chat")
    }

    @Test
    fun allAppsWithHiddenApps() = render(
        "all_apps_hidden_open",
        dark = true,
        before = {
            scrollAllAppsTo("▸ 2 hidden apps")
            composeRule.onNodeWithText("▸ 2 hidden apps").performClick()
        },
    ) {
        Home(screen = Screen.APPS, hidden = listOf(app("Sudoku"), app("Settings")))
    }

    @Test
    fun letterGrid() = render(
        "letter_grid_dark",
        dark = true,
        before = { composeRule.onNodeWithContentDescription("Letter grid").performClick() },
    ) { Home(screen = Screen.APPS) }

    @Test
    fun letterGridLargeText() = render(
        "letter_grid_200_percent",
        dark = false,
        fontScale = 2f,
        before = { composeRule.onNodeWithContentDescription("Letter grid").performClick() },
    ) { Home(screen = Screen.APPS) }

    @Test
    fun oneLetterFromTheGrid() = render(
        "letter_grid_picked_m",
        dark = true,
        before = {
            composeRule.onNodeWithContentDescription("Letter grid").performClick()
            composeRule.onNodeWithContentDescription("M, 2 apps").performClick()
        },
    ) { Home(screen = Screen.APPS) }

    @Test
    fun pickingALetterListsOnlyItsApps() {
        composeRule.setContent { NewtTheme(darkTheme = true) { Home(screen = Screen.APPS) } }
        composeRule.onNodeWithContentDescription("Letter grid").performClick()
        composeRule.onNodeWithContentDescription("S, 3 apps").performClick()
        composeRule.onNodeWithText("Signal").assertIsDisplayed()
        composeRule.onNodeWithText("Sudoku").assertIsDisplayed()
        composeRule.onNodeWithText("Camera").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Show all apps").performClick()
        composeRule.onNodeWithText("Camera").assertIsDisplayed()
    }

    @Test
    fun pinShortcutSheet() = render("pin_shortcut_dark", dark = true) {
        WithHoldMenu(
            menuOpen = true,
            menu = { PinShortcutSheet(name = "Weather.gov", from = "Vanadium", onAdd = {}, onCancel = {}) },
        ) { Home() }
    }

    @Test
    fun pinShortcutSheetLargeText() = render("pin_shortcut_200_percent", dark = false, fontScale = 2f) {
        WithHoldMenu(
            menuOpen = true,
            menu = { PinShortcutSheet(name = "Weather.gov", from = "Vanadium", onAdd = {}, onCancel = {}) },
        ) { Home() }
    }

    @Test
    fun holdMenuOnAShortcut() = render("hold_menu_shortcut", dark = true) {
        WithHoldMenu(menuOpen = true, menu = { AppMenu(app("Weather.gov"), isFavorite = false, folder = null) }) { Home() }
    }

    @Test
    fun shortcutsSayWhereTheyCameFrom() {
        composeRule.setContent { NewtTheme(darkTheme = true) { Home(screen = Screen.APPS) } }
        scrollAllAppsTo("Vanadium shortcut")
        composeRule.onNodeWithText("Vanadium shortcut").assertIsDisplayed()
    }

    @Test
    fun addWebsiteSheet() = render("add_website_dark", dark = true) {
        WithHoldMenu(menuOpen = true, menu = { AddWebsiteSheet(onAdd = { _, _ -> }, onDismiss = {}) }) { Home(screen = Screen.APPS) }
    }

    @Test
    fun addWebsiteSheetLargeText() = render("add_website_200_percent", dark = false, fontScale = 2f) {
        WithHoldMenu(menuOpen = true, menu = { AddWebsiteSheet(onAdd = { _, _ -> }, onDismiss = {}) }) { Home(screen = Screen.APPS) }
    }

    @Test
    fun holdMenuOnAWebsite() = render("hold_menu_website", dark = true) {
        WithHoldMenu(menuOpen = true, menu = { AppMenu(app("Bank"), isFavorite = false, folder = null) }) { Home() }
    }

    @Test
    fun addingAWebsiteChecksTheAddress() {
        var added: Pair<String, String>? = null
        composeRule.setContent {
            NewtTheme(darkTheme = true) { AddWebsiteSheet(onAdd = { n, u -> added = n to u }, onDismiss = {}) }
        }
        composeRule.onNodeWithText("Add website").performClick()
        composeRule.onNodeWithText("Type the site's address first.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Address, like weather.gov").performTextInput("not a site")
        composeRule.onNodeWithText("Add website").performClick()
        composeRule.onNodeWithText("That doesn't look like a web address.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Address, like weather.gov").performTextClearance()
        composeRule.onNodeWithContentDescription("Address, like weather.gov").performTextInput("www.weather.gov")
        composeRule.onNodeWithText("Add website").performClick()
        assertEquals("weather.gov" to "https://www.weather.gov", added)
    }

    @Test
    fun websitesShowTheirAddress() {
        composeRule.setContent { NewtTheme(darkTheme = true) { Home(screen = Screen.APPS) } }
        composeRule.onNodeWithText("website · bank.example").assertIsDisplayed()
    }

    private val signalShortcuts = listOf(
        AppShortcut("org.signal", "new", "New message", null),
        AppShortcut("org.signal", "chat-sam", "Sam", "Open your chat with Sam"),
    )

    @Composable
    private fun SignalMenu(onOpenShortcut: (AppShortcut) -> Unit = {}) = HoldMenu(
        app = app("Signal"),
        isFavorite = true,
        canMoveUp = true,
        canMoveDown = true,
        folderNames = folders.map { it.name },
        currentFolder = null,
        onToggleFavorite = {},
        onMove = {},
        onMoveToFolder = {},
        onMoveToNewFolder = {},
        onAppInfo = {},
        onDismiss = {},
        shortcuts = signalShortcuts,
        onOpenShortcut = onOpenShortcut,
    )

    @Test
    fun holdMenuWithAppShortcuts() = render("hold_menu_app_shortcuts", dark = true) {
        WithHoldMenu(menuOpen = true, menu = { SignalMenu() }) { Home() }
    }

    @Test
    fun holdMenuWithAppShortcutsLargeText() = render("hold_menu_app_shortcuts_200_percent", dark = false, fontScale = 2f) {
        WithHoldMenu(menuOpen = true, menu = { SignalMenu() }) { Home() }
    }

    @Test
    fun tappingAnAppShortcutOpensIt() {
        var opened: AppShortcut? = null
        composeRule.setContent { NewtTheme(darkTheme = true) { SignalMenu { opened = it } } }
        composeRule.onNodeWithText("Sam").performClick()
        assertEquals("chat-sam", opened?.id)
    }

    @Test
    fun missedCallsUnderPhone() = render("missed_calls_dark", dark = true) {
        Home(favs = favorites.map { if (it.name == "Phone") it.copy(alert = "2 missed") else it })
    }

    @Test
    fun missedCallsUnderPhoneLargeText() = render("missed_calls_200_percent", dark = false, fontScale = 2f) {
        Home(favs = favorites.map { if (it.name == "Phone") it.copy(alert = "2 missed") else it })
    }

    private val workAndPrivate = listOf(
        ProfileView(
            ProfileKind.WORK,
            locked = false,
            apps = listOf(LauncherApp("org.signal/.Main#10", "Signal", profile = ProfileKind.WORK)),
        ),
        ProfileView(ProfileKind.PRIVATE, locked = true, apps = emptyList()),
    )

    @Test
    fun allAppsWithProfiles() = render(
        "all_apps_profiles",
        dark = true,
        before = { scrollAllAppsTo("Unlock") },
    ) { Home(screen = Screen.APPS, profiles = workAndPrivate) }

    @Test
    fun unlockingAPrivateSpaceAsks() {
        var toggled: ProfileKind? = null
        composeRule.setContent {
            NewtTheme(darkTheme = true) { Home(screen = Screen.APPS, profiles = workAndPrivate, onToggleProfileLock = { toggled = it }) }
        }
        scrollAllAppsTo("Unlock")
        composeRule.onNodeWithText("Unlock").performClick()
        assertEquals(ProfileKind.PRIVATE, toggled)
    }

    private val sysInfo = SystemInfo(
        user = "newt",
        device = "Pixel 9",
        android = "Android 16",
        build = "BP2A.250805.005",
        kernel = "6.1.124",
        uptimeMillis = ((20 * 24 + 11) * 60) * 60_000L,
        apps = 142,
        storageUsed = 157_000_000_000,
        storageTotal = 228_000_000_000,
        memoryUsed = 9_000_000_000,
        memoryTotal = 12_000_000_000,
        battery = "54%",
        launcher = "Newt Home 0.1.42",
    )

    @Test
    fun sysSheet() = render("sys_dark", dark = true) {
        WithHoldMenu(menuOpen = true, menu = { SysSheet(sysInfo, onDismiss = {}) }) { Home() }
    }

    @Test
    fun sysSheetLargeText() = render("sys_200_percent", dark = false, fontScale = 2f) {
        WithHoldMenu(menuOpen = true, menu = { SysSheet(sysInfo, onDismiss = {}) }) { Home() }
    }

    @Test
    fun tappingTheStatusLineOpensSystemInfo() {
        var tapped = false
        composeRule.setContent { NewtTheme(darkTheme = true) { Home(onStatusTap = { tapped = true }) } }
        composeRule.onNodeWithContentDescription("Battery 54 percent, on Wi-Fi").performClick()
        assertEquals(true, tapped)
    }

    @Test
    fun theMicAsksForVoiceInput() {
        var asked = false
        composeRule.setContent { NewtTheme(darkTheme = true) { Home(onVoice = { asked = true }) } }
        composeRule.onNodeWithContentDescription("Speak instead of typing").performClick()
        assertEquals(true, asked)
    }

    @Test
    fun swipingDownClosesAllApps() {
        var went: Screen? = null
        composeRule.setContent { NewtTheme(darkTheme = true) { Home(screen = Screen.APPS, onScreen = { went = it }) } }
        composeRule.onNodeWithTag(ALL_APPS_LIST).performTouchInput { swipeDown() }
        assertEquals(Screen.HOME, went)
    }

    @Test
    fun swipingDownClosesASheet() {
        var closed = false
        composeRule.setContent {
            NewtTheme(darkTheme = true) {
                WithHoldMenu(menuOpen = true, menu = { HelpSheet(Commands.HELP, onPick = {}, onDismiss = { closed = true }) }) { Home() }
            }
        }
        // A real-sized drag: the title alone is too short to count as one.
        composeRule.onNodeWithText("Commands").performTouchInput { swipeDown(startY = centerY, endY = centerY + 1200f, durationMillis = 400) }
        assertEquals(true, closed)
    }

    @Test
    fun tappingAnAppOpensIt() {
        var opened: LauncherApp? = null
        var went: Screen? = null
        composeRule.setContent {
            NewtTheme(darkTheme = true) { Home(onOpen = { opened = it }, onScreen = { went = it }) }
        }
        composeRule.onNodeWithText("Signal").assertIsDisplayed().performClick()
        assertEquals("Signal", opened?.name)
        composeRule.onNodeWithContentDescription("All apps").performClick()
        assertEquals(Screen.APPS, went)
        composeRule.onNodeWithContentDescription("Folders").performClick()
        assertEquals(Screen.FOLDERS, went)
    }

    @Test
    fun foldersOpenAndCloseInPlace() {
        var opened: LauncherApp? = null
        var toggled: String? = null
        var went: Screen? = null
        composeRule.setContent {
            NewtTheme(darkTheme = true) {
                Home(
                    screen = Screen.FOLDERS,
                    onOpen = { opened = it },
                    onToggleFolder = { toggled = it },
                    onScreen = { went = it },
                )
            }
        }
        composeRule.onNodeWithText("Proton Mail").performClick()
        assertEquals("Proton Mail", opened?.name)
        composeRule.onNodeWithText("Games/").performClick()
        assertEquals("Games", toggled)
        composeRule.onNodeWithContentDescription("Home").performClick()
        assertEquals(Screen.HOME, went)
    }

    @Test
    fun allAppsListsEveryApp() {
        var went: Screen? = null
        composeRule.setContent {
            NewtTheme(darkTheme = true) { Home(screen = Screen.APPS, onScreen = { went = it }) }
        }
        composeRule.onNodeWithText("Camera").assertIsDisplayed()
        composeRule.onNodeWithText("Magnifier").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Home").performClick()
        assertEquals(Screen.HOME, went)
    }

    @Test
    fun bootTypeOutEndsWithTheClockShowing() {
        composeRule.setContent {
            NewtTheme(darkTheme = true) {
                HomeScreen(
                    clock = "13:22",
                    date = "Fri, Oct 9",
                    view = HomeView.Favorites,
                    favorites = favorites,
                    folders = folders,
                    openFolder = null,
                    allApps = apps,
                    query = "",
                    opening = null,
                    onQueryChange = {},
                    onSubmit = {},
                    onOpen = {},
                    onHold = {},
                    onToggleFolder = {},
                    onHoldFolder = {},
                    onNewFolder = {},
                    onClock = {},
                    onDate = {},
                    bootKey = 1,
                    animateBoot = true,
                )
            }
        }
        // The test clock runs the type-out to the end before checking.
        composeRule.onNodeWithText("13:22").assertIsDisplayed()
        composeRule.onNodeWithText("Signal").assertIsDisplayed()
    }

    @Test
    fun newFolderNeedsAName() {
        var made: String? = null
        composeRule.setContent {
            NewtTheme(darkTheme = true) {
                NewFolderSheet(existingNames = listOf("Games"), onCreate = { made = it }, onDismiss = {})
            }
        }
        composeRule.onNodeWithText("Make folder").performClick()
        composeRule.onNodeWithText("Type a name first.").assertIsDisplayed()
        assertEquals(null, made)
        composeRule.onNodeWithContentDescription("Folder's name").performTextInput("  games ")
        composeRule.onNodeWithText("There's already a folder called games.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Folder's name").performTextClearance()
        composeRule.onNodeWithContentDescription("Folder's name").performTextInput("  Music  and podcasts ")
        composeRule.onNodeWithText("Make folder").performClick()
        assertEquals("Music and podcasts", made)
    }
}
