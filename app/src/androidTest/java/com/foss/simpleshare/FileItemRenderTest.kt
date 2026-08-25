package com.foss.simpleshare

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.foss.simpleshare.data.FileModel
import com.foss.simpleshare.feature.browser.components.FileListItem
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Render-level Compose test for the file list item: name, size and child-count
 * formatting must surface through the real resource-backed strings.
 */
@RunWith(AndroidJUnit4::class)
class FileItemRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun folderModel(itemCount: Int) = FileModel(
        file = File("/storage/emulated/0/Folder"),
        name = "Holiday Photos",
        path = "/storage/emulated/0/Holiday Photos",
        isDirectory = true,
        size = 2048L,
        extension = "",
        itemCount = itemCount,
        isSelected = true
    )

    @Test
    fun listItemShowsNameAndChildCount() {
        composeRule.setContent {
            com.foss.simpleshare.ui.theme.SimpleShareTheme {
                FileListItem(
                    file = folderModel(itemCount = 12),
                    showThumbnail = false,
                    isPressed = false,
                    onClick = {}
                )
            }
        }

        composeRule.onNodeWithText("Holiday Photos").assertIsDisplayed()
        composeRule.onNodeWithText("12 items", substring = true).assertIsDisplayed()
    }

    @Test
    fun calculatingFolderShowsPlaceholderInsteadOfMinusOne() {
        composeRule.setContent {
            com.foss.simpleshare.ui.theme.SimpleShareTheme {
                FileListItem(
                    file = folderModel(itemCount = -1).copy(size = -1L),
                    showThumbnail = false,
                    isPressed = false,
                    onClick = {}
                )
            }
        }

        // The "-1" placeholder must never leak into the UI while calculating.
        composeRule.onNodeWithText("-1", substring = true).assertDoesNotExist()
    }
}
