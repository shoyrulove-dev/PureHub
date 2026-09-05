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
        composeRule.onNodeWithContentDescription("home", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("all_tools", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("community", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("settings", useUnmergedTree = true).assertIsDisplayed()

        composeRule.onNodeWithContentDescription("all_tools", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("All tools").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("community", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("PureHub belongs to everyone").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("settings", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Choose which tools appear in your catalog. Your preference stays on this device.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("home", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("PureHub").assertIsDisplayed()
    }

    @Test
    fun everyMiniAppCanOpenAndReturnWithoutCrashing() {
        composeRule.onNodeWithContentDescription("all_tools", useUnmergedTree = true).performClick()
        val search = composeRule.onNode(hasSetTextAction())

        MiniAppId.entries.forEach { tool ->
            search.performTextInput(tool.title)
            composeRule.onNode(hasText(tool.title) and hasClickAction() and !hasSetTextAction()).performClick()
            composeRule.onNodeWithContentDescription("Back", useUnmergedTree = true).assertIsDisplayed().performClick()
            search.performTextClearance()
        }
    }
}
