/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CanWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.NotEnoughSpaceFor4GbFile
import org.kiwix.kiwixmobile.zimManager.FileSystemCapability.CANNOT_WRITE_4GB
import org.kiwix.kiwixmobile.zimManager.FileSystemCapability.CAN_WRITE_4GB
import org.kiwix.kiwixmobile.zimManager.FileSystemCapability.INCONCLUSIVE
import java.io.File

class Fat32Checker constructor(
  kiwixDataStore: KiwixDataStore,
  private val fileSystemCheckers: List<FileSystemChecker>,
  @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
  private val _fileSystemStates =
    MutableStateFlow<FileSystemState>(FileSystemState.DetectingFileSystem)
  val fileSystemStates: StateFlow<FileSystemState> = _fileSystemStates.asStateFlow()
  private var fileObserver: FileObserver? = null
  private var isDisposed = false
  private val requestCheckSystemFileType = MutableSharedFlow<Unit>(replay = 1).apply {
    tryEmit(Unit)
  }

  private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

  init {
    scope.launch {
      kiwixDataStore.selectedStorage
        .onEach { _fileSystemStates.emit(FileSystemState.DetectingFileSystem) }
        .combine(requestCheckSystemFileType) { storage, _ -> storage }
        .collectLatest { storage ->
          val storageFile = File(storage)
          if (!storageFile.exists()) {
            _fileSystemStates.emit(NotEnoughSpaceFor4GbFile)
            replaceFileObserver(null)
            return@collectLatest
          }
          val systemState = toFileSystemState(storageFile)
          _fileSystemStates.emit(systemState)
          replaceFileObserver(storageFile.takeIf { systemState == NotEnoughSpaceFor4GbFile })
        }
    }
  }

  private fun replaceFileObserver(storageFile: File?) = synchronized(FILE_OBSERVER_LOCK) {
    fileObserver?.stopWatching()
    fileObserver = if (isDisposed) null else storageFile?.let(::startFileObserver)
  }

  private fun startFileObserver(storageFile: File): FileObserver? =
    try {
      createFileObserver(storageFile).apply { startWatching() }
    } catch (_: ConcurrentModificationException) {
      null
    }

  private fun createFileObserver(storageFile: File): FileObserver =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      object : FileObserver(storageFile, MOVED_FROM or DELETE) {
        override fun onEvent(event: Int, path: String?) {
          requestCheckSystemFileType.tryEmit(Unit)
        }
      }
    } else {
      @Suppress("DEPRECATION")
      object : FileObserver(storageFile.path, MOVED_FROM or DELETE) {
        override fun onEvent(event: Int, path: String?) {
          requestCheckSystemFileType.tryEmit(Unit)
        }
      }
    }

  private fun toFileSystemState(storageFile: File) =
    when {
      storageFile.freeSpace > FOUR_GIGABYTES_IN_BYTES ->
        if (canCreate4GbFile(storageFile.path)) {
          CanWrite4GbFile
        } else {
          CannotWrite4GbFile
        }

      else -> NotEnoughSpaceFor4GbFile
    }

  private fun canCreate4GbFile(storagePath: String): Boolean {
    for (checker in fileSystemCheckers) {
      when (checker.checkFilesystemSupports4GbFiles(storagePath)) {
        CAN_WRITE_4GB -> return true
        CANNOT_WRITE_4GB -> return false
        INCONCLUSIVE -> Unit
      }
    }
    return false
  }

  fun dispose() {
    synchronized(FILE_OBSERVER_LOCK) {
      isDisposed = true
      fileObserver?.stopWatching()
      fileObserver = null
    }
    scope.cancel()
  }

  companion object {
    const val FOUR_GIGABYTES_IN_BYTES = 4L * 1024L * 1024L * 1024L
    private val FILE_OBSERVER_LOCK = Any()
  }

  sealed class FileSystemState {
    object NotEnoughSpaceFor4GbFile : FileSystemState()
    object CanWrite4GbFile : FileSystemState()
    object CannotWrite4GbFile : FileSystemState()
    object DetectingFileSystem : FileSystemState()
  }
}
