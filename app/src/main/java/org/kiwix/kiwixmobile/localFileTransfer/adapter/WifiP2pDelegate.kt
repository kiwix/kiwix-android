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

package org.kiwix.kiwixmobile.localFileTransfer.adapter

import android.net.wifi.p2p.WifiP2pDevice
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.kiwix.kiwixmobile.core.base.adapter.AdapterDelegate
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions.viewBinding
import org.kiwix.kiwixmobile.databinding.RowPeerDeviceBinding

class WifiP2pDelegate(private val onItemClickAction: (WifiP2pDevice) -> Unit) :
  AdapterDelegate<WifiP2pDevice> {
  override fun createViewHolder(parent: ViewGroup): ViewHolder =
    WifiP2pViewHolder(
      parent.viewBinding(RowPeerDeviceBinding::inflate, false),
      onItemClickAction
    )

  override fun bind(viewHolder: ViewHolder, itemToBind: WifiP2pDevice) {
    (viewHolder as WifiP2pViewHolder).bind(itemToBind)
  }

  override fun isFor(item: WifiP2pDevice) = true
}
