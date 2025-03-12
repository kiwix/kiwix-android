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

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.faviconToPainter
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk

@Suppress("UnusedParameter", "LongMethod", "ComposableLambdaParameterNaming")
@Composable
fun BookItem(
  bookOnDisk: BookOnDisk,
  onClick: (BookOnDisk) -> Unit,
  onLongClick: (BookOnDisk) -> Unit,
  onMultiSelect: (BookOnDisk) -> Unit,
  selectionMode: SelectionMode,
  isCheckboxVisible: Boolean = false,
  isChecked: Boolean = false,
  onCheckedChange: (Boolean) -> Unit = {},
  tags: @Composable () -> Unit = {}
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(dimensionResource(id = R.dimen.card_margin))
      .clickable { onClick(bookOnDisk) },
    shape = MaterialTheme.shapes.medium,
    elevation = CardDefaults.elevatedCardElevation()
  ) {
    Row(
      modifier = Modifier
        .padding(dimensionResource(id = R.dimen.activity_horizontal_margin))
        .fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (isCheckboxVisible) {
        Checkbox(
          checked = isChecked,
          onCheckedChange = onCheckedChange,
          modifier = Modifier.padding(end = 10.dp)
        )
      }

      Icon(
        painter = bookOnDisk.book.faviconToPainter(),
        contentDescription = stringResource(R.string.fav_icon),
        modifier = Modifier
          .size(40.dp)
          .padding(end = 10.dp)
      )

      Column(
        modifier = Modifier.weight(1f)
      ) {
        Text(
          text = bookOnDisk.book.title,
          style = MaterialTheme.typography.titleMedium
        )
        Text(
          text = bookOnDisk.book.description.orEmpty(),
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(top = 4.dp)
        ) {
          Text(
            text = bookOnDisk.book.date,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = bookOnDisk.book.size,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = bookOnDisk.book.articleCount.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        tags()
      }
    }
  }
}
