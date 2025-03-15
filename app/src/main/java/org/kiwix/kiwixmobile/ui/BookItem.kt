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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.faviconToPainter
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.BOOK_ICON_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FIVE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FOUR_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWO_DP
import org.kiwix.kiwixmobile.core.zim_manager.KiloByte
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.ArticleCount
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookItem(
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
        ),
      shape = MaterialTheme.shapes.extraSmall,
      elevation = CardDefaults.elevatedCardElevation(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
      BookContent(bookOnDisk, selectionMode, onMultiSelect, onClick)
    }
  }
}

@Composable
private fun BookContent(
  bookOnDisk: BookOnDisk,
  selectionMode: SelectionMode,
  onMultiSelect: ((BookOnDisk) -> Unit)?,
  onClick: ((BookOnDisk) -> Unit)?,
) {
  Row(
    modifier = Modifier
      .padding(top = SIXTEEN_DP, start = SIXTEEN_DP)
      .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (selectionMode == SelectionMode.MULTI) {
      BookCheckbox(bookOnDisk, selectionMode, onMultiSelect, onClick)
    }
    BookIcon(bookOnDisk.book.faviconToPainter())
    BookDetails(Modifier.weight(1f), bookOnDisk)
  }
}

@Composable
private fun BookCheckbox(
  bookOnDisk: BookOnDisk,
  selectionMode: SelectionMode,
  onMultiSelect: ((BookOnDisk) -> Unit)?,
  onClick: ((BookOnDisk) -> Unit)?
) {
  Checkbox(
    checked = bookOnDisk.isSelected,
    onCheckedChange = {
      when (selectionMode) {
        SelectionMode.MULTI -> onMultiSelect?.invoke(bookOnDisk)
        SelectionMode.NORMAL -> onClick?.invoke(bookOnDisk)
      }
    }
  )
}

@Composable
fun BookIcon(painter: Painter) {
  Icon(
    painter = painter,
    contentDescription = stringResource(R.string.fav_icon),
    modifier = Modifier
      .size(BOOK_ICON_SIZE),
    tint = Color.Unspecified
  )
}

@Composable
private fun BookDetails(modifier: Modifier, bookOnDisk: BookOnDisk) {
  Column(modifier = modifier.padding(start = SIXTEEN_DP)) {
    Text(
      text = bookOnDisk.book.title,
      style = MaterialTheme.typography.titleSmall
    )
    Spacer(modifier = Modifier.height(TWO_DP))
    Text(
      text = bookOnDisk.book.description.orEmpty(),
      style = MaterialTheme.typography.bodyMedium,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      color = MaterialTheme.colorScheme.onSecondary
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(top = FIVE_DP)
    ) {
      Text(
        text = bookOnDisk.book.date,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onTertiary
      )
      Spacer(modifier = Modifier.width(EIGHT_DP))
      Text(
        text = KiloByte(bookOnDisk.book.size).humanReadable,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onTertiary
      )
      Spacer(modifier = Modifier.width(EIGHT_DP))
      Text(
        text = ArticleCount(bookOnDisk.book.articleCount.orEmpty())
          .toHumanReadable(LocalContext.current),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onTertiary
      )
    }
    Spacer(modifier = Modifier.height(FOUR_DP))
    TagsView(bookOnDisk.tags)
  }
}
