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

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.ui.components.BookIcon
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FOUR_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWELVE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWENTY_EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWO_DP
import org.kiwix.kiwixmobile.core.zim_manager.Byte
import org.kiwix.kiwixmobile.core.zim_manager.KiwixTag
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.HumanReadableCount
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode
import org.kiwix.kiwixmobile.ui.BookItemScreen.BOOK_ARTICLE_COUNT_TEST_TAG
import org.kiwix.kiwixmobile.ui.BookItemScreen.BOOK_DESCRIPTION_TEST_TAG
import org.kiwix.kiwixmobile.ui.BookItemScreen.BOOK_ITEM_CHECKBOX_TESTING_TAG
import org.kiwix.kiwixmobile.ui.BookItemScreen.BOOK_ITEM_TESTING_TAG
import java.text.SimpleDateFormat
import java.util.Locale
import org.kiwix.kiwixmobile.core.R as CoreR

@Composable
fun LibraryCard(
  modifier: Modifier = Modifier,
  isSelected: Boolean = false,
  content: @Composable () -> Unit
) {
  val containerColor = if (isSelected) {
    MaterialTheme.colorScheme.primaryContainer
  } else {
    MaterialTheme.colorScheme.surfaceContainer
  }
  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = FOUR_DP, vertical = TWO_DP),
    shape = RoundedCornerShape(TWO_DP),
    elevation = CardDefaults.elevatedCardElevation(TWO_DP),
    colors = CardDefaults.cardColors(containerColor = containerColor)
  ) {
    content()
  }
}

@Composable
fun BookItem(
  index: Int,
  bookOnDisk: BookOnDisk,
  onClick: ((BookOnDisk) -> Unit)? = null,
  onLongClick: ((BookOnDisk) -> Unit)? = null,
  onMultiSelect: ((BookOnDisk) -> Unit)? = null,
  selectionMode: SelectionMode = SelectionMode.NORMAL,
) {
  val isSelected = bookOnDisk.isSelected
  KiwixTheme {
    LibraryCard(
      isSelected = isSelected,
      modifier = Modifier
        .combinedClickable(
          onClick = {
            when (selectionMode) {
              SelectionMode.MULTI -> onMultiSelect?.invoke(bookOnDisk)
              SelectionMode.NORMAL -> onClick?.invoke(bookOnDisk)
            }
          },
          onLongClick = {
            when (selectionMode) {
              SelectionMode.MULTI -> onMultiSelect?.invoke(bookOnDisk)
              SelectionMode.NORMAL -> onLongClick?.invoke(bookOnDisk)
            }
          }
        )
        .testTag(BOOK_ITEM_TESTING_TAG)
        .semantics { selected = isSelected }
    ) {
      BookContent(
        book = bookOnDisk.book,
        tags = bookOnDisk.tags,
        isOnlineLibrary = false,
        trailingContent = if (isSelected) {
          { BookSelectedIcon(index = index) }
        } else {
          null
        }
      )
    }
  }
}

@Suppress("ComposableLambdaParameterNaming")
@Composable
fun BookContent(
  book: LibkiwixBook,
  tags: List<KiwixTag>,
  isOnlineLibrary: Boolean,
  modifier: Modifier = Modifier,
  sizeModifier: Modifier = Modifier,
  trailingContent: (@Composable () -> Unit)? = null,
) {
  Row(
    modifier = modifier
      .padding(start = SIXTEEN_DP, top = SIXTEEN_DP, end = SIXTEEN_DP, bottom = TWELVE_DP)
      .fillMaxWidth(),
    verticalAlignment = Alignment.Top
  ) {
    BookIconAndSize(
      iconSource = book.favicon,
      size = book.size,
      isOnlineLibrary = isOnlineLibrary,
      sizeModifier = sizeModifier
    )

    BookDetails(
      modifier = Modifier.weight(1f),
      title = book.title,
      date = book.date,
      description = book.description.orEmpty(),
      tags = tags,
      articleCount = book.articleCount,
      mediaCount = book.mediaCount
    )

    if (trailingContent != null) {
      Box(
        modifier = Modifier
          .align(Alignment.CenterVertically)
          .padding(start = SIXTEEN_DP),
        contentAlignment = Alignment.Center
      ) {
        trailingContent()
      }
    }
  }
}

@Composable
fun BookIconAndSize(
  iconSource: String,
  size: String,
  isOnlineLibrary: Boolean,
  modifier: Modifier = Modifier,
  sizeModifier: Modifier = Modifier
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier.padding(end = SIXTEEN_DP)
  ) {
    BookIcon(iconSource = iconSource, isOnlineLibrary = isOnlineLibrary)
    Spacer(modifier = Modifier.height(FOUR_DP))
    BookSize(
      size = Byte(size).humanReadable,
      style = MaterialTheme.typography.labelSmall,
      textAlign = TextAlign.Center,
      color = Color.Unspecified,
      modifier = sizeModifier
    )
  }
}

