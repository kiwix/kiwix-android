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

package org.kiwix.kiwixmobile.zim_manager.library_view.adapter

import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.KiwixTag.Companion.YesNoValueTag.DetailsTag
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.KiwixTag.Companion.YesNoValueTag.FtIndexTag
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.KiwixTag.Companion.YesNoValueTag.PicturesTag
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.KiwixTag.Companion.YesNoValueTag.VideoTag
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.KiwixTag.TagValue.NO
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.KiwixTag.TagValue.YES

sealed class KiwixTag {
  companion object {
    fun from(tagString: String?) = tagString?.split(";")
      ?.map { tags ->
        val split = tags.split(":")
        val value = split.getOrNull(1)
        when (val tag = split[0]) {
          "_ftindex" -> FtIndexTag(value!!)
          "_pictures" -> PicturesTag(value!!)
          "_video" -> VideoTag(value!!)
          "_details" -> DetailsTag(value!!)
          "_category" -> CategoryTag(value!!)
          else -> value?.let { ArbitraryTag(tag, it) } ?: TagOnly(tag)
        }
      } ?: emptyList()

    data class CategoryTag(val categoryValue: String) : KiwixTag()
    data class ArbitraryTag(val tag: String, val value: String) : KiwixTag()
    data class TagOnly(val tag: String) : KiwixTag()

    sealed class YesNoValueTag(
      _value: String,
      val value: TagValue = if (_value == "yes") YES else NO
    ) : KiwixTag() {
      class FtIndexTag(value: String) : YesNoValueTag(value)
      class PicturesTag(value: String) : YesNoValueTag(value)
      class VideoTag(value: String) : YesNoValueTag(value)
      class DetailsTag(value: String) : YesNoValueTag(value)
    }
  }

  enum class TagValue {
    YES,
    NO
  }
}
