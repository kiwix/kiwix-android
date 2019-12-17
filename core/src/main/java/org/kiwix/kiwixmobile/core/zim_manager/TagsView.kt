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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.android.synthetic.main.tag_content.view.tag_picture
import kotlinx.android.synthetic.main.tag_content.view.tag_short_text
import kotlinx.android.synthetic.main.tag_content.view.tag_text_only
import kotlinx.android.synthetic.main.tag_content.view.tag_video
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions.inflate
import org.kiwix.kiwixmobile.core.zim_manager.KiwixTag.Companion.YesNoValueTag
import org.kiwix.kiwixmobile.core.zim_manager.KiwixTag.Companion.YesNoValueTag.DetailsTag
import org.kiwix.kiwixmobile.core.zim_manager.KiwixTag.Companion.YesNoValueTag.PicturesTag
import org.kiwix.kiwixmobile.core.zim_manager.KiwixTag.Companion.YesNoValueTag.VideoTag
import org.kiwix.kiwixmobile.core.zim_manager.KiwixTag.TagValue.YES

class TagsView(context: Context, attrs: AttributeSet) : ChipGroup(context, attrs) {

  init {
    inflate(R.layout.tag_content, true)
  }

  fun render(tags: List<KiwixTag>) {
    val pictureTagIsSet = tags.isSet<PicturesTag>()
    val videoTagIsSet = tags.isSet<VideoTag>()
    tag_picture.selectBy(pictureTagIsSet)
    tag_video.selectBy(videoTagIsSet)
    tag_text_only.selectBy(!pictureTagIsSet && !videoTagIsSet)
    tag_short_text.selectBy(!tags.isSet<DetailsTag>())
  }

  private inline fun <reified T : YesNoValueTag> List<KiwixTag>.isSet() =
    filterIsInstance<T>().getOrNull(0)?.value == YES

  private fun Chip.selectBy(isTagSet: Boolean) {
    isChecked = isTagSet
    isEnabled = isTagSet
  }
}
