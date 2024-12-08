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

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import eu.mhutti1.utils.storage.adapter.StorageAdapter
import eu.mhutti1.utils.storage.adapter.StorageDelegate
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.R.dimen
import org.kiwix.kiwixmobile.core.databinding.StorageSelectDialogBinding
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.isLandScapeMode
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.utils.DimenUtils.getWindowWidth
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

const val STORAGE_SELECT_STORAGE_TITLE_TEXTVIEW_SIZE = 16F

class StorageSelectDialog : DialogFragment() {
  var onSelectAction: ((StorageDevice) -> Unit)? = null
  var titleSize: Float? = null

  @Inject lateinit var storageCalculator: StorageCalculator
  @Inject lateinit var sharedPreferenceUtil: SharedPreferenceUtil
  private var aTitle: String? = null
  private var storageSelectDialogViewBinding: StorageSelectDialogBinding? = null
  private val storageDeviceList = arrayListOf<StorageDevice>()
  private var shouldShowCheckboxSelected: Boolean = true

  private val storageAdapter: StorageAdapter by lazy {
    StorageAdapter(
      StorageDelegate(
        storageCalculator,
        sharedPreferenceUtil,
        lifecycleScope,
        shouldShowCheckboxSelected,
      ) {
        onSelectAction?.invoke(it)
        dismiss()
      }
    )
  }

  fun setStorageDeviceList(storageDeviceList: List<StorageDevice>) {
    this.storageDeviceList.addAll(storageDeviceList)
  }

  fun setShouldShowCheckboxSelected(shouldShowCheckboxSelected: Boolean) {
    this.shouldShowCheckboxSelected = shouldShowCheckboxSelected
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    storageSelectDialogViewBinding = StorageSelectDialogBinding.inflate(inflater, container, false)
    return storageSelectDialogViewBinding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    CoreApp.coreComponent.inject(this)
    storageSelectDialogViewBinding?.title?.apply {
      text = aTitle
      titleSize?.let(::setTextSize)
    }
    storageSelectDialogViewBinding?.deviceList?.run {
      adapter = storageAdapter
      layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
      setHasFixedSize(true)
    }
    storageAdapter.items = storageDeviceList
  }

  override fun show(fm: FragmentManager, text: String?) {
    aTitle = text
    super.show(fm, text)
  }

  override fun onStart() {
    super.onStart()
    setStorageSelectDialogWidth()
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    setStorageSelectDialogWidth()
  }

  @Suppress("MagicNumber")
  private fun setStorageSelectDialogWidth() {
    val windowWidth = requireActivity().getWindowWidth()
    val maximumStorageSelectDialogWidth =
      requireActivity().resources.getDimensionPixelSize(dimen.maximum_donation_popup_width)

    val width =
      if (windowWidth > maximumStorageSelectDialogWidth || requireActivity().isLandScapeMode()) {
        maximumStorageSelectDialogWidth
      } else {
        (windowWidth * 0.9).toInt()
      }
    dialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    storageSelectDialogViewBinding = null
  }
}
