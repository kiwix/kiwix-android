/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.zim_manager

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import org.kiwix.kiwixmobile.core.databinding.TagContentBinding
import org.kiwix.kiwixmobile.core.zim_manager.KiwixTag.Companion.YesNoValueTag
import org.kiwix.kiwixmobile.core.zim_manager.KiwixTag.Companion.YesNoValueTag.DetailsTag
import org.kiwix.kiwixmobile.core.zim_manager.KiwixTag.Companion.YesNoValueTag.PicturesTag
import org.kiwix.kiwixmobile.core.zim_manager.KiwixTag.Companion.YesNoValueTag.VideoTag
import org.kiwix.kiwixmobile.core.zim_manager.KiwixTag.TagValue.YES

class TagsView(context: Context, attrs: AttributeSet) : ChipGroup(context, attrs) {
  private var tagContentBinding: TagContentBinding? = null

  init {
    tagContentBinding = TagContentBinding.inflate(LayoutInflater.from(context), this)
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    tagContentBinding = null
  }

  fun render(tags: List<KiwixTag>) {
    tagContentBinding?.tagPicture?.selectBy(tags.isYesOrNotDefined<PicturesTag>())
    tagContentBinding?.tagVideo?.selectBy(tags.isYesOrNotDefined<VideoTag>())
    val shortTextIsSelected = tags.isDefinedAndNo<DetailsTag>()
    tagContentBinding?.tagTextOnly?.selectBy(
      tags.isDefinedAndNo<PicturesTag>() &&
        tags.isDefinedAndNo<VideoTag>() &&
        !shortTextIsSelected
    )
    tagContentBinding?.tagShortText?.selectBy(shortTextIsSelected)
  }

  private inline fun <reified T : YesNoValueTag> List<KiwixTag>.isYesOrNotDefined() =
    isYes<T>() || !isDefined<T>()

  private inline fun <reified T : YesNoValueTag> List<KiwixTag>.isDefinedAndNo() =
    isDefined<T>() && !isYes<T>()

  private inline fun <reified T : YesNoValueTag> List<KiwixTag>.isYes() =
    filterIsInstance<T>().getOrNull(0)?.value == YES

  private inline fun <reified T : YesNoValueTag> List<KiwixTag>.isDefined() =
    filterIsInstance<T>().isNotEmpty()

  private fun Chip.selectBy(criteria: Boolean) {
    isChecked = criteria
    isEnabled = criteria
    visibility = if (criteria) VISIBLE else GONE
  }
}
