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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.zim_manager.KiwixTag
import org.kiwix.kiwixmobile.core.zim_manager.KiwixTag.Companion.YesNoValueTag
import org.kiwix.kiwixmobile.core.zim_manager.KiwixTag.Companion.YesNoValueTag.DetailsTag
import org.kiwix.kiwixmobile.core.zim_manager.KiwixTag.Companion.YesNoValueTag.PicturesTag
import org.kiwix.kiwixmobile.core.zim_manager.KiwixTag.Companion.YesNoValueTag.VideoTag
import org.kiwix.kiwixmobile.core.zim_manager.KiwixTag.TagValue.YES

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsView(tags: List<KiwixTag>, modifier: Modifier = Modifier, hasCode: Int) {
  FlowRow(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(EIGHT_DP)
  ) {
    if (tags.isYesOrNotDefined<PicturesTag>()) {
      val picture = stringResource(R.string.tag_pic)
      TagChip(text = picture, contentDescription = "$picture$hasCode")
    }
    if (tags.isYesOrNotDefined<VideoTag>()) {
      val video = stringResource(R.string.tag_vid)
      TagChip(text = video, contentDescription = "$video$hasCode")
    }
    val shortTextIsSelected = tags.isDefinedAndNo<DetailsTag>()
    if (tags.isDefinedAndNo<PicturesTag>() &&
      tags.isDefinedAndNo<VideoTag>() &&
      !shortTextIsSelected
    ) {
      val textOnly = stringResource(R.string.tag_text_only)
      TagChip(text = textOnly, contentDescription = "$textOnly$hasCode")
    }
    if (shortTextIsSelected) {
      val shortText = stringResource(R.string.tag_short_text)
      TagChip(text = shortText, contentDescription = "$shortText$hasCode")
    }
  }
}

@Composable
private fun TagChip(text: String, contentDescription: String) {
  val chipColors = SuggestionChipDefaults.suggestionChipColors(
    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
    disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f)
  )
  SuggestionChip(
    onClick = {},
    label = { Text(text) },
    enabled = false,
    shape = MaterialTheme.shapes.extraLarge,
    colors = chipColors,
    border = null,
    modifier = Modifier.semantics { this.contentDescription = contentDescription }
  )
}

private inline fun <reified T : YesNoValueTag> List<KiwixTag>.isYesOrNotDefined() =
  isYes<T>() || !isDefined<T>()

private inline fun <reified T : YesNoValueTag> List<KiwixTag>.isDefinedAndNo() =
  isDefined<T>() && !isYes<T>()

private inline fun <reified T : YesNoValueTag> List<KiwixTag>.isYes() =
  filterIsInstance<T>().getOrNull(0)?.value == YES

private inline fun <reified T : YesNoValueTag> List<KiwixTag>.isDefined() =
  filterIsInstance<T>().isNotEmpty()
