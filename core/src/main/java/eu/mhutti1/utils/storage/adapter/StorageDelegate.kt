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

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import eu.mhutti1.utils.storage.StorageDevice
import org.kiwix.kiwixmobile.core.R.layout
import org.kiwix.kiwixmobile.core.base.adapter.AdapterDelegate
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions.inflate
import org.kiwix.kiwixmobile.core.settings.StorageCalculator

class StorageDelegate(
  private val storageCalculator: StorageCalculator,
  private val onClickAction: (StorageDevice) -> Unit
) : AdapterDelegate<StorageDevice> {
  override fun createViewHolder(parent: ViewGroup): ViewHolder =
    StorageViewHolder(
      parent.inflate(layout.device_item, false),
      storageCalculator,
      onClickAction
    )

  override fun bind(viewHolder: ViewHolder, itemToBind: StorageDevice) {
    (viewHolder as eu.mhutti1.utils.storage.adapter.StorageViewHolder).bind(itemToBind)
  }

  override fun isFor(item: StorageDevice) = true
}
