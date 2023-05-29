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
import eu.mhutti1.utils.storage.StorageDevice
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.core.databinding.DeviceItemBinding
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

@SuppressLint("SetTextI18n")
internal class StorageViewHolder(
  private val deviceItemBinding: DeviceItemBinding,
  private val storageCalculator: StorageCalculator,
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val onClickAction: (StorageDevice) -> Unit
) : BaseViewHolder<StorageDevice>(deviceItemBinding.root) {

  override fun bind(item: StorageDevice) {
    deviceItemBinding.fileName.setText(
      if (item.isInternal) R.string.internal_storage
      else R.string.external_storage
    )

    if (adapterPosition == sharedPreferenceUtil.storagePosition) {
      deviceItemBinding.fileName.isChecked = true
    }
    deviceItemBinding.fileSize.text = storageCalculator.calculateAvailableSpace(item.file) + " / " +
      storageCalculator.calculateTotalSpace(item.file) + "  "
    deviceItemBinding.clickOverlay.setOnClickListener {
      onClickAction.invoke(item)
    }
  }
}
