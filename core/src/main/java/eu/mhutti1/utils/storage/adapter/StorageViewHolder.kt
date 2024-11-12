/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package eu.mhutti1.utils.storage.adapter

import android.annotation.SuppressLint
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.view.View.VISIBLE
import eu.mhutti1.utils.storage.StorageDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.core.databinding.ItemStoragePreferenceBinding
import org.kiwix.kiwixmobile.core.downloader.downloadManager.ZERO
import org.kiwix.kiwixmobile.core.extensions.getFreeSpace
import org.kiwix.kiwixmobile.core.extensions.getUsedSpace
import org.kiwix.kiwixmobile.core.extensions.setToolTipWithContentDescription
import org.kiwix.kiwixmobile.core.extensions.storagePathAndTitle
import org.kiwix.kiwixmobile.core.extensions.usedPercentage
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

const val FREE_SPACE_TEXTVIEW_SIZE = 12F
const val STORAGE_TITLE_TEXTVIEW_SIZE = 15

@SuppressLint("SetTextI18n")
@Suppress("LongParameterList")
internal class StorageViewHolder(
  private val itemStoragePreferenceBinding: ItemStoragePreferenceBinding,
  private val storageCalculator: StorageCalculator,
  private val lifecycleScope: CoroutineScope,
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val shouldShowCheckboxSelected: Boolean,
  private val onClickAction: (StorageDevice) -> Unit
) : BaseViewHolder<StorageDevice>(itemStoragePreferenceBinding.root) {

  override fun bind(item: StorageDevice) {
    lifecycleScope.launch {
      with(itemStoragePreferenceBinding) {
        storagePathAndTitle.text =
          resizeStoragePathAndTitle(
            item.storagePathAndTitle(
              root.context,
              adapterPosition,
              sharedPreferenceUtil,
              storageCalculator
            )
          )

        radioButton.isChecked = shouldShowCheckboxSelected &&
          adapterPosition == sharedPreferenceUtil.storagePosition
        freeSpace.apply {
          text = item.getFreeSpace(root.context, storageCalculator)
          textSize = FREE_SPACE_TEXTVIEW_SIZE
        }
        usedSpace.apply {
          text = item.getUsedSpace(root.context, storageCalculator)
          textSize = FREE_SPACE_TEXTVIEW_SIZE
        }
        storageProgressBar.progress = item.usedPercentage(storageCalculator)
        clickOverlay.apply {
          visibility = VISIBLE
          setToolTipWithContentDescription(
            root.context.getString(
              R.string.storage_selection_dialog_accessibility_description
            )
          )
          setOnClickListener {
            onClickAction.invoke(item)
          }
        }
      }
    }
  }

  private fun resizeStoragePathAndTitle(storagePathAndTitle: String): CharSequence =
    SpannableStringBuilder(storagePathAndTitle).apply {
      setSpan(
        AbsoluteSizeSpan(STORAGE_TITLE_TEXTVIEW_SIZE, true),
        ZERO,
        storagePathAndTitle.indexOf('\n'),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
      )
      setSpan(
        AbsoluteSizeSpan(FREE_SPACE_TEXTVIEW_SIZE.toInt(), true),
        storagePathAndTitle.indexOf('\n') + 1,
        storagePathAndTitle.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
      )
    }
}
