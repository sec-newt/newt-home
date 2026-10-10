package io.github.secnewt.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.checkRoboAccessibility
import io.github.secnewt.home.apps.AppSearch
import io.github.secnewt.home.apps.Connection
import io.github.secnewt.home.apps.DeviceStatus
import io.github.secnewt.home.apps.LauncherApp
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
    ).map { LauncherApp(key = it, name = it) }
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
                failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Warning,
            )
        )
    }

    @Composable
    private fun Home(
        view: HomeView = HomeView.Favorites,
        query: String = "",
        opening: LauncherApp? = null,
        favs: List<LauncherApp> = favorites,
        openFolder: String? = "Productivity",
        onOpen: (LauncherApp) -> Unit = {},
        onShowAll: () -> Unit = {},
        onToggleFolder: (String) -> Unit = {},
    ) = HomeScreen(
        clock = "13:22",
        date = "Fri, Oct 9",
        view = view,
        favorites = favs,
        folders = folders,
        openFolder = openFolder,
        allApps = apps.sortedBy { it.name },
        query = query,
        opening = opening,
        onQueryChange = {},
        onSubmit = {},
        onOpen = onOpen,
        onHold = {},
        onToggleFolder = onToggleFolder,
        onHoldFolder = {},
        onNewFolder = {},
        onClock = {},
        onDate = {},
        onShowAll = onShowAll,
        folderOf = { a -> folders.firstOrNull { a in it.apps }?.name },
        status = DeviceStatus(battery = 54, connection = Connection.WIFI),
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
    fun homeEmptyFolderOpen() = render("home_empty_folder_open", dark = false) { Home(openFolder = "AI") }

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
    fun allApps() = render("all_apps", dark = true) { Home(view = HomeView.AllApps) }

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
    fun tappingAnAppOpensIt() {
        var opened: LauncherApp? = null
        var showedAll = false
        var toggled: String? = null
        composeRule.setContent {
            NewtTheme(darkTheme = true) {
                Home(onOpen = { opened = it }, onShowAll = { showedAll = true }, onToggleFolder = { toggled = it })
            }
        }
        composeRule.onNodeWithText("Signal").assertIsDisplayed().performClick()
        assertEquals("Signal", opened?.name)
        composeRule.onNodeWithText("Proton Mail").performClick()
        assertEquals("Proton Mail", opened?.name)
        composeRule.onNodeWithText("All apps", substring = true).performClick()
        assertEquals(true, showedAll)
        composeRule.onNodeWithText("Games/").performClick()
        assertEquals("Games", toggled)
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
                    onShowAll = {},
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
