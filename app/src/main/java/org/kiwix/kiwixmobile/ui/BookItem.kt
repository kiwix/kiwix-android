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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.kiwix.kiwixmobile.core.downloader.model.Base64String
import org.kiwix.kiwixmobile.core.downloader.model.toPainter
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.BOOK_ICON_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FIVE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FOUR_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWO_DP
import org.kiwix.kiwixmobile.core.zim_manager.Byte
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.ArticleCount
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode
import org.kiwix.kiwixmobile.ui.BookItemScreen.BOOK_ARTICLE_COUNT_TEST_TAG
import org.kiwix.kiwixmobile.ui.BookItemScreen.BOOK_ITEM_CHECKBOX_TESTING_TAG
import org.kiwix.kiwixmobile.ui.BookItemScreen.BOOK_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.ui.BookItemScreen.OFFLINE_IMAGE_TEST_TAG
import org.kiwix.kiwixmobile.ui.BookItemScreen.ONLINE_LIBRARY_IMAGE_TEST_TAG
import org.kiwix.kiwixmobile.core.R as CoreR

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
      shape = RoundedCornerShape(TWO_DP),
      elevation = CardDefaults.elevatedCardElevation(4.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
      BookContent(bookOnDisk, selectionMode, onMultiSelect, index)
    }
  }
}

@Composable
private fun BookContent(
  bookOnDisk: BookOnDisk,
  selectionMode: SelectionMode,
  onMultiSelect: ((BookOnDisk) -> Unit)?,
  index: Int,
) {
  Row(
    modifier = Modifier
      .padding(SIXTEEN_DP)
      .fillMaxWidth(),
    verticalAlignment = Alignment.Top
  ) {
    if (selectionMode == SelectionMode.MULTI) {
      BookCheckbox(bookOnDisk, onMultiSelect, index)
    }
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(end = SIXTEEN_DP)
    ) {
      BookIcon(bookOnDisk.book.favicon, isOnlineLibrary = false)
      Spacer(modifier = Modifier.height(FOUR_DP))
      BookSize(
        size = Byte(bookOnDisk.book.size).humanReadable,
        index = index,
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
        color = Color.Unspecified
      )
    }

    BookDetails(Modifier.weight(1f), bookOnDisk, index)
  }
}

@Composable
private fun BookCheckbox(
  bookOnDisk: BookOnDisk,
  onMultiSelect: ((BookOnDisk) -> Unit)?,
  index: Int
) {
  Checkbox(
    checked = bookOnDisk.isSelected,
    onCheckedChange = {
      onMultiSelect?.invoke(bookOnDisk)
    },
    modifier = Modifier
      .testTag("$BOOK_ITEM_CHECKBOX_TESTING_TAG$index")
      .semantics { contentDescription = "${bookOnDisk.isSelected}$index" }
  )
}

@Composable
fun BookIcon(iconSource: String, isOnlineLibrary: Boolean) {
  val modifier = Modifier.size(BOOK_ICON_SIZE)
  if (isOnlineLibrary) {
    AsyncImage(
      model = iconSource,
      contentDescription = stringResource(CoreR.string.fav_icon) + iconSource.hashCode(),
      modifier = modifier.testTag(ONLINE_LIBRARY_IMAGE_TEST_TAG),
      placeholder = painterResource(CoreR.drawable.default_zim_file_icon),
      error = painterResource(CoreR.drawable.default_zim_file_icon)
    )
  } else {
    Image(
      painter = Base64String(iconSource).toPainter(),
      contentDescription = stringResource(CoreR.string.fav_icon) + iconSource.hashCode(),
      modifier = modifier.testTag(OFFLINE_IMAGE_TEST_TAG)
    )
  }
}

