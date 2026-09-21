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

package org.kiwix.kiwixmobile.language.composables

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.utils.ComposeDimens
import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.kiwix.kiwixmobile.language.composables.LanguageListItem.LanguageItem

const val LANGUAGE_ITEM_RADIO_BUTTON_TESTING_TAG = "languageItemRadioButtonTestingTag"
const val MOVE_UP_TESTING_TAG = "moveUpTestingTag"
const val MOVE_DOWN_TESTING_TAG = "moveDownTestingTag"

@Suppress("LongParameterList")
@Composable
fun LanguageItemRow(
  context: Context,
  modifier: Modifier,
  item: LanguageItem,
  isFirst: Boolean = false,
  isLast: Boolean = false,
  onItemClick: (LanguageItem) -> Unit,
  onMoveUp: (LanguageItem) -> Unit = {},
  onMoveDown: (LanguageItem) -> Unit = {}
) {
  val language = item.language
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = ComposeDimens.SIXTEEN_DP),
    shape = itemShape(isFirst, isLast),
    color = MaterialTheme.colorScheme.surfaceVariant
  ) {
    Column {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = ComposeDimens.FIFTY_SIX_DP)
          .clickable { onItemClick(item) }
          .semantics {
            contentDescription = context.getString(R.string.select_language_content_description)
            testTag = "$LANGUAGE_ITEM_RADIO_BUTTON_TESTING_TAG${language.language}"
          }
          .padding(
            start = ComposeDimens.SIXTEEN_DP,
            end = ComposeDimens.SIXTEEN_DP,
            top = ComposeDimens.TWELVE_DP,
            bottom = ComposeDimens.TWELVE_DP
          ),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (item.isSelectedSection) {
          SelectedLanguageLeading(item, onMoveUp, onMoveDown)
        }
        LanguageTitles(language, context, Modifier.weight(1f))
        Text(
          text = pluralStringResource(
            R.plurals.book_count,
            language.occurencesOfLanguage,
            language.occurencesOfLanguage
          ),
          modifier = Modifier.padding(start = ComposeDimens.EIGHT_DP),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      if (!isLast) {
        HorizontalDivider(
          modifier = Modifier.padding(horizontal = ComposeDimens.SIXTEEN_DP),
          thickness = 0.5.dp,
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
      }
    }
  }
}

private fun itemShape(isFirst: Boolean, isLast: Boolean): Shape =
  when {
    isFirst && isLast -> RoundedCornerShape(ComposeDimens.MEDIUM_ROUND_SHAPE_SIZE)
    isFirst ->
      RoundedCornerShape(
        topStart = ComposeDimens.MEDIUM_ROUND_SHAPE_SIZE,
        topEnd = ComposeDimens.MEDIUM_ROUND_SHAPE_SIZE
      )
    isLast ->
      RoundedCornerShape(
        bottomStart = ComposeDimens.MEDIUM_ROUND_SHAPE_SIZE,
        bottomEnd = ComposeDimens.MEDIUM_ROUND_SHAPE_SIZE
      )
    else -> RectangleShape
  }

@Composable
private fun SelectedLanguageLeading(
  item: LanguageItem,
  onMoveUp: (LanguageItem) -> Unit,
  onMoveDown: (LanguageItem) -> Unit
) {
  val currentOnMoveUp by rememberUpdatedState(onMoveUp)
  val currentOnMoveDown by rememberUpdatedState(onMoveDown)
  val canMoveUp = item.canMoveUp
  val canMoveDown = item.canMoveDown
  val currentCanMoveUp by rememberUpdatedState(canMoveUp)
  val currentCanMoveDown by rememberUpdatedState(canMoveDown)
  val currentItem by rememberUpdatedState(item)
  val thresholdPx = with(LocalDensity.current) { ComposeDimens.TWENTY_FOUR_DP.toPx() }
  var dragAccumulator by remember { mutableFloatStateOf(0f) }

  val moveUpLabel = stringResource(R.string.move_up)
  val moveDownLabel = stringResource(R.string.move_down)

  Icon(
    imageVector = Icons.Default.Menu,
    contentDescription = stringResource(R.string.reorder_language),
    tint = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier
      .padding(end = ComposeDimens.TWELVE_DP)
      .reorderAccessibilityActions(
        canMoveUp = canMoveUp,
        canMoveDown = canMoveDown,
        moveUpLabel = moveUpLabel,
        moveDownLabel = moveDownLabel,
        onMoveUp = { currentOnMoveUp(item) },
        onMoveDown = { currentOnMoveDown(item) }
      )
      .pointerInput(item.id) {
        detectVerticalDragGestures(
          onDragStart = { dragAccumulator = 0f },
          onDragEnd = { dragAccumulator = 0f },
          onDragCancel = { dragAccumulator = 0f },
          onVerticalDrag = { change, dragAmount ->
            change.consume()
            dragAccumulator += dragAmount
            if (dragAccumulator <= -thresholdPx) {
              if (currentCanMoveUp) currentOnMoveUp(currentItem)
              dragAccumulator = 0f
            } else if (dragAccumulator >= thresholdPx) {
              if (currentCanMoveDown) currentOnMoveDown(currentItem)
              dragAccumulator = 0f
            }
          }
        )
      }
  )
  Text(
    text = "${item.rank}.",
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(end = ComposeDimens.EIGHT_DP)
  )
}

@Composable
private fun LanguageTitles(
  language: Language,
  context: Context,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier) {
    Text(
      text = language.language.ifEmpty { context.getString(R.string.all_languages) },
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurface
    )
    if (language.languageLocalized.isNotEmpty() &&
      language.languageLocalized != language.language
    ) {
      Text(
        text = language.languageLocalized,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Suppress("LongParameterList")
private fun Modifier.reorderAccessibilityActions(
  canMoveUp: Boolean,
  canMoveDown: Boolean,
  moveUpLabel: String,
  moveDownLabel: String,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit
): Modifier = semantics {
  customActions = buildList {
    if (canMoveUp) {
      add(
        CustomAccessibilityAction(moveUpLabel) {
          onMoveUp()
          true
        }
      )
    }
    if (canMoveDown) {
      add(
        CustomAccessibilityAction(moveDownLabel) {
          onMoveDown()
          true
        }
      )
    }
  }
}
