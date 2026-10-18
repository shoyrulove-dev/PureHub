package com.purehub.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.purehub.app.feature.catalog.MiniAppId
import com.purehub.app.ui.AppLanguage
import com.purehub.app.ui.saveAppLanguage
import com.purehub.app.ui.screens.compactToolTitle
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PureHubNavigationTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun launchWithEnglishSelected() {
        ApplicationProvider.getApplicationContext<android.content.Context>().saveAppLanguage(AppLanguage.English)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.waitForIdle()
    }

    @After
    fun closeActivity() {
        scenario.close()
    }

    @Test
    fun primaryNavigationMatchesFriendlyFourSectionLayout() {
        composeRule.onNodeWithContentDescription("Home", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tools", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Community", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Settings", useUnmergedTree = true).assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Tools", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("All tools").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Community", useUnmergedTree = true).performClick()
        composeRule.onNodeWithContentDescription("Settings", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("App information").assertExists()
        composeRule.onNodeWithContentDescription("Home", useUnmergedTree = true).performClick()
    }

    @Test
    fun everyMiniAppCanBeFoundInTheCatalog() {
        composeRule.onNodeWithContentDescription("Tools", useUnmergedTree = true).performClick()
        val search = composeRule.onNode(hasSetTextAction())

        MiniAppId.entries.forEach { tool ->
            search.performTextInput(tool.title)
            check(
                composeRule.onAllNodes(hasText(compactToolTitle(tool, AppLanguage.English)) and hasClickAction() and !hasSetTextAction())
                    .fetchSemanticsNodes().isNotEmpty(),
            ) { "Missing ${tool.title} in the catalog" }
            search.performTextClearance()
        }
    }
}
