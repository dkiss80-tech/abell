package com.example

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class TabletLayoutRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    @Config(sdk = [36], qualifiers = "w1280dp-h800dp-land")
    fun `main activity locks sensorLandscape and renders full-screen two-pane layouts with 80m chained schedule flow`() {
        composeRule.waitForIdle()

        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            composeRule.activity.requestedOrientation,
        )

        // Full-screen tablet landscape uses NavigationRail + 2-pane Timetable
        composeRule.onNodeWithTag("navigation_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_navigation_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("tablet_two_pane_timetable").assertIsDisplayed()

        // Open schedule builder from tablet quick-add button
        composeRule.onNodeWithTag("tablet_quick_add_schedule_button").performClick()
        composeRule.waitForIdle()

        // Select 80-minute preset and chain-save into 2교시
        composeRule.onNodeWithTag("class_duration_80").performClick()
        composeRule.onNodeWithTag("save_and_chain_button").performClick()

        // Wait for async Room save & chained draft update to 2교시
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodes(hasText("2교시")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("save_schedule_button").performClick()
        composeRule.waitForIdle()

        // Switch to Sounds tab -> 2-pane sounds layout
        composeRule.onNodeWithTag("tab_sounds").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tablet_two_pane_sounds").assertIsDisplayed()

        // Switch to Settings tab -> 2-pane settings layout
        composeRule.onNodeWithTag("tab_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tablet_two_pane_settings").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w915dp-h411dp-land")
    fun `compact landscape device renders navigation rail and full-screen two-pane timetable`() {
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("navigation_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("tablet_two_pane_timetable").assertIsDisplayed()
        composeRule.onNodeWithTag("fab_add_schedule").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h915dp-port")
    fun `narrow split-window fallback renders compact bottom bar and list`() {
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("compact_timetable_list").assertIsDisplayed()
    }
}
