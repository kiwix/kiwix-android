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
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import eu.mhutti1.utils.storage.StorageDevice
import eu.mhutti1.utils.storage.StorageDeviceUtils
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.core.R
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

  override fun setStorage() {
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
        setPathAndTitleForStorage(
          buildStoragePathAndTitle(
            storageDevice,
            index,
            selectedStoragePosition,
            sharedPreferenceUtil
          )
        )
        setFreeSpace(getFreeSpaceText(storageDevice))
        setUsedSpace(getUsedSpaceText(storageDevice))
        setProgress(calculateUsedPercentage(storageDevice))
      }
    }
  }

  private fun getFreeSpaceText(storageDevice: StorageDevice): String {
    val freeSpace = storageCalculator?.calculateAvailableSpace(storageDevice.file)
    return getString(R.string.pref_free_storage, freeSpace)
  }

  private fun getUsedSpaceText(storageDevice: StorageDevice): String {
    val usedSpace = storageCalculator?.calculateUsedSpace(storageDevice.file)
    return getString(R.string.pref_storage_used, usedSpace)
  }

  private fun buildStoragePathAndTitle(
    storageDevice: StorageDevice,
    index: Int,
    selectedStoragePosition: Int,
    sharedPreferenceUtil: SharedPreferenceUtil
  ): String {
    val storageName = if (storageDevice.isInternal) {
      getString(R.string.internal_storage)
    } else {
      getString(R.string.external_storage)
    }
    val storagePath = if (index == selectedStoragePosition) {
      sharedPreferenceUtil.prefStorage
    } else {
      getStoragePathForNonSelectedStorage(storageDevice, sharedPreferenceUtil)
    }
    val totalSpace = storageCalculator?.calculateTotalSpace(storageDevice.file)
    return "$storageName - $totalSpace\n$storagePath/Kiwix"
  }

  private fun getStoragePathForNonSelectedStorage(
    storageDevice: StorageDevice,
    sharedPreferenceUtil: SharedPreferenceUtil
  ): String =
    if (storageDevice.isInternal) {
      sharedPreferenceUtil.getPublicDirectoryPath(storageDevice.name)
    } else {
      storageDevice.name
    }

  @Suppress("MagicNumber")
  private fun calculateUsedPercentage(storageDevice: StorageDevice): Int {
    val totalSpace = storageCalculator?.totalBytes(storageDevice.file) ?: 1
    val availableSpace = storageCalculator?.availableBytes(storageDevice.file) ?: 0
    val usedSpace = totalSpace - availableSpace
    return (usedSpace.toDouble() / totalSpace * 100).toInt()
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
    super.onDestroyView()
  }

  companion object {
    const val PREF_MANAGE_EXTERNAL_STORAGE_PERMISSION =
      "pref_manage_external_storage"
  }
}
