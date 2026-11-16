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

import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextOverflow
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.ui.components.BookIcon
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.PAGE_LIST_ITEM_FAVICON_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP

const val PAGE_ITEM_TESTING_TAG = "pageItemTestingTag"
const val PAGE_ITEM_ICON_TESTING_TAG = "pageItemIconTestingTag"
const val PAGE_ITEM_TITLE_TESTING_TAG = "pageItemTitleTestingTag"

@Composable
fun PageListItem(
  index: Int,
  page: Page,
  onItemClick: (Page) -> Unit,
  onItemLongClick: (Page) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .combinedClickable(
        onClick = { onItemClick(page) },
        onLongClick = { onItemLongClick(page) }
      )
      .padding(
        horizontal = SIXTEEN_DP,
        vertical = EIGHT_DP
      )
      .semantics {
        testTag = PAGE_ITEM_TESTING_TAG
        if (page.isSelected) selected = true
      },
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Decorative: the title is next to the icon, and the row tells accessibility
    // services that it is selected.
    if (page.isSelected) {
      Image(
        painter = painterResource(id = R.drawable.ic_check_circle_blue_24dp),
        contentDescription = null,
        modifier = Modifier
          .size(PAGE_LIST_ITEM_FAVICON_SIZE)
          .testTag("$PAGE_ITEM_ICON_TESTING_TAG$index")
      )
    } else {
      BookIcon(
        iconSource = page.favicon,
        size = PAGE_LIST_ITEM_FAVICON_SIZE,
        testTag = "$PAGE_ITEM_ICON_TESTING_TAG$index"
      )
    }

    Spacer(modifier = Modifier.width(SIXTEEN_DP))

    Text(
      text = page.title,
      style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier
        .weight(1f)
        .testTag("$PAGE_ITEM_TITLE_TESTING_TAG$index"),
      maxLines = ONE,
      overflow = TextOverflow.Ellipsis
    )
  }
}
