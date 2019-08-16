/*
 * Copyright 2016 Isaac Hutt <mhutti1@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package eu.mhutti1.utils.storage

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.device_item.file_name
import kotlinx.android.synthetic.main.device_item.file_size
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.R.string
import org.kiwix.kiwixmobile.extensions.inflate

internal class StorageSelectArrayAdapter(
  context: Context,
  devices: List<StorageDevice>
) : ArrayAdapter<StorageDevice>(context, 0, devices) {

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    var convertView = convertView

    val holder: ViewHolder
    if (convertView == null) {
      convertView = parent.inflate(R.layout.device_item, false)
      holder = ViewHolder(convertView)
      convertView.tag = holder
    } else {
      holder = convertView.tag as ViewHolder
    }
    holder.bind(getItem(position)!!)

    return convertView
  }

  @SuppressLint("SetTextI18n")
  internal inner class ViewHolder(override val containerView: View) : LayoutContainer {
    fun bind(device: StorageDevice) {
      file_name.setText(if (device.isInternal) string.internal_storage else string.external_storage)
      file_size.text = device.availableSpace + " / " + device.totalSize
    }
  }
}
