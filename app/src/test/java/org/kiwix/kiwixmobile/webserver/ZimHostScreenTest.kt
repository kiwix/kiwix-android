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

import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.theme.StartServerGreen
import org.kiwix.kiwixmobile.core.ui.theme.StopServerRed
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode
import org.kiwix.kiwixmobile.utils.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.kiwix.kiwixmobile.core.R as CoreR

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], application = TestApplication::class)
class ZimHostScreenTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private val context get() = RuntimeEnvironment.getApplication()

  private fun fakeBookOnDisk(
    title: String = "Test Book",
    id: String = "book-id-1",
    isSelected: Boolean = false
  ): BookOnDisk {
    val book = LibkiwixBook(
      nativeBook = null,
      _id = id,
      _title = title,
      _language = "en",
      _size = "123456"
    )
    val source = mockk<ZimReaderSource>(relaxed = true)
    return BookOnDisk(
      book = book,
      zimReaderSource = source,
      isSelected = isSelected
    )
  }

  private fun renderZimHostScreen(
    serverIpText: String = "",
    showShareIcon: Boolean = false,
    shareIconClick: () -> Unit = {},
    qrVisible: Boolean = false,
    qrIcon: IconItem = IconItem.Drawable(R.drawable.ic_storage),
    booksList: List<BooksOnDiskListItem> = emptyList(),
    startServerButtonText: String = context.getString(CoreR.string.start_server_label),
    startServerButtonColor: Color = StartServerGreen,
    startServerButtonClick: () -> Unit = {},
    selectionMode: SelectionMode = SelectionMode.NORMAL,
    onClick: ((BookOnDisk) -> Unit)? = null,
    onLongClick: ((BookOnDisk) -> Unit)? = null,
    onMultiSelect: ((BookOnDisk) -> Unit)? = null
  ) {
    composeTestRule.setContent {
      ZimHostScreen(
        serverIpText = serverIpText,
        showShareIcon = showShareIcon,
        shareIconClick = shareIconClick,
        qrImageItem = qrVisible to qrIcon,
        booksList = booksList,
        startServerButtonItem = Triple(
          startServerButtonText,
          startServerButtonColor,
          startServerButtonClick
        ),
        selectionMode = selectionMode,
        onClick = onClick,
        onLongClick = onLongClick,
        onMultiSelect = onMultiSelect,
        navigationIcon = {}
      )
    }
  }

  @Test
  fun zimHostScreen_isDisplayed() {
    renderZimHostScreen()
    composeTestRule
      .onNodeWithTag(ZIM_HOST_SCREEN_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun zimHostScreen_whenStartServerButton_isAlwaysDisplayed() {
    renderZimHostScreen()
    composeTestRule
      .onNodeWithTag(START_SERVER_BUTTON_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun zimHostScreen_whenStartServerButton_showsStartLabelWhenServerStopped() {
    renderZimHostScreen(
      startServerButtonText = context.getString(CoreR.string.start_server_label),
      startServerButtonColor = StartServerGreen
    )
    composeTestRule.onNodeWithTag(START_SERVER_BUTTON_TESTING_TAG).assertIsDisplayed()
  }

  @Test
  fun zimHostScreen_whenStartServerButton_showsStopLabelWhenServerRunning() {
    renderZimHostScreen(
      startServerButtonText = context.getString(CoreR.string.stop_server_label),
      startServerButtonColor = StopServerRed
    )
    composeTestRule.onNodeWithTag(START_SERVER_BUTTON_TESTING_TAG).assertIsDisplayed()
  }

  @Test
  fun zimHostScreen_whenStartServerButton_triggersCallback() {
    var clicked = false
    renderZimHostScreen(startServerButtonClick = { clicked = true })
    composeTestRule
      .onNodeWithTag(START_SERVER_BUTTON_TESTING_TAG)
      .performClick()
    assertTrue("Start Server Button callback should be triggered", clicked)
  }

  @Test
  fun zimHostScreen_whenShareIconIsDisplayedWhenShowShareIconIsTrue() {
    renderZimHostScreen(showShareIcon = true)
    composeTestRule
      .onNodeWithTag(SHARE_ICON_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun zimHostScreen_whenShareIconIsHiddenWhenShowShareIconIsFalse() {
    renderZimHostScreen(showShareIcon = false)
    composeTestRule
      .onNodeWithTag(SHARE_ICON_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun zimHostScreen_whenShareIcon_triggersCallback() {
    var clicked = false
    renderZimHostScreen(
      showShareIcon = true,
      shareIconClick = { clicked = true }
    )
    composeTestRule
      .onNodeWithTag(SHARE_ICON_TESTING_TAG)
      .performClick()
    assertTrue("Share icon callback should be triggered", clicked)
  }

  @Test
  fun zimHostScreen_whenServerIpText_shareIconDisplayedAlongsideIp() {
    renderZimHostScreen(
      serverIpText = context.getString(
        CoreR.string.server_started_message,
        "192.168.69.1:8080"
      ),
      showShareIcon = true
    )
    composeTestRule
      .onNodeWithTag(SHARE_ICON_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun zimHostScreen_whenServerIpText_noShareIconWhenIpIsEmpty() {
    renderZimHostScreen(
      serverIpText = context.getString(CoreR.string.server_textview_default_message)
    )
    composeTestRule
      .onNodeWithTag(SHARE_ICON_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun zimHostScreen_whenQrImageIsDisplayedIsQrVisible() {
    renderZimHostScreen(qrVisible = true)
    composeTestRule
      .onNodeWithTag(QR_IMAGE_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun zimHostScreen_whenQrImageIsHiddenQrNotVisible() {
    renderZimHostScreen(qrVisible = false)
    composeTestRule
      .onNodeWithTag(QR_IMAGE_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun zimHostScreen_whenBooksList_displaysBookTitle() {
    val book = fakeBookOnDisk(title = "Wikipedia EN")
    renderZimHostScreen(booksList = listOf(book))
    composeTestRule
      .onNodeWithText("Wikipedia EN")
      .assertIsDisplayed()
  }

  @Test
  fun zimHostScreen_whenBooksList_displaysMultipleBooks() {
    val books = listOf(
      fakeBookOnDisk(title = "Wikipedia EN", id = "id-1"),
      fakeBookOnDisk(title = "Wiktionary FR", id = "id-2")
    )
    renderZimHostScreen(booksList = books)
    composeTestRule.onNodeWithText("Wikipedia EN").assertIsDisplayed()
    composeTestRule.onNodeWithText("Wiktionary FR").assertIsDisplayed()
  }

  @Test
  fun zimHostScreen_whenBooksListIsEmpty_noBooksProvided() {
    renderZimHostScreen(booksList = emptyList())
    composeTestRule
      .onNodeWithText("Wikipedia EN")
      .assertDoesNotExist()
  }

  @Test
  fun zimHostScreen_whenBookItem_triggersOnClickCallback() {
    var clickedBook: BookOnDisk? = null
    val book = fakeBookOnDisk(title = "Wikipedia EN")
    renderZimHostScreen(
      booksList = listOf(book),
      onClick = { clickedBook = it }
    )
    composeTestRule
      .onNodeWithText("Wikipedia EN")
      .performClick()
    assertTrue("onClick callback should be triggered on book item click", clickedBook != null)
  }

  @Test
  fun zimHostScreen_whenLanguageHeaderIsDisplayed() {
    val header = BooksOnDiskListItem.LanguageItem(id = "en", text = "English")
    renderZimHostScreen(booksList = listOf(header))
    composeTestRule
      .onNodeWithText("English")
      .assertIsDisplayed()
  }

  @Test
  fun zimHostScreen_whenServerStartedState_displaysShareIconQrAndStopButton() {
    renderZimHostScreen(
      serverIpText = context.getString(
        CoreR.string.server_started_message,
        "192.168.69.1:8080"
      ),
      showShareIcon = true,
      qrVisible = true,
      startServerButtonText = context.getString(CoreR.string.stop_server_label),
      startServerButtonColor = StopServerRed
    )
    composeTestRule.onNodeWithTag(SHARE_ICON_TESTING_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(QR_IMAGE_TESTING_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(START_SERVER_BUTTON_TESTING_TAG).assertIsDisplayed()
  }

  @Test
  fun zimHostScreen_whenServerStoppedState_hidesShareIconAndQr() {
    renderZimHostScreen(
      serverIpText = context.getString(CoreR.string.server_textview_default_message),
      showShareIcon = false,
      qrVisible = false,
      startServerButtonText = context.getString(CoreR.string.start_server_label),
      startServerButtonColor = StartServerGreen
    )
    composeTestRule.onNodeWithTag(SHARE_ICON_TESTING_TAG).assertDoesNotExist()
    composeTestRule.onNodeWithTag(QR_IMAGE_TESTING_TAG).assertDoesNotExist()
    composeTestRule.onNodeWithTag(START_SERVER_BUTTON_TESTING_TAG).assertIsDisplayed()
  }
}
