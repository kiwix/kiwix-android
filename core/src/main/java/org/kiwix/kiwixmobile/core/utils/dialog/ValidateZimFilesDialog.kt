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

package org.kiwix.kiwixmobile.core.utils.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel.ValidateZimItemState
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel.ValidationStatus.Failed
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel.ValidationStatus.InProgress
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel.ValidationStatus.Pending
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel.ValidationStatus.Success
import org.kiwix.kiwixmobile.core.settings.DIALOG_LIST_MAX_HEIGHT_RATIO
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.models.toPainter
import org.kiwix.kiwixmobile.core.ui.theme.StartServerGreen
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DEFAULT_TEXT_ALPHA
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DIALOG_DEFAULT_PADDING_FOR_CONTENT
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FIVE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.ONE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIX_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.VALIDATION_BOOK_ICON_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.VALIDATION_BOOK_TITLE_TEXT

@Composable
fun ValidateZimDialog(
  items: List<ValidateZimItemState>,
  confirmButtonText: Int,
  onConfirmButtonClick: () -> Unit,
  cancelButtonText: Int? = null,
  modifier: Modifier,
  startEndPaddingForConfirmButton: Dp = DIALOG_DEFAULT_PADDING_FOR_CONTENT,
  onCancelButtonClick: (() -> Unit)? = null
) {
  BoxWithConstraints {
    val listMaxHeight = this.maxHeight * DIALOG_LIST_MAX_HEIGHT_RATIO
    Column(modifier = Modifier.fillMaxWidth()) {
      Column(modifier) {
        DialogTitle(getTitle(items.size == 1))
        ValidateZimList(items = items, maxHeight = listMaxHeight)
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        cancelButtonText?.let {
          DialogDismissButton(it, onCancelButtonClick, null)
        }
        DialogConfirmButton(
          stringResource(confirmButtonText),
          onConfirmButtonClick,
          startEndPadding = startEndPaddingForConfirmButton,
          null
        )
      }
    }
  }
}

@Composable
private fun ValidateZimList(items: List<ValidateZimItemState>, maxHeight: Dp) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(max = maxHeight)
      .verticalScroll(rememberScrollState())
  ) {
    items.forEachIndexed { index, item ->
      ValidateZimItemRow(index, item)
    }
  }
}

@Composable
private fun getTitle(isSingleFile: Boolean) = if (isSingleFile) {
  R.string.validating_zim_file
} else {
  R.string.validating_zim_files
}

@Composable
private fun ValidateZimItemRow(index: Int, item: ValidateZimItemState) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = SIX_DP),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = item.book.book.title,
      fontSize = VALIDATION_BOOK_TITLE_TEXT,
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = FIVE_DP, vertical = ONE_DP),
      color = MaterialTheme.colorScheme.onSurface.copy(alpha = DEFAULT_TEXT_ALPHA)
    )

    val modifier = Modifier
      .size(VALIDATION_BOOK_ICON_SIZE)
      .padding(horizontal = FIVE_DP, vertical = ONE_DP)
    when (item.status) {
      InProgress -> {
        ContentLoadingProgressBar(modifier)
      }

      Pending,
      Success,
      is Failed -> {
        val iconItem = when (item.status) {
          Pending -> IconItem.Drawable(R.drawable.ic_baseline_wait_24px) to LocalContentColor.current
          Success -> IconItem.Vector(Icons.Default.CheckCircle) to StartServerGreen
          is Failed -> IconItem.Drawable(R.drawable.ic_baseline_error_24px) to Color.Red
          else -> error("Unhandled status: ${item.status}")
        }

        Icon(
          painter = iconItem.first.toPainter(),
          contentDescription = stringResource(R.string.status) + index,
          modifier = modifier,
          tint = iconItem.second
        )
      }
    }
  }
}
