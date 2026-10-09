package io.github.secnewt.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.checkRoboAccessibility
import io.github.secnewt.home.apps.AppSearch
import io.github.secnewt.home.apps.LauncherApp
import io.github.secnewt.home.ui.theme.NewtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Screenshots (light, dark, 200% text) and accessibility checks for the home screen. */
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

    private fun render(name: String, dark: Boolean, fontScale: Float = 1f, content: @Composable () -> Unit) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                NewtTheme(darkTheme = dark) { content() }
            }
        }
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
        onOpen: (LauncherApp) -> Unit = {},
        onShowAll: () -> Unit = {},
    ) = HomeScreen(
        clock = "13:22",
        date = "Fri, Oct 9",
        view = view,
        favorites = favs,
        allApps = apps.sortedBy { it.name },
        query = query,
        opening = opening,
        onQueryChange = {},
        onSubmit = {},
        onOpen = onOpen,
        onHold = {},
        onClock = {},
        onDate = {},
        onShowAll = onShowAll,
    )

    private fun results(query: String) = HomeView.Results(query, AppSearch.search(apps, query, name = { it.name }))

    @Test
    fun homeDark() = render("home_dark", dark = true) { Home() }

    @Test
    fun homeLight() = render("home_light", dark = false) { Home() }

    @Test
    fun homeLargeText() = render("home_200_percent", dark = true, fontScale = 2f) { Home() }

    @Test
    fun homeNoFavoritesYet() = render("home_no_favorites", dark = true) { Home(favs = emptyList()) }

    @Test
    fun searchOpeningTheOnlyMatch() = render("search_only_match", dark = true) {
        val signal = apps.first { it.name == "Signal" }
        Home(view = results("sign"), query = "sign", opening = signal)
    }

    @Test
    fun searchSeveralMatches() = render("search_several", dark = false) { Home(view = results("s"), query = "s") }

    @Test
    fun searchNoMatch() = render("search_no_match", dark = true) { Home(view = results("zz"), query = "zz") }

    @Test
    fun allApps() = render("all_apps", dark = true) { Home(view = HomeView.AllApps) }

    @Test
    fun holdMenuOnAFavorite() = render("hold_menu_favorite", dark = true) {
        WithHoldMenu(
            menu = {
                HoldMenu(
                    app = favorites[2],
                    isFavorite = true,
                    canMoveUp = true,
                    canMoveDown = true,
                    onToggleFavorite = {},
                    onMove = {},
                    onAppInfo = {},
                    onDismiss = {},
                )
            },
        ) { Home() }
    }

    @Test
    fun holdMenuLargeText() = render("hold_menu_200_percent", dark = false, fontScale = 2f) {
        WithHoldMenu(
            menu = {
                HoldMenu(
                    app = apps.last(),
                    isFavorite = false,
                    canMoveUp = false,
                    canMoveDown = false,
                    onToggleFavorite = {},
                    onMove = {},
                    onAppInfo = {},
                    onDismiss = {},
                )
            },
        ) { Home() }
    }

    @Test
    fun tappingAnAppOpensIt() {
        var opened: LauncherApp? = null
        var showedAll = false
        composeRule.setContent {
            NewtTheme(darkTheme = true) { Home(onOpen = { opened = it }, onShowAll = { showedAll = true }) }
        }
        composeRule.onNodeWithText("Signal").assertIsDisplayed().performClick()
        assertEquals("Signal", opened?.name)
        composeRule.onNodeWithText("All apps", substring = true).performClick()
        assertEquals(true, showedAll)
    }
}
