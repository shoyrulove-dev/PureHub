package com.purehub.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class PureHubNavigationTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeAndBottomNavRender() {
        rule.onNodeWithText("Zen & Time").assertIsDisplayed()
        rule.onNodeWithText("Tools").assertIsDisplayed()
        rule.onNodeWithText("Vision").assertIsDisplayed()
        rule.onNodeWithText("System").assertIsDisplayed()
        rule.onNodeWithText("Finance").assertIsDisplayed()
    }

    @Test
    fun bottomNavNavigatesAcrossFiveTabs() {
        rule.onNodeWithText("Tools").performClick()
        rule.onNodeWithText("Measure & Tools").assertIsDisplayed()

        rule.onNodeWithText("Vision").performClick()
        rule.onNodeWithText("Choose Tool").assertIsDisplayed()

        rule.onNodeWithText("System").performClick()
        rule.onNodeWithText("System & Security").assertIsDisplayed()

        rule.onNodeWithText("Finance").performClick()
        rule.onNodeWithText("Finance & Fun").assertIsDisplayed()

        rule.onNodeWithText("Zen & Time").performClick()
        rule.onNodeWithText("Zen & Time").assertIsDisplayed()
    }
}