/**
 * Shows that the book is selected. The icon is decorative: the card tells accessibility
 * services that it is selected, and a tap on the icon goes to the card.
 */
@Composable
private fun BookSelectedIcon(
  index: Int,
  modifier: Modifier = Modifier
) {
  Image(
    painter = painterResource(id = CoreR.drawable.ic_check_circle_blue_24dp),
    contentDescription = null,
    modifier = modifier
      .size(TWENTY_EIGHT_DP)
      .testTag("$BOOK_ITEM_CHECKBOX_TESTING_TAG$index")
      .clip(CircleShape)
  )
}

@Composable
fun BookDetails(
  modifier: Modifier = Modifier,
  title: String,
  date: String,
  description: String,
  tags: List<KiwixTag>,
  articleCount: String? = null,
  mediaCount: String? = null
) {
  Column(modifier = modifier) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      BookAnnotatedTitle(
        title = title,
        date = date,
        modifier = Modifier.weight(1f)
      )
    }
    Spacer(modifier = Modifier.height(FOUR_DP))
    BookDescription(
      bookDescription = description,
      style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
      color = Color.Unspecified,
      minLines = 2
    )
    Spacer(modifier = Modifier.height(FOUR_DP))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      TagsView(tags)
      Row(
        modifier = Modifier
          .weight(1f, fill = false)
          .padding(start = SIXTEEN_DP),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        BookArticleCount(
          articleCount = articleCount,
          mediaCount = mediaCount
        )
      }
    }
  }
}

@Composable
private fun BookAnnotatedTitle(
  title: String,
  date: String,
  modifier: Modifier = Modifier
) {
  val titleStyle = MaterialTheme.typography.titleSmall.copy(
    fontWeight = FontWeight.SemiBold,
    color = MaterialTheme.colorScheme.onSurface
  )
  val dateStyle = MaterialTheme.typography.labelSmall.copy(
    fontWeight = FontWeight.Medium,
    baselineShift = BaselineShift.Superscript,
    color = MaterialTheme.colorScheme.onSurfaceVariant
  )
  val formattedDate = formatBookDate(date)
  val annotatedTitle = buildAnnotatedString {
    append(title)
    if (formattedDate.isNotEmpty()) {
      append(" ")
      withStyle(dateStyle.toSpanStyle()) {
        append(formattedDate)
      }
    }
  }
  Text(
    text = annotatedTitle,
    style = titleStyle,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
    modifier = modifier
  )
}

@Composable
fun BookArticleCount(articleCount: String?, mediaCount: String?) {
  val context = LocalContext.current
  val articlesStr =
    HumanReadableCount(articleCount.orEmpty())
      .toHumanReadable(context, CoreR.string.pagesCount)
  val mediasStr =
    HumanReadableCount(mediaCount.orEmpty())
      .toHumanReadable(context, CoreR.string.mediaCount)

  val combined = listOf(articlesStr, mediasStr).filter { it.isNotEmpty() }.joinToString(" • ")

  Text(
    text = combined,
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.testTag(BOOK_ARTICLE_COUNT_TEST_TAG)
  )
}

@Composable
fun BookSize(
  size: String,
  modifier: Modifier = Modifier,
  style: TextStyle = MaterialTheme.typography.bodyMedium,
  color: Color = MaterialTheme.colorScheme.onTertiary,
  textAlign: TextAlign? = null
) {
  Text(
    text = size,
    style = style,
    color = color,
    textAlign = textAlign,
    modifier = modifier
  )
}

@Composable
fun BookTitle(
  title: String,
  modifier: Modifier = Modifier,
  style: TextStyle = MaterialTheme.typography.titleSmall.copy(
    fontWeight = FontWeight.SemiBold,
    color = MaterialTheme.colorScheme.onSurface
  )
) {
  Text(
    text = title,
    style = style,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
    modifier = modifier
  )
}

@Composable
fun BookDescription(
  bookDescription: String,
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
    modifier = Modifier.testTag(BOOK_DESCRIPTION_TEST_TAG)
  )
}

private const val INPUT_YEAR_MONTH_FORMAT = "yyyy-MM"
private const val OUTPUT_YEAR_MONTH_FORMAT = "MM-yyyy"

fun formatBookDate(rawDate: String): String = runCatching {
  val date = SimpleDateFormat(INPUT_YEAR_MONTH_FORMAT, Locale.getDefault())
    .parse(rawDate.take(INPUT_YEAR_MONTH_FORMAT.length))
  SimpleDateFormat(OUTPUT_YEAR_MONTH_FORMAT, Locale.getDefault()).format(date!!)
}.getOrDefault(rawDate)

object BookItemScreen {
  const val BOOK_ARTICLE_COUNT_TEST_TAG = "bookArticleCountTestingTag"
  const val BOOK_DESCRIPTION_TEST_TAG = "bookDescriptionTestingTag"
  const val BOOK_ITEM_CHECKBOX_TESTING_TAG = "bookItemCheckboxTestingTag"
  const val BOOK_ITEM_TESTING_TAG = "bookItemTestingTag"
}
