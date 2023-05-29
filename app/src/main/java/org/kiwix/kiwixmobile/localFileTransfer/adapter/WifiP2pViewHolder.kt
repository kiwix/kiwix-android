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
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.databinding.RowPeerDeviceBinding

class WifiP2pViewHolder(
  private val rowPeerDeviceBinding: RowPeerDeviceBinding,
  private val onItemClickAction: (WifiP2pDevice) -> Unit
) : BaseViewHolder<WifiP2pDevice>(rowPeerDeviceBinding.root) {
  override fun bind(item: WifiP2pDevice) {
    rowPeerDeviceBinding.rowDeviceName.text = item.deviceName
    containerView.setOnClickListener {
      onItemClickAction.invoke(item)
    }
  }
}
