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
package org.kiwix.kiwixmobile.zimManager

import android.os.Build
import android.os.FileObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CanWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.NotEnoughSpaceFor4GbFile
import org.kiwix.kiwixmobile.zimManager.FileSystemCapability.CANNOT_WRITE_4GB
import org.kiwix.kiwixmobile.zimManager.FileSystemCapability.CAN_WRITE_4GB
import org.kiwix.kiwixmobile.zimManager.FileSystemCapability.INCONCLUSIVE
import java.io.File

class Fat32Checker constructor(
  sharedPreferenceUtil: SharedPreferenceUtil,
  private val fileSystemCheckers: List<FileSystemChecker>
) {
  private val _fileSystemStates =
    MutableStateFlow<FileSystemState>(FileSystemState.DetectingFileSystem)
  val fileSystemStates: StateFlow<FileSystemState> = _fileSystemStates.asStateFlow()
  private var fileObserver: FileObserver? = null
  private val requestCheckSystemFileType = MutableSharedFlow<Unit>(replay = 1).apply {
    tryEmit(Unit)
  }

  @Suppress("InjectDispatcher")
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  init {
    scope.launch {
      sharedPreferenceUtil.prefStorages
        .onEach { _fileSystemStates.emit(FileSystemState.DetectingFileSystem) }
        .combine(requestCheckSystemFileType) { storage, _ -> storage }
        .collectLatest { storage ->
          val systemState = toFileSystemState(storage)
          _fileSystemStates.emit(systemState)
          fileObserver =
            if (systemState == NotEnoughSpaceFor4GbFile) fileObserver(storage) else null
        }
    }
  }

  private fun fileObserver(it: String): FileObserver =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      object : FileObserver(File(it), MOVED_FROM or DELETE) {
        override fun onEvent(event: Int, path: String?) {
          requestCheckSystemFileType.tryEmit(Unit)
        }
      }.apply { startWatching() }
    } else {
      @Suppress("DEPRECATION")
      object : FileObserver(it, MOVED_FROM or DELETE) {
        override fun onEvent(event: Int, path: String?) {
          requestCheckSystemFileType.tryEmit(Unit)
        }
      }.apply { startWatching() }
    }

  private fun toFileSystemState(it: String) =
    when {
      File(it).freeSpace > FOUR_GIGABYTES_IN_BYTES ->
        if (canCreate4GbFile(it)) {
          CanWrite4GbFile
        } else {
          CannotWrite4GbFile
        }

      else -> NotEnoughSpaceFor4GbFile
    }

  private fun canCreate4GbFile(storage: String): Boolean {
    fileSystemCheckers.iterator().forEach {
      when (it.checkFilesystemSupports4GbFiles(storage)) {
        CAN_WRITE_4GB -> return@canCreate4GbFile true
        CANNOT_WRITE_4GB -> return@canCreate4GbFile false
        INCONCLUSIVE -> {
          // do nothing
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
    object DetectingFileSystem : FileSystemState()
  }
}
