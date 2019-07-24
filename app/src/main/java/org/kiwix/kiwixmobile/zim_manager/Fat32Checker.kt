/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.zim_manager

import android.Manifest.permission
import android.content.pm.PackageManager
import android.os.FileObserver
import android.util.Log
import androidx.core.content.ContextCompat
import io.reactivex.Flowable
import io.reactivex.functions.Function3
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.KiwixApplication
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker.FileSystemState.CanWrite4GbFile
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker.FileSystemState.NotEnoughSpaceFor4GbFile
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

class Fat32Checker @Inject constructor(sharedPreferenceUtil: SharedPreferenceUtil) {
  private val _fileSystemStates: BehaviorProcessor<FileSystemState> = BehaviorProcessor.create()
  val fileSystemStates: Flowable<FileSystemState> = _fileSystemStates.distinctUntilChanged()
  var fileObserver: FileObserver? = null
  private val requestCheckSystemFileType = BehaviorProcessor.createDefault(Unit)

  init {
    Flowable.combineLatest(
      sharedPreferenceUtil.prefStorages.distinctUntilChanged(),
      requestCheckSystemFileType,
      pollForExternalStoragePermissionGranted(),
      Function3 { storage: String, _: Unit, _: Boolean -> storage }
    )
      .observeOn(Schedulers.io())
      .subscribe(
        {
          val systemState = toFileSystemState(it)
          _fileSystemStates.onNext(systemState)
          fileObserver = if (systemState == NotEnoughSpaceFor4GbFile) fileObserver(it) else null
        },
        Throwable::printStackTrace
      )
  }

  private fun pollForExternalStoragePermissionGranted() =
    Flowable.interval(1, SECONDS)
      .map {
        ContextCompat.checkSelfPermission(
          KiwixApplication.getInstance(), permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
      }
      .filter { it }
      .take(1)

  private fun fileObserver(it: String?): FileObserver {
    return object : FileObserver(it, MOVED_FROM or DELETE) {
      override fun onEvent(
        event: Int,
        path: String?
      ) {
        requestCheckSystemFileType.onNext(Unit)
      }
    }.apply { startWatching() }
  }

  private fun toFileSystemState(it: String) =
    when {
      File(it).freeSpace > FOUR_GIGABYTES_IN_BYTES ->
        if (canCreate4GbFile(it)) CanWrite4GbFile
        else CannotWrite4GbFile
      else -> NotEnoughSpaceFor4GbFile
    }

  private fun canCreate4GbFile(storage: String): Boolean {
    val path = "$storage/large_file_test.txt"
    File(path).delete()
    try {
      RandomAccessFile(path, "rw").use {
        it.setLength(FOUR_GIGABYTES_IN_BYTES)
        return@canCreate4GbFile true
      }
    } catch (e: Exception) {
      e.printStackTrace()
      Log.d("Fat32Checker", e.message)
      return false
    } finally {
      File(path).delete()
    }
  }

  companion object {
    const val FOUR_GIGABYTES_IN_BYTES = 4L * 1024L * 1024L * 1024L
    const val FOUR_GIGABYTES_IN_KILOBYTES = 4L * 1024L * 1024L
  }

  sealed class FileSystemState {
    object NotEnoughSpaceFor4GbFile : FileSystemState()
    object CanWrite4GbFile : FileSystemState()
    object CannotWrite4GbFile : FileSystemState()
  }
}
