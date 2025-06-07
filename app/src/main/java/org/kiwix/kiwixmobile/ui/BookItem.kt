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

package org.kiwix.kiwixmobile.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.downloader.model.Base64String
import org.kiwix.kiwixmobile.core.downloader.model.toPainter
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.BOOK_ICON_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FIVE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FOUR_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWO_DP
import org.kiwix.kiwixmobile.core.zim_manager.Byte
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.ArticleCount
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode

const val BOOK_ITEM_CHECKBOX_TESTING_TAG = "bookItemCheckboxTestingTag"
const val BOOK_ITEM_TESTING_TAG = "bookItemTestingTag"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookItem(
  index: Int,
  bookOnDisk: BookOnDisk,
  onClick: ((BookOnDisk) -> Unit)? = null,
  onLongClick: ((BookOnDisk) -> Unit)? = null,
  onMultiSelect: ((BookOnDisk) -> Unit)? = null,
  selectionMode: SelectionMode = SelectionMode.NORMAL,
) {
  KiwixTheme {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(FIVE_DP)
        .combinedClickable(
          onClick = {
            when (selectionMode) {
              SelectionMode.MULTI -> onMultiSelect?.invoke(bookOnDisk)
              SelectionMode.NORMAL -> onClick?.invoke(bookOnDisk)
            }
          },
          onLongClick = {
            if (selectionMode == SelectionMode.NORMAL) {
              onLongClick?.invoke(bookOnDisk)
            }
          }
        )
        .testTag(BOOK_ITEM_TESTING_TAG),
      shape = MaterialTheme.shapes.extraSmall,
      elevation = CardDefaults.elevatedCardElevation(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
      BookContent(bookOnDisk, selectionMode, onMultiSelect, onClick, index)
    }
  }
}

@Composable
private fun BookContent(
  bookOnDisk: BookOnDisk,
  selectionMode: SelectionMode,
  onMultiSelect: ((BookOnDisk) -> Unit)?,
  onClick: ((BookOnDisk) -> Unit)?,
  index: Int,
) {
  Row(
    modifier = Modifier
      .padding(top = SIXTEEN_DP, start = SIXTEEN_DP)
      .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (selectionMode == SelectionMode.MULTI) {
      BookCheckbox(bookOnDisk, selectionMode, onMultiSelect, onClick, index)
    }
    BookIcon(bookOnDisk.book.favicon, isOnlineLibrary = false)
    BookDetails(Modifier.weight(1f), bookOnDisk)
  }
}

@Composable
private fun BookCheckbox(
  bookOnDisk: BookOnDisk,
  selectionMode: SelectionMode,
  onMultiSelect: ((BookOnDisk) -> Unit)?,
  onClick: ((BookOnDisk) -> Unit)?,
  index: Int
) {
  Checkbox(
    checked = bookOnDisk.isSelected,
    onCheckedChange = {
      when (selectionMode) {
        SelectionMode.MULTI -> onMultiSelect?.invoke(bookOnDisk)
        SelectionMode.NORMAL -> onClick?.invoke(bookOnDisk)
      }
    },
    modifier = Modifier.testTag("$BOOK_ITEM_CHECKBOX_TESTING_TAG$index")
  )
}

@Composable
fun BookIcon(iconSource: String, isOnlineLibrary: Boolean) {
  val modifier = Modifier.size(BOOK_ICON_SIZE)
  if (isOnlineLibrary) {
    AsyncImage(
      model = iconSource,
      contentDescription = stringResource(R.string.fav_icon),
      modifier = modifier,
      placeholder = painterResource(R.drawable.default_zim_file_icon),
      error = painterResource(R.drawable.default_zim_file_icon),
    )
  } else {
    Image(
      painter = Base64String(iconSource).toPainter(),
      contentDescription = stringResource(R.string.fav_icon),
      modifier = modifier
    )
  }
}

@Composable
private fun BookDetails(modifier: Modifier, bookOnDisk: BookOnDisk) {
  Column(modifier = modifier.padding(start = SIXTEEN_DP)) {
    BookTitle(bookOnDisk.book.title)
    Spacer(modifier = Modifier.height(TWO_DP))
    BookDescription(bookOnDisk.book.description.orEmpty())
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(top = FIVE_DP)
    ) {
      BookDate(bookOnDisk.book.date)
      Spacer(modifier = Modifier.width(EIGHT_DP))
      BookSize(Byte(bookOnDisk.book.size).humanReadable)
      Spacer(modifier = Modifier.width(EIGHT_DP))
      BookArticleCount(
        ArticleCount(bookOnDisk.book.articleCount.orEmpty())
          .toHumanReadable(LocalContext.current)
      )
    }
    Spacer(modifier = Modifier.height(FOUR_DP))
    TagsView(bookOnDisk.tags)
  }
}

@Composable
private fun BookArticleCount(articleCount: String) {
  Text(
    text = articleCount,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onTertiary
  )
}

@Composable
fun BookSize(size: String, modifier: Modifier = Modifier) {
  Text(
    text = size,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onTertiary,
    modifier = modifier
  )
}

@Composable
fun BookDate(date: String) {
  Text(
    text = date,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onTertiary
  )
}

@Composable
fun BookTitle(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleSmall
  )
}

@Composable
fun BookDescription(bookDescription: String) {
  Text(
    text = bookDescription,
    style = MaterialTheme.typography.bodyMedium,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
    color = MaterialTheme.colorScheme.onSecondary
  )
}
