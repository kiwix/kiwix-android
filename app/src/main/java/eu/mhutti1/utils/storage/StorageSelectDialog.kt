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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.storage_select_dialog.device_list
import kotlinx.android.synthetic.main.storage_select_dialog.title
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.utils.StyleUtils

class StorageSelectDialog : DialogFragment() {

  private var onSelectAction: ((StorageDevice) -> Unit)? = null
  private var mAdapter: StorageSelectArrayAdapter? = null

  private var mTitle: String? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    setStyle(STYLE_NORMAL, StyleUtils.dialogStyle())
    super.onCreate(savedInstanceState)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = inflater.inflate(R.layout.storage_select_dialog, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    title.text = mTitle
    mAdapter = StorageSelectArrayAdapter(
      activity!!,
      StorageDeviceUtils.getStorageDevices(activity!!, true)
    )
    device_list.adapter = mAdapter
    device_list.onItemClickListener = OnItemClickListener { _, _, position, _ ->
      onSelectAction?.invoke(mAdapter!!.getItem(position)!!)
      dismiss()
    }
  }

  override fun show(fm: FragmentManager, text: String) {
    mTitle = text
    super.show(fm, text)
  }

  fun setOnSelectListener(onSelectAction: (StorageDevice) -> Unit) {
    this.onSelectAction = onSelectAction
  }
}
