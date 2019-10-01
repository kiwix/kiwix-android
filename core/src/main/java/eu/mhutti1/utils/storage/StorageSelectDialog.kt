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

package eu.mhutti1.utils.storage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.storage_select_dialog.device_list
import kotlinx.android.synthetic.main.storage_select_dialog.title
import org.kiwix.kiwixmobile.core.KiwixApplication
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.utils.StyleUtils
import javax.inject.Inject

class StorageSelectDialog : DialogFragment() {

  private var onSelectAction: ((StorageDevice) -> Unit)? = null
  private var adapter: StorageSelectArrayAdapter? = null
  private var aTitle: String? = null

  @Inject lateinit var storageCalculator: StorageCalculator

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
    KiwixApplication.getApplicationComponent().inject(this)
    title.text = aTitle
    Flowable.fromCallable { StorageDeviceUtils.getWritableStorage(activity!!) }
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(
        {
          adapter = StorageSelectArrayAdapter(activity!!, it, storageCalculator)
          device_list.adapter = adapter
        },
        Throwable::printStackTrace
      )

    device_list.onItemClickListener = OnItemClickListener { _, _, position, _ ->
      onSelectAction?.invoke(adapter!!.getItem(position)!!)
      dismiss()
    }
  }

  override fun show(fm: FragmentManager, text: String) {
    aTitle = text
    super.show(fm, text)
  }

  fun setOnSelectListener(onSelectAction: (StorageDevice) -> Unit) {
    this.onSelectAction = onSelectAction
  }
}
