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
package org.kiwix.kiwixmobile.zim_manager

import android.os.FileObserver
import io.reactivex.Flowable
import io.reactivex.functions.BiFunction
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.schedulers.Schedulers
import java.io.File
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker.FileSystemState.CanWrite4GbFile
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker.FileSystemState.NotEnoughSpaceFor4GbFile
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker.FileSystemState.Unknown
import org.kiwix.kiwixmobile.zim_manager.FileSystemCapability.CANNOT_WRITE_4GB
import org.kiwix.kiwixmobile.zim_manager.FileSystemCapability.CAN_WRITE_4GB
import org.kiwix.kiwixmobile.zim_manager.FileSystemCapability.INCONCLUSIVE

class Fat32Checker constructor(
  sharedPreferenceUtil: SharedPreferenceUtil,
  private val fileSystemCheckers: List<FileSystemChecker>
) {
  val fileSystemStates: BehaviorProcessor<FileSystemState> = BehaviorProcessor.create()
  private var fileObserver: FileObserver? = null
  private val requestCheckSystemFileType = BehaviorProcessor.createDefault(Unit)

  init {
    Flowable.combineLatest(
      sharedPreferenceUtil.prefStorages
        .distinctUntilChanged()
        .doOnNext { fileSystemStates.offer(Unknown) },
      requestCheckSystemFileType,
      BiFunction { storage: String, _: Unit -> storage }
    )
      .observeOn(Schedulers.io())
      .subscribeOn(Schedulers.io())
      .subscribe(
        {
          val systemState = toFileSystemState(it)
          fileSystemStates.offer(systemState)
          fileObserver = if (systemState == NotEnoughSpaceFor4GbFile) fileObserver(it) else null
        },
        Throwable::printStackTrace
      )
  }

  private fun fileObserver(it: String?): FileObserver {
    return object : FileObserver(it, MOVED_FROM or DELETE) {
      override fun onEvent(event: Int, path: String?) {
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
    fileSystemCheckers.forEach {
      when (it.checkFilesystemSupports4GbFiles(storage)) {
        CAN_WRITE_4GB -> return@canCreate4GbFile true
        CANNOT_WRITE_4GB -> return@canCreate4GbFile false
        INCONCLUSIVE -> {
          /*do nothing*/
        }
      }
    }
    return false
  }

  companion object {
    const val FOUR_GIGABYTES_IN_BYTES = 4L * 1024L * 1024L * 1024L
    const val FOUR_GIGABYTES_IN_KILOBYTES = 4L * 1024L * 1024L
  }

  sealed class FileSystemState {
    object NotEnoughSpaceFor4GbFile : FileSystemState()
    object CanWrite4GbFile : FileSystemState()
    object CannotWrite4GbFile : FileSystemState()
    object Unknown : FileSystemState()
  }
}
