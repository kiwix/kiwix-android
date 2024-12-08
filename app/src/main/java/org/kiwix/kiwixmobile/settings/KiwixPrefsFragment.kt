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

package org.kiwix.kiwixmobile.settings

import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import eu.mhutti1.utils.storage.StorageDevice
import eu.mhutti1.utils.storage.StorageDeviceUtils
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.getFreeSpace
import org.kiwix.kiwixmobile.core.extensions.getUsedSpace
import org.kiwix.kiwixmobile.core.extensions.storagePathAndTitle
import org.kiwix.kiwixmobile.core.extensions.usedPercentage
import org.kiwix.kiwixmobile.core.navigateToSettings
import org.kiwix.kiwixmobile.core.settings.CorePrefsFragment
import org.kiwix.kiwixmobile.core.settings.StorageRadioButtonPreference
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.Companion.PREF_EXTERNAL_STORAGE
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.Companion.PREF_INTERNAL_STORAGE

class KiwixPrefsFragment : CorePrefsFragment() {
  private var storageDisposable: Disposable? = null
  private var storageDeviceList: List<StorageDevice> = listOf()

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    setUpLanguageChooser(SharedPreferenceUtil.PREF_LANG)
    setMangeExternalStoragePermission()
  }

  override suspend fun setStorage() {
    sharedPreferenceUtil?.let {
      if (storageDisposable?.isDisposed == false) {
        // update the storage when user switch to other storage.
        setUpStoragePreference(it)
      }
      storageDisposable =
        Flowable.fromCallable { StorageDeviceUtils.getWritableStorage(requireActivity()) }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(
            { storageList ->
              storageDeviceList = storageList
              showExternalPreferenceIfAvailable()
              setUpStoragePreference(it)
            },
            Throwable::printStackTrace
          )
    }
  }

  private fun setUpStoragePreference(sharedPreferenceUtil: SharedPreferenceUtil) {
    lifecycleScope.launch {
      storageDeviceList.forEachIndexed { index, storageDevice ->
        val preferenceKey = if (index == 0) PREF_INTERNAL_STORAGE else PREF_EXTERNAL_STORAGE
        val selectedStoragePosition = sharedPreferenceUtil.storagePosition
        val isChecked = selectedStoragePosition == index
        findPreference<StorageRadioButtonPreference>(preferenceKey)?.apply {
          this.isChecked = isChecked
          setOnPreferenceClickListener {
            onStorageDeviceSelected(storageDevice)
            true
          }
          storageCalculator?.let {
            setPathAndTitleForStorage(
              storageDevice.storagePathAndTitle(context, index, sharedPreferenceUtil, it)
            )
            setFreeSpace(storageDevice.getFreeSpace(context, it))
            setUsedSpace(storageDevice.getUsedSpace(context, it))
            setProgress(storageDevice.usedPercentage(it))
          }
        }
      }
    }
  }

  private fun showExternalPreferenceIfAvailable() {
    findPreference<StorageRadioButtonPreference>(PREF_EXTERNAL_STORAGE)?.isVisible =
      storageDeviceList.size > 1
  }

  private fun setMangeExternalStoragePermission() {
    val permissionPref = findPreference<Preference>(PREF_MANAGE_EXTERNAL_STORAGE_PERMISSION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
      sharedPreferenceUtil?.isPlayStoreBuild == false
    ) {
      showPermissionPreference()
      val externalStorageManager = Environment.isExternalStorageManager()
      if (externalStorageManager) {
        permissionPref?.setSummary(R.string.allowed)
      } else {
        permissionPref?.setSummary(R.string.not_allowed)
      }
      permissionPref?.onPreferenceClickListener =
        Preference.OnPreferenceClickListener {
          activity?.let(FragmentActivity::navigateToSettings)
          true
        }
    }
  }

  private fun showPermissionPreference() {
    val preferenceCategory = findPreference<PreferenceCategory>(
      PREF_PERMISSION
    )
    preferenceCategory?.isVisible = true
  }

  override fun onDestroyView() {
    storageDisposable?.dispose()
    storageDisposable = null
    super.onDestroyView()
  }

  companion object {
    const val PREF_MANAGE_EXTERNAL_STORAGE_PERMISSION =
      "pref_manage_external_storage"
  }
}
