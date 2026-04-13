/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile.webserver

import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.R as CoreR
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.theme.StartServerGreen
import org.kiwix.kiwixmobile.core.ui.theme.StopServerRed
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode
import org.kiwix.kiwixmobile.ui.BOOK_ITEM_CHECKBOX_TESTING_TAG
import java.io.File

@RunWith(AndroidJUnit4::class)
class ZimHostScreenTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private val context = InstrumentationRegistry.getInstrumentation().targetContext

  private fun testBookOnDisk(
    id: String = "id",
    title: String = "Kiwix Book",
    isSelected: Boolean = false
  ) = BookOnDisk(
    book = LibkiwixBook(
      _id = id,
      _title = title
    ),
    zimReaderSource = ZimReaderSource(File("")),
    isSelected = isSelected
  )

  @Test
  fun zimHostScreen_whenServerStopped_showsStartButtonAndNoQrOrShareIcon() {
    composeTestRule.setContent {
      ZimHostScreen(
        serverIpText = "",
        showShareIcon = false,
        shareIconClick = {},
        qrImageItem = false to IconItem.Drawable(R.drawable.ic_storage),
        booksList = emptyList(),
        startServerButtonItem = Triple(
          context.getString(CoreR.string.start_server_label),
          StartServerGreen
        ) {},
        selectionMode = SelectionMode.MULTI,
        navigationIcon = { Text("Back") }
      )
    }

    composeTestRule.apply {
      onNodeWithTag(START_SERVER_BUTTON_TESTING_TAG)
        .assertIsDisplayed()
        .assertTextEquals(context.getString(CoreR.string.start_server_label).uppercase())

      onNodeWithTag(QR_IMAGE_TESTING_TAG).assertDoesNotExist()

      onNodeWithTag(SHARE_ICON_TESTING_TAG).assertDoesNotExist()
    }
  }

  @Test
  fun zimHostScreen_whenServerRunning_showsStopButtonAndQrAndShareIcon() {
    composeTestRule.setContent {
      ZimHostScreen(
        serverIpText = "192.168.69.7:8080",
        showShareIcon = true,
        shareIconClick = {},
        qrImageItem = true to IconItem.Drawable(R.drawable.ic_storage),
        booksList = emptyList(),
        startServerButtonItem = Triple(
          context.getString(CoreR.string.stop_server_label),
          StopServerRed
        ) {},
        selectionMode = SelectionMode.MULTI,
        navigationIcon = { Text("Back") }
      )
    }

    composeTestRule.apply {
      onNodeWithTag(START_SERVER_BUTTON_TESTING_TAG)
        .assertIsDisplayed()
        .assertTextEquals(context.getString(CoreR.string.stop_server_label).uppercase())

      onNodeWithTag(ZIM_HOST_SCREEN_TESTING_TAG).assertIsDisplayed()

      onNodeWithTag(QR_IMAGE_TESTING_TAG).assertIsDisplayed()
      onNodeWithTag(SHARE_ICON_TESTING_TAG).assertIsDisplayed()
    }
  }

  @Test
  fun zimHostScreen_startServerButtonClick_triggersClickCallback() {
    var wasClicked = false
    composeTestRule.setContent {
      ZimHostScreen(
        serverIpText = "",
        showShareIcon = false,
        shareIconClick = {},
        qrImageItem = false to IconItem.Drawable(R.drawable.ic_storage),
        booksList = emptyList(),
        startServerButtonItem = Triple(
          context.getString(CoreR.string.start_server_label),
          StartServerGreen
        ) { wasClicked = true },
        selectionMode = SelectionMode.MULTI,
        navigationIcon = { Text("Back") }
      )
    }

    composeTestRule.onNodeWithTag(START_SERVER_BUTTON_TESTING_TAG).performClick()

    assert(wasClicked) { "Start server button not clicked" }
  }

  @Test
  fun zimHostScreen_shareIconClick_triggersClickCallback() {
    var shareClicked = false
    composeTestRule.setContent {
      ZimHostScreen(
        serverIpText = "192.168.69.7:8080",
        showShareIcon = true,
        shareIconClick = { shareClicked = true },
        qrImageItem = true to IconItem.Drawable(R.drawable.ic_storage),
        booksList = emptyList(),
        startServerButtonItem = Triple(
          context.getString(CoreR.string.stop_server_label),
          StopServerRed
        ) {},
        selectionMode = SelectionMode.MULTI,
        navigationIcon = { Text("Back") }
      )
    }

    composeTestRule.onNodeWithTag(SHARE_ICON_TESTING_TAG)
      .assertIsDisplayed()
      .performClick()

    assert(shareClicked) { "Share icon button not clicked" }
  }

  @Test
  fun zimHostScreen_withZimFiles_rendersCheckboxForEachBook() {
    val book1 = testBookOnDisk(id = "id1", title = "Kiwix Book One", isSelected = true)
    val book2 = testBookOnDisk(id = "id2", title = "Kiwix Book Two", isSelected = false)

    composeTestRule.setContent {
      ZimHostScreen(
        serverIpText = "",
        showShareIcon = false,
        shareIconClick = {},
        qrImageItem = false to IconItem.Drawable(R.drawable.ic_storage),
        booksList = listOf(book1, book2),
        startServerButtonItem = Triple(
          context.getString(CoreR.string.start_server_label),
          StartServerGreen
        ) {},
        selectionMode = SelectionMode.MULTI,
        navigationIcon = { Text("Back") }
      )
    }

    composeTestRule.apply {
      onNodeWithTag("${BOOK_ITEM_CHECKBOX_TESTING_TAG}$ZERO").assertIsDisplayed()
      onNodeWithTag("${BOOK_ITEM_CHECKBOX_TESTING_TAG}$ONE").assertIsDisplayed()
    }
  }

  @Test
  fun zimHostScreen_navigationIconClick_triggersCallback() {
    var navClicked = false
    composeTestRule.setContent {
      ZimHostScreen(
        serverIpText = "",
        showShareIcon = false,
        shareIconClick = {},
        qrImageItem = false to IconItem.Drawable(R.drawable.ic_storage),
        booksList = emptyList(),
        startServerButtonItem = Triple(
          context.getString(CoreR.string.start_server_label),
          StartServerGreen
        ) {},
        selectionMode = SelectionMode.MULTI,
        navigationIcon = {
          IconButton(onClick = { navClicked = true }) {
            Text("Back")
          }
        }
      )
    }

    composeTestRule.onNodeWithText("Back").performClick()

    assert(navClicked) { "Navigation icon button not clicked" }
  }
}
