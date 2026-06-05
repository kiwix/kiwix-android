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

package org.kiwix.kiwixmobile.ui

import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.zim_manager.Byte
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.ArticleCount
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode
import org.kiwix.kiwixmobile.ui.BookItemScreen.BOOK_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.ui.BookItemScreen.CHECKBOX_TESTING_TAG
import org.kiwix.kiwixmobile.ui.BookItemScreen.OFFLINE_IMAGE_TEST_TAG
import org.kiwix.sharedFunctions.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], application = TestApplication::class)
class BookItemUITest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private val context get() = RuntimeEnvironment.getApplication()

  private fun createBookOnDisk(
    title: String = "Kotlin Docs",
    description: String? = "Kotlin",
    date: String = "2026",
    size: String = "1024",
    articleCount: String? = "22",
    isSelected: Boolean = false
  ): BookOnDisk {
    return BookOnDisk(
      book = LibkiwixBook(
        _id = "book-id",
        _title = title,
        _description = description,
        _date = date,
        _size = size,
        _articleCount = articleCount,
        _language = "eng"
      ),
      zimReaderSource = ZimReaderSource(File("test.zim")),
      isSelected = isSelected
    )
  }

  private fun bookItem(
    index: Int,
    bookOnDisk: BookOnDisk,
    onClick: ((BookOnDisk) -> Unit)? = null,
    onLongClick: ((BookOnDisk) -> Unit)? = null,
    onMultiSelect: ((BookOnDisk) -> Unit)? = null,
    selectionMode: SelectionMode = SelectionMode.NORMAL,
  ) {
    composeTestRule.setContent {
      BookItem(index, bookOnDisk, onClick, onLongClick, onMultiSelect, selectionMode)
    }
  }

  @Test
  fun whenInitialized_bookInformationIsDisplayed() {
    val bookOnDisk = createBookOnDisk()

    bookItem(0, bookOnDisk)

    val expectedArticleCount = ArticleCount("22").toHumanReadable(context)

    val expectedSize = Byte("1024").humanReadable

    composeTestRule.onNodeWithText("Kotlin Docs").assertExists()
    composeTestRule.onNodeWithText("Kotlin").assertExists()
    composeTestRule.onNodeWithText("2026").assertExists()
    composeTestRule.onNodeWithText(expectedSize).assertExists()
    composeTestRule.onNodeWithText(expectedArticleCount).assertExists()
    composeTestRule.onNodeWithTag(OFFLINE_IMAGE_TEST_TAG, useUnmergedTree = true).assertExists()
  }

  @Test
  fun bookItem_whenClickedInNormalMode_onClickInvoked() {
    val bookOnDisk = createBookOnDisk()
    val onClick: (BookOnDisk) -> Unit = mockk(relaxed = true)

    bookItem(
      index = 0,
      bookOnDisk = bookOnDisk,
      onClick = onClick,
      selectionMode = SelectionMode.NORMAL
    )

    composeTestRule.onNodeWithTag(BOOK_ITEM_TESTING_TAG).performClick()

    verify(exactly = 1) {
      onClick(bookOnDisk)
    }
  }

  @Test
  fun bookItem_whenClickedInMultiSelectMode_onMultiSelectInvoked() {
    val bookOnDisk = createBookOnDisk()
    val onMultiSelect: (BookOnDisk) -> Unit = mockk(relaxed = true)

    bookItem(
      index = 0,
      bookOnDisk = bookOnDisk,
      onMultiSelect = onMultiSelect,
      selectionMode = SelectionMode.MULTI
    )

    composeTestRule.onNodeWithTag(BOOK_ITEM_TESTING_TAG).performClick()

    verify(exactly = 1) {
      onMultiSelect(bookOnDisk)
    }
  }

  @Test
  fun bookItem_whenSelectionModeNormal_checkBoxIsNotDisplayed() {
    val bookOnDisk = createBookOnDisk()

    bookItem(
      index = 0,
      bookOnDisk = bookOnDisk,
      selectionMode = SelectionMode.NORMAL
    )

    composeTestRule.onNodeWithTag("${CHECKBOX_TESTING_TAG}0").assertDoesNotExist()
  }

  @Test
  fun bookItem_whenLongClickedInNormalMode_onLongClickInvoked() {
    val bookOnDisk = createBookOnDisk()

    var longClick = false
    bookItem(
      index = 0,
      bookOnDisk = bookOnDisk,
      onLongClick = { longClick = true },
      selectionMode = SelectionMode.NORMAL
    )

    composeTestRule.onNodeWithTag(BOOK_ITEM_TESTING_TAG)
      .performTouchInput { longClick() }

    assertTrue(longClick)
  }

  @Test
  fun bookItem_whenLongClickedInMultiMode_onLongClickIsNotInvoked() {
    val bookOnDisk = createBookOnDisk()
    var longClick = false

    bookItem(
      index = 0,
      bookOnDisk = bookOnDisk,
      onLongClick = { longClick = true },
      selectionMode = SelectionMode.MULTI
    )

    composeTestRule.onNodeWithTag(BOOK_ITEM_TESTING_TAG)
      .performTouchInput {
        longClick()
      }

    assertFalse(longClick)
  }

  @Test
  fun bookItem_whenSelectionModeMulti_checkboxIsDisplayed() {
    val bookOnDisk = createBookOnDisk()

    bookItem(
      index = 0,
      bookOnDisk = bookOnDisk,
      selectionMode = SelectionMode.MULTI
    )

    composeTestRule.onNodeWithTag("${CHECKBOX_TESTING_TAG}0").assertIsDisplayed()
  }

  @Test
  fun whenBookSelected_checkboxIsChecked() {
    val bookOnDisk = createBookOnDisk(isSelected = true)

    bookItem(
      index = 0,
      bookOnDisk = bookOnDisk,
      selectionMode = SelectionMode.MULTI
    )

    composeTestRule.onNodeWithTag("${CHECKBOX_TESTING_TAG}0").assertIsOn()
  }

  @Test
  fun bookItem_whenBookNotSelected_checkboxIsUnchecked() {
    val bookOnDisk = createBookOnDisk(isSelected = false)

    bookItem(
      index = 0,
      bookOnDisk = bookOnDisk,
      selectionMode = SelectionMode.MULTI
    )

    composeTestRule.onNodeWithTag("${CHECKBOX_TESTING_TAG}0").assertIsOff()
  }

  @Test
  fun bookItem_whenDescriptionIsNull_emptyDescriptionIsDisplayed() {
    val bookOnDisk = createBookOnDisk(description = null)

    bookItem(index = 0, bookOnDisk = bookOnDisk)

    composeTestRule.onNodeWithText("").assertExists()
  }

  @Test
  fun bookItem_whenArticleCountIsNull_displaysEmptyArticleCount() {
    val bookOnDisk = createBookOnDisk(articleCount = null)

    bookItem(index = 0, bookOnDisk = bookOnDisk)

    val expectedArticleCount = ArticleCount("").toHumanReadable(context)

    composeTestRule.onNodeWithText(expectedArticleCount).assertExists()
  }
}
