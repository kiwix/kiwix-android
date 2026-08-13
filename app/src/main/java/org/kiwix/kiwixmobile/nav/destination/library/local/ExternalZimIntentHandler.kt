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

package org.kiwix.kiwixmobile.nav.destination.library.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineDispatcher
import org.kiwix.kiwixmobile.nav.destination.library.StorageSelectDialogConfig
import org.kiwix.kiwixmobile.utils.effects.ShowStorageSelectionDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.MainCoroutineDispatcher
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.di.MainDispatcher
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.effects.ManageExternalFilesPermissionDialog
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.libkiwix.Book
import java.io.File
import java.lang.ref.WeakReference
import java.util.Collections
import javax.inject.Inject

private const val REQUEST_STORAGE_PERMISSION = 100

@Suppress("LongParameterList", "TooGenericExceptionCaught")
class ExternalZimIntentHandler @Inject constructor(
  private val kiwixDataStore: KiwixDataStore,
  private val libkiwixBookOnDisk: LibkiwixBookOnDisk,
  private val repositoryActions: MainRepositoryActions,
  private val zimReaderFactory: ZimFileReader.Factory,
  private val processSelectedZimFilesForStandalone: ProcessSelectedZimFilesForStandalone,
  private val processSelectedZimFilesForPlayStore: ProcessSelectedZimFilesForPlayStore,
  private val kiwixPermissionChecker: KiwixPermissionChecker,
  @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  @param:MainDispatcher private val mainDispatcher: MainCoroutineDispatcher
) : SelectedZimFileCallback {
  private val activeImports = Collections.synchronizedSet(mutableSetOf<String>())
  private var activityRef: WeakReference<KiwixMainActivity>? = null
  private var pendingUri: Uri? = null
  private var isZimOpenedDirectly = false

  fun handleIntent(activity: KiwixMainActivity, intent: Intent, coroutineScope: CoroutineScope) {
    val uri = intent.data ?: return
    activityRef = WeakReference(activity)
    coroutineScope.launch {
      val needsManageStoragePermission = !kiwixPermissionChecker.isManageExternalStoragePermissionGranted()
      val needsWriteStoragePermission = !kiwixPermissionChecker.hasWriteExternalStoragePermission()

      if (needsManageStoragePermission && kiwixPermissionChecker.isAndroid11OrAbove()) {
        pendingUri = uri
        ManageExternalFilesPermissionDialog(activity.alertDialogShower).invokeWith(activity)
      } else if (needsWriteStoragePermission) {
        pendingUri = uri
        ActivityCompat.requestPermissions(
          activity,
          arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
          REQUEST_STORAGE_PERMISSION
        )
      } else {
        pendingUri = null
        val canOpenDirectly = isValidZim(activity, uri)
        isZimOpenedDirectly = canOpenDirectly
        if (canOpenDirectly) {
          activity.openZimFromFilePath(uri.toString())
        }
        importZim(activity, uri, coroutineScope)
      }
      activity.clearIntentDataAndAction()
    }
  }

  fun handlePendingUri(activity: KiwixMainActivity, coroutineScope: CoroutineScope) {
    val uri = pendingUri ?: return
    activityRef = WeakReference(activity)
    coroutineScope.launch {
      val needsManageStoragePermission = !kiwixPermissionChecker.isManageExternalStoragePermissionGranted()
      val needsWriteStoragePermission = !kiwixPermissionChecker.hasWriteExternalStoragePermission()
      if (!needsManageStoragePermission && !needsWriteStoragePermission) {
        pendingUri = null
        val canOpenDirectly = isValidZim(activity, uri)
        isZimOpenedDirectly = canOpenDirectly
        if (canOpenDirectly) {
          activity.openZimFromFilePath(uri.toString())
        }
        importZim(activity, uri, coroutineScope)
      }
    }
  }

  suspend fun isValidZim(context: Context, uri: Uri): Boolean {
    return withContext(ioDispatcher) {
      try {
        val afdList = FileUtils.getAssetFileDescriptorFromUri(context, uri)
        val zimReaderSource = ZimReaderSource(uri = uri, assetFileDescriptorList = afdList)
        zimReaderSource.canOpenInLibkiwix(ioDispatcher)
      } catch (e: Exception) {
        e.printStackTrace()
        false
      }
    }
  }

  private suspend fun importZim(
    activity: KiwixMainActivity,
    uri: Uri,
    coroutineScope: CoroutineScope
  ) {
    val importKey = FileUtils.getLocalFilePathByUri(activity, uri, ioDispatcher) ?: uri.toString()
    if (!activeImports.add(importKey)) {
      return
    }

    val books = withContext(ioDispatcher) { libkiwixBookOnDisk.getBooks() }
    val isAlreadyInLibrary = books.any { book ->
      book.zimReaderSource.toDatabase() == importKey || book.zimReaderSource.uri?.toString() == uri.toString()
    }
    if (isAlreadyInLibrary) {
      activeImports.remove(importKey)
      return
    }

    coroutineScope.launch(ioDispatcher) {
      try {
        if (kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove()) {
          processSelectedZimFilesForPlayStore.init(
            coroutineScope,
            activity.alertDialogShower,
            activity.snackBarHostState,
            this@ExternalZimIntentHandler
          )
          processSelectedZimFilesForPlayStore.processSelectedFiles(listOf(uri))
        } else {
          processSelectedZimFilesForStandalone.init(this@ExternalZimIntentHandler)
          processSelectedZimFilesForStandalone.processSelectedFiles(listOf(uri))
        }
      } catch (e: Exception) {
        Log.e("ExternalZimIntentHandler", "Error during ZIM import", e)
      } finally {
        activeImports.remove(importKey)
      }
    }
  }

  override fun navigateToReaderScreen(file: File) {
    val activity = activityRef?.get() ?: return
    if (!isZimOpenedDirectly) {
      CoroutineScope(mainDispatcher).launch {
        activity.openZimFromFilePath(file.path)
      }
    }
    addBookToLibkiwixBookOnDisk(file)
  }

  override fun addBookToLibkiwixBookOnDisk(file: File) {
    CoroutineScope(ioDispatcher).launch {
      runCatching {
        zimReaderFactory.create(ZimReaderSource(file), false)
          ?.let { zimFileReader ->
            val book = Book().apply { update(zimFileReader.jniKiwixReader) }
            repositoryActions.saveBook(book)
            zimFileReader.dispose()
          }
      }.onFailure {
        Log.e("ExternalZimIntentHandler", "Failed to save book: ${file.path}", it)
      }
    }
  }

  override fun showFileCopyMoveErrorDialog(errorMessage: String, callBack: suspend () -> Unit) {
    val activity = activityRef?.get() ?: return
    CoroutineScope(mainDispatcher).launch {
      activity.alertDialogShower.show(
        KiwixDialog.FileCopyMoveError(errorMessage),
        { CoroutineScope(ioDispatcher).launch { callBack.invoke() } }
      )
    }
  }

  override fun showStorageSelectionDialog(dialogConfig: StorageSelectDialogConfig) {
    val activity = activityRef?.get() ?: return
    CoroutineScope(mainDispatcher).launch {
      ShowStorageSelectionDialog(activity.alertDialogShower, dialogConfig).invokeWith(activity)
    }
  }
}
