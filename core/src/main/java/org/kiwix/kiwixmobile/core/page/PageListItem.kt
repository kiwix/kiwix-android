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

package org.kiwix.kiwixmobile.core.page

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextOverflow
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.downloader.model.Base64String
import org.kiwix.kiwixmobile.core.downloader.model.toPainter
import org.kiwix.kiwixmobile.core.page.adapter.OnItemClickListener
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.PAGE_LIST_ITEM_FAVICON_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP

const val PAGE_ITEM_TITLE_TESTING_TAG = "pageItemTitleTestingTag"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PageListItem(
  page: Page,
  itemClickListener: OnItemClickListener
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .combinedClickable(
        onClick = { itemClickListener.onItemClick(page) },
        onLongClick = { itemClickListener.onItemLongClick(page) }
      )
      .background(MaterialTheme.colorScheme.surface)
      .padding(
        horizontal = SIXTEEN_DP,
        vertical = EIGHT_DP
      ),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Image(
      painter = if (page.isSelected) {
        painterResource(id = R.drawable.ic_check_circle_blue_24dp)
      } else {
        Base64String(page.favicon).toPainter()
      },
      contentDescription = stringResource(R.string.fav_icon),
      modifier = Modifier
        .size(PAGE_LIST_ITEM_FAVICON_SIZE)
    )

    Spacer(modifier = Modifier.width(SIXTEEN_DP))

    Text(
      text = page.title,
      style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier
        .weight(1f)
        .semantics { testTag = PAGE_ITEM_TITLE_TESTING_TAG },
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}
