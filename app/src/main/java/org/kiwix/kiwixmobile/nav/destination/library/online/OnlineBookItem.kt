/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.extensions.throttledClickable
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.ui.theme.PureGrey
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FIVE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.ONLINE_BOOK_DISABLED_COLOR_ALPHA
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWELVE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWO_DP
import org.kiwix.kiwixmobile.ui.BookDetails
import org.kiwix.kiwixmobile.ui.BookIconAndSize
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.DetectingFileSystem
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.BookItem

const val ONLINE_BOOK_ITEM_TESTING_TAG = "onlineBookItemTestingTag"
const val ONLINE_BOOK_SIZE_TEXT_TESTING_TAG = "onlineBookSizeTextTestingTag"
const val ONLINE_BOOK_OVERLAY_TESTING_TAG = "onlineBookOverlayTestingTag"
const val ONLINE_BOOK_CREATOR_TEXT_TESTING_TAG = "onlineBookCreatorTextTestingTag"
const val ONLINE_BOOK_LANGUAGE_TEXT_TESTING_TAG = "onlineBookLanguageTextTestingTag"

@Composable
fun OnlineBookItem(
  index: Int,
  item: BookItem,
  bookUtils: BookUtils,
  availableSpaceCalculator: AvailableSpaceCalculator,
  onBookItemClick: (BookItem) -> Unit
) {
  var hasAvailableSpaceInStorage by remember { mutableStateOf(false) }
  LaunchedEffect(item.book, item.fileSystemState, availableSpaceCalculator) {
    hasAvailableSpaceInStorage =
      availableSpaceCalculator.hasAvailableSpaceForBook(item.book)
  }
  val isClickable = item.canBeDownloaded && hasAvailableSpaceInStorage
  KiwixTheme {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(FIVE_DP)
        .testTag(ONLINE_BOOK_ITEM_TESTING_TAG)
        .clickable(enabled = isClickable) {
          onBookItemClick.invoke(item)
        },
      shape = RoundedCornerShape(TWO_DP),
      elevation = CardDefaults.elevatedCardElevation(TWO_DP),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
      Box(modifier = Modifier.fillMaxWidth()) {
        OnlineBookContent(item, bookUtils, index)
        ShowDetectingFileSystemUi(
          isClickable,
          hasAvailableSpaceInStorage,
          item,
          onBookItemClick,
          Modifier.matchParentSize()
        )
      }
    }
  }
}

@Composable
private fun ShowDetectingFileSystemUi(
  isClickable: Boolean,
  hasAvailableSpaceInStorage: Boolean,
  item: BookItem,
  onBookItemClick: (BookItem) -> Unit,
  modifier: Modifier
) {
  if (!isClickable) {
    val context = LocalContext.current
    val handleStorageCheckClick = {
      when (item.fileSystemState) {
        CannotWrite4GbFile -> context.toast(R.string.file_system_does_not_support_4gb)
        DetectingFileSystem -> context.toast(R.string.detecting_file_system)
        else -> {
          if (item.canBeDownloaded && !hasAvailableSpaceInStorage) {
            onBookItemClick.invoke(item)
          }
        }
      }
    }

    Box(
      modifier = modifier
        .background(color = PureGrey.copy(alpha = ONLINE_BOOK_DISABLED_COLOR_ALPHA))
        .zIndex(1f)
        .pointerInput(Unit) {
          awaitPointerEventScope {
            while (true) {
              awaitPointerEvent()
            }
          }
        }.semantics {
          contentDescription = context.getString(R.string.detecting_file_system)
        }.testTag(ONLINE_BOOK_OVERLAY_TESTING_TAG)
        .throttledClickable(
          onClick = handleStorageCheckClick,
          onLongClick = handleStorageCheckClick
        )
    )
  }
}

@Composable
private fun OnlineBookContent(item: BookItem, bookUtils: BookUtils, index: Int) {
  Row(
    modifier = Modifier
      .padding(start = SIXTEEN_DP, top = SIXTEEN_DP, end = SIXTEEN_DP, bottom = TWELVE_DP)
      .fillMaxWidth(),
    verticalAlignment = Alignment.Top
  ) {
    BookIconAndSize(
      iconSource = item.book.favicon,
      size = item.book.size,
      isOnlineLibrary = true,
      index = index,
      sizeModifier = Modifier.testTag(ONLINE_BOOK_SIZE_TEXT_TESTING_TAG)
    )
    BookDetails(
      modifier = Modifier.weight(1f),
      title = item.book.title,
      date = item.book.date,
      description = item.book.description.orEmpty(),
      tags = item.tags,
      index = index,
      subheadContent = if (item.book.creator.isNotEmpty()) {
        {
          BookCreator(
            creator = item.book.creator,
            modifier = Modifier.testTag(ONLINE_BOOK_CREATOR_TEXT_TESTING_TAG)
          )
        }
      } else {
        null
      }
    ) {
      BookLanguage(
        language = bookUtils.getLanguage(item.book.language),
        modifier = Modifier.testTag(ONLINE_BOOK_LANGUAGE_TEXT_TESTING_TAG)
      )
    }
  }
}

@Composable
fun BookCreator(creator: String, modifier: Modifier = Modifier) {
  Text(
    text = creator,
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    modifier = modifier.semantics { contentDescription = creator }
  )
}

@Composable
fun BookLanguage(language: String, modifier: Modifier = Modifier) {
  Text(
    text = language,
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines = 1,
    modifier = modifier.semantics { contentDescription = language }
  )
}
