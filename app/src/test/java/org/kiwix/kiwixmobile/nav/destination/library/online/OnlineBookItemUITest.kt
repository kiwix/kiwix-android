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

package org.kiwix.kiwixmobile.nav.destination.library.online

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.content.ContextCompat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.zim_manager.Byte
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CanWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.DetectingFileSystem
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.NotEnoughSpaceFor4GbFile
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.BookItem
import org.kiwix.sharedFunctions.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], application = TestApplication::class)
class OnlineBookItemUITest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private val context get() = RuntimeEnvironment.getApplication()

  private lateinit var mockOnBookItemClick: (BookItem) -> Unit
  private lateinit var mockAvailableSpaceCalculator: AvailableSpaceCalculator
  private lateinit var mockBookUtils: BookUtils

  @Before
  fun setUp() {
    mockOnBookItemClick = mockk(relaxed = true)
    mockAvailableSpaceCalculator = mockk(relaxed = true)
    mockBookUtils = mockk(relaxed = true)
    mockkObject(CoreApp.Companion)
    mockkStatic(ContextCompat::class)
    every { CoreApp.instance } returns mockk(relaxed = true)
    every { ContextCompat.getContextForLanguage(any()) } returns context
    coEvery {
      mockAvailableSpaceCalculator.hasAvailableSpaceForBook(any())
    } returns true
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  private fun mockLibkiwixBook(
    title: String = "Kotlin",
    description: String = "A programming language",
    favicon: String = "https://kiwix.org/favicon.png",
    language: String = "eng",
    creator: String = "Kiwix",
    date: String = "2024-01-01",
    size: String = "51200",
    id: String = "book-id-1",
    tags: String = ""
  ): LibkiwixBook {
    val book = LibkiwixBook()
    book.title = title
    book.description = description
    book.favicon = favicon
    book.language = language
    book.creator = creator
    book.date = date
    book.size = size
    book.id = id
    book.tags = tags
    return book
  }

  private fun mockBookItem(
    book: LibkiwixBook = mockLibkiwixBook(),
    fileSystemState: FileSystemState = CanWrite4GbFile
  ) = BookItem(
    book = book,
    fileSystemState = fileSystemState
  )

  private fun setContent(
    item: BookItem = mockBookItem(),
    onBookItemClick: (BookItem) -> Unit = mockOnBookItemClick
  ) {
    composeTestRule.setContent {
      OnlineBookItem(
        index = ZERO,
        item = item,
        bookUtils = mockBookUtils,
        availableSpaceCalculator = mockAvailableSpaceCalculator,
        onBookItemClick = onBookItemClick
      )
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun onlineBookItem_checksAvailableSpaceWhenRendered() {
    val book = mockLibkiwixBook()

    setContent(item = mockBookItem(book = book))

    coVerify(exactly = 1) {
      mockAvailableSpaceCalculator.hasAvailableSpaceForBook(book)
    }
  }

  @Test
  fun onlineBookItem_whenRendered_cardIsDisplayed() {
    setContent()
    composeTestRule
      .onAllNodesWithTag(ONLINE_BOOK_ITEM_TESTING_TAG)[ZERO]
      .assertIsDisplayed()
  }

  @Test
  fun onlineBookItem_whenDescriptionIsEmpty_cardStillRendersWithoutCrash() {
    setContent(item = mockBookItem(book = mockLibkiwixBook(description = "")))
    composeTestRule
      .onAllNodesWithTag(ONLINE_BOOK_ITEM_TESTING_TAG)[ZERO]
      .assertIsDisplayed()
  }

  @Test
  fun onlineBookItem_whenNoTags_cardRendersWithoutCrash() {
    setContent(item = mockBookItem(book = mockLibkiwixBook(tags = "")))
    composeTestRule
      .onNodeWithTag(ONLINE_BOOK_ITEM_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun onlineBookItem_bookTitleIsInTree() {
    setContent(item = mockBookItem(book = mockLibkiwixBook(title = "Kotlin")))
    composeTestRule
      .onNode(hasContentDescription("Kotlin$ZERO"))
      .assertExists()
  }

  @Test
  fun onlineBookItem_bookDescriptionIsInTree() {
    setContent(item = mockBookItem(book = mockLibkiwixBook(description = "A programming language")))
    composeTestRule
      .onNode(hasContentDescription("A programming language$ZERO"))
      .assertExists()
  }

  @Test
  fun onlineBookItem_bookSizeTagIsInTree() {
    setContent()
    composeTestRule
      .onNodeWithTag(ONLINE_BOOK_SIZE_TEXT_TESTING_TAG, useUnmergedTree = true)
      .assertExists()
  }

  @Test
  fun onlineBookItem_bookSizeContentDescriptionIsCorrect() {
    setContent(item = mockBookItem(book = mockLibkiwixBook(size = "51200")))
    val expectedSize = Byte("51200").humanReadable
    composeTestRule
      .onNode(hasContentDescription("$expectedSize$ZERO"))
      .assertExists()
  }

  @Test
  fun onlineBookItem_bookDateTagIsInTree() {
    setContent(item = mockBookItem(book = mockLibkiwixBook(date = "2024-01-01")))
    composeTestRule
      .onNodeWithTag(ONLINE_BOOK_DATE_TEXT_TESTING_TAG, useUnmergedTree = true)
      .assertExists()
  }

  @Test
  fun onlineBookItem_bookDateContentDescriptionIsCorrect() {
    val date = "2024-01-01"
    setContent(item = mockBookItem(book = mockLibkiwixBook(date = date)))
    composeTestRule
      .onNode(hasContentDescription("$date$ZERO"))
      .assertExists()
  }

  @Test
  fun onlineBookItem_bookCreatorTagIsInTree() {
    setContent(item = mockBookItem(book = mockLibkiwixBook(creator = "Kiwix")))
    composeTestRule
      .onNodeWithTag(ONLINE_BOOK_CREATOR_TEXT_TESTING_TAG, useUnmergedTree = true)
      .assertExists()
  }

  @Test
  fun onlineBookItem_bookLanguageTagIsInTree() {
    every { mockBookUtils.getLanguage("eng") } returns "English"
    setContent(item = mockBookItem(book = mockLibkiwixBook(language = "eng")))
    composeTestRule
      .onNodeWithTag(ONLINE_BOOK_LANGUAGE_TEXT_TESTING_TAG, useUnmergedTree = true)
      .assertExists()
  }

  @Test
  fun onlineBookItem_bookLanguageContentIsCorrect() {
    every { mockBookUtils.getLanguage("eng") } returns "English"
    setContent(item = mockBookItem(book = mockLibkiwixBook(language = "eng")))
    composeTestRule
      .onNode(hasContentDescription("English"), useUnmergedTree = true)
      .assertExists()
  }

  @Test
  fun onlineBookItem_bookIconIsInTree() {
    val favicon = "https://kiwix.org/favicon.png"
    setContent(item = mockBookItem(book = mockLibkiwixBook(favicon = favicon)))
    val expectedDesc = context.getString(
      org.kiwix.kiwixmobile.core.R.string.fav_icon
    ) + favicon.hashCode()
    composeTestRule
      .onNode(hasContentDescription(expectedDesc))
      .assertExists()
  }

  @Test
  fun onlineBookItem_whenTagsPresent_cardRendersWithoutCrash() {
    setContent(item = mockBookItem(book = mockLibkiwixBook(tags = "_videos:yes;_pictures:yes")))
    composeTestRule
      .onNodeWithTag(ONLINE_BOOK_ITEM_TESTING_TAG)
      .assertExists()
  }

  @Test
  fun onlineBookItem_whenClickable_clickInvokesCallback() {
    val item = mockBookItem(fileSystemState = CanWrite4GbFile)
    setContent(item = item)
    composeTestRule
      .onNodeWithTag(ONLINE_BOOK_ITEM_TESTING_TAG)
      .performClick()
    verify(exactly = 1) { mockOnBookItemClick.invoke(item) }
  }

  @Test
  fun onlineBookItem_whenClickable_callbackNotCalledBeforeClick() {
    setContent(item = mockBookItem(fileSystemState = CanWrite4GbFile))
    verify(exactly = 0) { mockOnBookItemClick.invoke(any()) }
  }

  @Test
  fun onlineBookItem_multipleClicks_callbackInvokedEachTime() {
    val item = mockBookItem(fileSystemState = CanWrite4GbFile)
    setContent(item = item)
    composeTestRule.onNodeWithTag(ONLINE_BOOK_ITEM_TESTING_TAG).performClick()
    composeTestRule.onNodeWithTag(ONLINE_BOOK_ITEM_TESTING_TAG).performClick()
    composeTestRule.onNodeWithTag(ONLINE_BOOK_ITEM_TESTING_TAG).performClick()
    verify(exactly = 3) { mockOnBookItemClick.invoke(item) }
  }

  @Test
  fun onlineBookItem_whenNotEnoughSpaceFor4GbFile_bookIsClickable() {
    val item = mockBookItem(fileSystemState = NotEnoughSpaceFor4GbFile)
    setContent(item = item)
    composeTestRule
      .onNodeWithTag(ONLINE_BOOK_ITEM_TESTING_TAG)
      .performClick()
    verify(exactly = 1) { mockOnBookItemClick.invoke(item) }
  }

  @Test
  fun onlineBookItem_whenLargeBookAndCanWrite4GbFile_bookIsClickable() {
    val item = mockBookItem(
      book = mockLibkiwixBook(size = "5000000"),
      fileSystemState = CanWrite4GbFile
    )
    setContent(item = item)
    composeTestRule
      .onNodeWithTag(ONLINE_BOOK_ITEM_TESTING_TAG)
      .performClick()
    verify(exactly = 1) { mockOnBookItemClick.invoke(item) }
  }

  @Test
  fun onlineBookItem_whenSmallBookAndCannotWrite4GbFile_bookIsClickable() {
    val item = mockBookItem(
      book = mockLibkiwixBook(size = "51200"),
      fileSystemState = CannotWrite4GbFile
    )
    setContent(item = item)
    composeTestRule
      .onNodeWithTag(ONLINE_BOOK_ITEM_TESTING_TAG)
      .performClick()
    verify(exactly = 1) { mockOnBookItemClick.invoke(item) }
  }

  @Test
  fun onlineBookItem_whenCanWrite4GbFile_overlayDoesNotExist() {
    setContent(item = mockBookItem(fileSystemState = CanWrite4GbFile))
    composeTestRule
      .onNodeWithTag(ONLINE_BOOK_OVERLAY_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun onlineBookItem_whenSpaceAvailable_overlayDoesNotExist() {
    coEvery {
      mockAvailableSpaceCalculator.hasAvailableSpaceForBook(any())
    } returns true
    setContent(item = mockBookItem(fileSystemState = CanWrite4GbFile))
    composeTestRule
      .onNodeWithTag(ONLINE_BOOK_OVERLAY_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun onlineBookItem_whenDetectingFileSystemWithSpaceAvailable_overlayDoesNotExist() {
    coEvery {
      mockAvailableSpaceCalculator.hasAvailableSpaceForBook(any())
    } returns true
    setContent(
      item = mockBookItem(
        book = mockLibkiwixBook(size = "51200"),
        fileSystemState = DetectingFileSystem
      )
    )
    composeTestRule
      .onNodeWithTag(ONLINE_BOOK_OVERLAY_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun onlineBookItem_whenSmallBookAndCannotWrite4GbFile_overlayDoesNotExist() {
    setContent(
      item = mockBookItem(
        book = mockLibkiwixBook(size = "51200"),
        fileSystemState = CannotWrite4GbFile
      )
    )
    composeTestRule
      .onNodeWithTag(ONLINE_BOOK_OVERLAY_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun onlineBookItem_whenDetectingFileSystem_overlayExistsInTree() {
    coEvery {
      mockAvailableSpaceCalculator.hasAvailableSpaceForBook(any())
    } returns false
    setContent(item = mockBookItem(fileSystemState = DetectingFileSystem))
    composeTestRule
      .onNodeWithTag(ONLINE_BOOK_OVERLAY_TESTING_TAG)
      .assertExists()
  }

  @Test
  fun onlineBookItem_whenCannotWrite4GbFile_overlayExistsInTree() {
    setContent(
      item = mockBookItem(
        book = mockLibkiwixBook(size = "5000000"),
        fileSystemState = CannotWrite4GbFile
      )
    )
    composeTestRule
      .onNodeWithTag(ONLINE_BOOK_OVERLAY_TESTING_TAG)
      .assertExists()
  }

  @Test
  fun onlineBookItem_whenLargeBookAndCanWrite4GbFile_overlayDoesNotExist() {
    setContent(
      item = mockBookItem(
        book = mockLibkiwixBook(size = "5000000"),
        fileSystemState = CanWrite4GbFile
      )
    )
    composeTestRule
      .onNodeWithTag(ONLINE_BOOK_OVERLAY_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun onlineBookItem_whenNotEnoughSpace_overlayInvokesCallback() {
    coEvery {
      mockAvailableSpaceCalculator.hasAvailableSpaceForBook(any())
    } returns false
    val item = mockBookItem(fileSystemState = CanWrite4GbFile)
    setContent(item = item)
    composeTestRule
      .onNodeWithTag(ONLINE_BOOK_OVERLAY_TESTING_TAG)
      .performClick()
    verify(exactly = 1) { mockOnBookItemClick.invoke(item) }
  }

  @Test
  fun onlineBookItem_whenDetectingFileSystem_overlayAbsorbsClick() {
    coEvery {
      mockAvailableSpaceCalculator.hasAvailableSpaceForBook(any())
    } returns false
    setContent(item = mockBookItem(fileSystemState = DetectingFileSystem))
    composeTestRule
      .onNodeWithTag(ONLINE_BOOK_OVERLAY_TESTING_TAG)
      .performClick()
    verify(exactly = 0) { mockOnBookItemClick.invoke(any()) }
  }

  @Test
  fun onlineBookItem_overlayVisibilityUpdatesWhenAvailableSpaceChanges() {
    val book = mockLibkiwixBook()

    var item by mutableStateOf(
      mockBookItem(book = book)
    )

    coEvery {
      mockAvailableSpaceCalculator.hasAvailableSpaceForBook(book)
    } returns false

    composeTestRule.setContent {
      OnlineBookItem(
        index = ZERO,
        item = item,
        bookUtils = mockBookUtils,
        availableSpaceCalculator = mockAvailableSpaceCalculator,
        onBookItemClick = mockOnBookItemClick
      )
    }

    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag(ONLINE_BOOK_OVERLAY_TESTING_TAG)
      .assertExists()

    coEvery {
      mockAvailableSpaceCalculator.hasAvailableSpaceForBook(book)
    } returns true

    composeTestRule.runOnUiThread {
      item = item.copy(
        fileSystemState = DetectingFileSystem
      )
    }

    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag(ONLINE_BOOK_OVERLAY_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun onlineBookItem_whenCannotWrite4GbFile_overlayAbsorbsClick() {
    setContent(
      item = mockBookItem(
        book = mockLibkiwixBook(size = "5000000"),
        fileSystemState = CannotWrite4GbFile
      )
    )
    composeTestRule
      .onNodeWithTag(ONLINE_BOOK_OVERLAY_TESTING_TAG)
      .performClick()
    verify(exactly = 0) { mockOnBookItemClick.invoke(any()) }
  }

  @Test
  fun onlineBookItem_multipleItems_allCardsAreInTree() {
    val item1 = mockBookItem(book = mockLibkiwixBook(id = "book-id-1", title = "Kotlin"))
    val item2 = mockBookItem(book = mockLibkiwixBook(id = "book-id-2", title = "Java"))
    composeTestRule.setContent {
      Column {
        OnlineBookItem(
          index = 0,
          item = item1,
          bookUtils = mockBookUtils,
          availableSpaceCalculator = mockAvailableSpaceCalculator,
          onBookItemClick = mockOnBookItemClick
        )
        OnlineBookItem(
          index = 1,
          item = item2,
          bookUtils = mockBookUtils,
          availableSpaceCalculator = mockAvailableSpaceCalculator,
          onBookItemClick = mockOnBookItemClick
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule
      .onAllNodesWithTag(ONLINE_BOOK_ITEM_TESTING_TAG)[0]
      .assertExists()
    composeTestRule
      .onAllNodesWithTag(ONLINE_BOOK_ITEM_TESTING_TAG)[1]
      .assertExists()
    composeTestRule
      .onAllNodesWithTag(ONLINE_BOOK_ITEM_TESTING_TAG)[0]
      .assertIsDisplayed()
  }

  @Test
  fun onlineBookItem_multipleItems_titlesAreDistinctByIndex() {
    val item1 = mockBookItem(book = mockLibkiwixBook(id = "book-id-1", title = "Kotlin"))
    val item2 = mockBookItem(book = mockLibkiwixBook(id = "book-id-2", title = "Java"))
    composeTestRule.setContent {
      Column {
        OnlineBookItem(
          index = 0,
          item = item1,
          bookUtils = mockBookUtils,
          availableSpaceCalculator = mockAvailableSpaceCalculator,
          onBookItemClick = mockOnBookItemClick
        )
        OnlineBookItem(
          index = 1,
          item = item2,
          bookUtils = mockBookUtils,
          availableSpaceCalculator = mockAvailableSpaceCalculator,
          onBookItemClick = mockOnBookItemClick
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNode(hasContentDescription("Kotlin0")).assertExists()
    composeTestRule.onNode(hasContentDescription("Java1")).assertExists()
  }

  @Test
  fun onlineBookItem_multipleItems_clickingFirstInvokesCorrectCallback() {
    val item1 = mockBookItem(book = mockLibkiwixBook(id = "book-id-1", title = "Kotlin"))
    val item2 = mockBookItem(book = mockLibkiwixBook(id = "book-id-2", title = "Java"))
    val onClickItem1: (BookItem) -> Unit = mockk(relaxed = true)
    val onClickItem2: (BookItem) -> Unit = mockk(relaxed = true)
    composeTestRule.setContent {
      Column {
        OnlineBookItem(
          index = 0,
          item = item1,
          bookUtils = mockBookUtils,
          availableSpaceCalculator = mockAvailableSpaceCalculator,
          onBookItemClick = onClickItem1
        )
        OnlineBookItem(
          index = 1,
          item = item2,
          bookUtils = mockBookUtils,
          availableSpaceCalculator = mockAvailableSpaceCalculator,
          onBookItemClick = onClickItem2
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule
      .onAllNodesWithTag(ONLINE_BOOK_ITEM_TESTING_TAG)[0]
      .performClick()
    verify(exactly = 1) { onClickItem1.invoke(any()) }
    verify(exactly = 0) { onClickItem2.invoke(any()) }
  }
}