@Composable
private fun BookDetails(modifier: Modifier, bookOnDisk: BookOnDisk, index: Int) {
  Column(modifier = modifier) {
    val titleStyle = MaterialTheme.typography.titleSmall
    val dateStyle = MaterialTheme.typography.labelSmall.copy(
      baselineShift = BaselineShift.Superscript
    )
    val annotatedTitle = buildAnnotatedString {
      append(bookOnDisk.book.title)
      if (bookOnDisk.book.date.isNotEmpty()) {
        append(" ")
        withStyle(dateStyle.toSpanStyle()) {
          append(bookOnDisk.book.date)
        }
      }
    }
    Text(
      text = annotatedTitle,
      style = titleStyle,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.semantics {
        contentDescription = "${bookOnDisk.book.title}${bookOnDisk.book.date}$index"
      }
    )
    Spacer(modifier = Modifier.height(FOUR_DP))
    BookDescription(
      bookDescription = bookOnDisk.book.description.orEmpty(),
      index = index,
      style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
      color = Color.Unspecified,
      minLines = 2
    )
    Spacer(modifier = Modifier.height(FOUR_DP))
    HorizontalDivider(
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
    Spacer(modifier = Modifier.height(FOUR_DP))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      TagsView(bookOnDisk.tags, modifier = Modifier.weight(1f), index = index)
      Row(modifier = Modifier.padding(start = SIXTEEN_DP)) {
        BookArticleCount(
          articleCount = bookOnDisk.book.articleCount,
          mediaCount = bookOnDisk.book.mediaCount,
          index = index
        )
      }
    }
  }
}

@Composable
private fun BookArticleCount(articleCount: String?, mediaCount: String?, index: Int) {
  val context = LocalContext.current
  val articlesStr =
    ArticleCount(articleCount.orEmpty())
      .toHumanReadable(context, CoreR.string.articleCount)
  val mediasStr =
    ArticleCount(mediaCount.orEmpty())
      .toHumanReadable(context, CoreR.string.mediaCount)

  val combined = listOf(articlesStr, mediasStr).filter { it.isNotEmpty() }.joinToString(" • ")

  Text(
    text = combined,
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier
      .testTag(BOOK_ARTICLE_COUNT_TEST_TAG)
      .semantics { contentDescription = "$combined$index" }
  )
}

@Composable
fun BookSize(
  size: String,
  modifier: Modifier = Modifier,
  index: Int,
  style: TextStyle = MaterialTheme.typography.bodyMedium,
  color: Color = MaterialTheme.colorScheme.onTertiary,
  textAlign: TextAlign? = null
) {
  Text(
    text = size,
    style = style,
    color = color,
    textAlign = textAlign,
    modifier = modifier.semantics { contentDescription = "$size$index" }
  )
}

@Composable
fun BookDate(
  date: String,
  index: Int,
  modifier: Modifier = Modifier,
  style: TextStyle = MaterialTheme.typography.bodyMedium,
  color: Color = MaterialTheme.colorScheme.onTertiary
) {
  Text(
    text = date,
    style = style,
    color = color,
    modifier = modifier.semantics { contentDescription = "$date$index" }
  )
}

@Composable
fun BookTitle(
  title: String,
  index: Int,
  modifier: Modifier = Modifier,
  style: TextStyle = MaterialTheme.typography.titleSmall
) {
  Text(
    text = title,
    style = style,
    modifier = modifier.semantics { contentDescription = "$title$index" }
  )
}

@Composable
fun BookDescription(
  bookDescription: String,
  index: Int,
  style: TextStyle = MaterialTheme.typography.bodyMedium,
  color: Color = MaterialTheme.colorScheme.onSecondary,
  minLines: Int = 1
) {
  Text(
    text = bookDescription,
    style = style,
    color = color,
    minLines = minLines,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
    modifier = Modifier.semantics { contentDescription = "$bookDescription$index" }
  )
}

object BookItemScreen {
  const val ONLINE_LIBRARY_IMAGE_TEST_TAG = "onlineLibraryImageTestingTag"
  const val OFFLINE_IMAGE_TEST_TAG = "localLibraryImageTestingTag"

  const val BOOK_ARTICLE_COUNT_TEST_TAG = "bookArticleCountTestingTag"
  const val BOOK_ITEM_CHECKBOX_TESTING_TAG = "bookItemCheckboxTestingTag"
  const val BOOK_ITEM_TESTING_TAG = "bookItemTestingTag"
}
