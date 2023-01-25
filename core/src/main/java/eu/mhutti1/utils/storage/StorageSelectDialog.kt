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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import eu.mhutti1.utils.storage.adapter.StorageAdapter
import eu.mhutti1.utils.storage.adapter.StorageDelegate
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.databinding.StorageSelectDialogBinding
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

class StorageSelectDialog : DialogFragment() {
  var onSelectAction: ((StorageDevice) -> Unit)? = null

  @Inject lateinit var storageCalculator: StorageCalculator
  @Inject lateinit var sharedPreferenceUtil: SharedPreferenceUtil
  private var aTitle: String? = null
  private var storageSelectDialogViewBinding: StorageSelectDialogBinding? = null
  private var storageDisposable: Disposable? = null

  private val storageAdapter: StorageAdapter by lazy {
    StorageAdapter(
      StorageDelegate(storageCalculator, sharedPreferenceUtil) {
        onSelectAction?.invoke(it)
        dismiss()
      }
    )
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
    storageSelectDialogViewBinding?.title?.text = aTitle
    storageSelectDialogViewBinding?.deviceList?.run {
      adapter = storageAdapter
      layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
      setHasFixedSize(true)
    }

    storageDisposable =
      Flowable.fromCallable { StorageDeviceUtils.getWritableStorage(requireActivity()) }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
          { storageAdapter.items = it },
          Throwable::printStackTrace
        )
  }

  override fun show(fm: FragmentManager, text: String?) {
    aTitle = text
    super.show(fm, text)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    storageDisposable?.dispose()
    storageSelectDialogViewBinding = null
  }
}
