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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.zIndex
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.ui.theme.PureGrey
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FIVE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.ONLINE_BOOK_DISABLED_COLOR_ALPHA
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWO_DP
import org.kiwix.kiwixmobile.core.zim_manager.Byte
import org.kiwix.kiwixmobile.ui.BookDate
import org.kiwix.kiwixmobile.ui.BookDescription
import org.kiwix.kiwixmobile.ui.BookIcon
import org.kiwix.kiwixmobile.ui.BookSize
import org.kiwix.kiwixmobile.ui.BookTitle
import org.kiwix.kiwixmobile.ui.TagsView
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.DetectingFileSystem
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem.BookItem

const val ONLINE_BOOK_ITEM_TESTING_TAG = "onlineBookItemTestingTag"
const val ONLINE_BOOK_SIZE_TEXT_TESTING_TAG = "onlineBookSizeTextTestingTag"

@Composable
fun OnlineBookItem(
  item: BookItem,
  bookUtils: BookUtils,
  availableSpaceCalculator: AvailableSpaceCalculator,
  onBookItemClick: (BookItem) -> Unit
) {
  var hasAvailableSpaceInStorage by remember { mutableStateOf(false) }
  LaunchedEffect(item, availableSpaceCalculator) {
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
      shape = MaterialTheme.shapes.extraSmall,
      elevation = CardDefaults.elevatedCardElevation(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
      Box(modifier = Modifier.fillMaxWidth()) {
        OnlineBookContent(item, bookUtils)
        ShowDetectingFileSystemUi(
          isClickable,
          item,
          onBookItemClick,
          hasAvailableSpaceInStorage,
          Modifier.matchParentSize()
        )
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShowDetectingFileSystemUi(
  isClickable: Boolean,
  item: BookItem,
  onBookItemClick: (BookItem) -> Unit,
  hasAvailableSpaceInStorage: Boolean,
  modifier: Modifier
) {
  if (!isClickable) {
    val context = LocalContext.current
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
        }
        .semantics {
          contentDescription = context.getString(R.string.detecting_file_system)
        }
        .combinedClickable(
          // Do nothing on normal click.
          onClick = {},
          onLongClick = {
            when (item.fileSystemState) {
              CannotWrite4GbFile -> context.toast(R.string.file_system_does_not_support_4gb)
              DetectingFileSystem -> context.toast(R.string.detecting_file_system)
              else -> {
                if (item.canBeDownloaded && !hasAvailableSpaceInStorage) {
                  onBookItemClick.invoke(item)
                } else {
                  throw IllegalStateException("impossible invalid state: ${item.fileSystemState}")
                }
              }
            }
          }
        )
    )
  }
}

@Composable
private fun OnlineBookContent(item: BookItem, bookUtils: BookUtils) {
  Row(
    modifier = Modifier
      .padding(top = SIXTEEN_DP, start = SIXTEEN_DP)
      .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    BookIcon(item.book.favicon, isOnlineLibrary = true)
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(start = SIXTEEN_DP)
    ) {
      BookTitle(item.book.title)
      Spacer(modifier = Modifier.height(TWO_DP))
      BookDescription(item.book.description.orEmpty())
      BookSizeAndDateRow(item)
      BookCreatorAndLanguageRow(item, bookUtils)
      TagsView(item.tags, hasCode = item.hashCode())
    }
  }
}

@Composable
private fun BookCreatorAndLanguageRow(item: BookItem, bookUtils: BookUtils) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = FIVE_DP)
      .padding(end = SIXTEEN_DP),
    verticalAlignment = Alignment.CenterVertically
  ) {
    BookCreator(item.book.creator, Modifier.weight(1f))
    BookLanguage(bookUtils.getLanguage(item.book.language))
  }
}

@Composable
private fun BookSizeAndDateRow(item: BookItem) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = FIVE_DP)
      .padding(end = SIXTEEN_DP),
    verticalAlignment = Alignment.CenterVertically
  ) {
    BookSize(
      Byte(item.book.size).humanReadable,
      modifier = Modifier
        .weight(1f)
        .testTag(ONLINE_BOOK_SIZE_TEXT_TESTING_TAG)
    )
    BookDate(item.book.date)
  }
}

@Composable
private fun BookCreator(creator: String, modifier: Modifier = Modifier) {
  Text(
    text = creator,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onTertiary,
    modifier = modifier
  )
}

@Composable
private fun BookLanguage(language: String) {
  Text(
    text = language,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onTertiary
  )
}
