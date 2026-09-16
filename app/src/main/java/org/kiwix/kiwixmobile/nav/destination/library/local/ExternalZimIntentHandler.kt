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

import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.LibkiwixBookFactory
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.di.MainDispatcher
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.effects.ManageExternalFilesPermissionDialog
import org.kiwix.kiwixmobile.core.utils.effects.ReadPermissionRequiredDialog
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.StorageSelectDialogConfig
import org.kiwix.kiwixmobile.utils.effects.ShowStorageSelectionDialog
import java.io.File
import java.lang.ref.WeakReference
import javax.inject.Inject

@Suppress("LongParameterList")
class ExternalZimIntentHandler @Inject constructor(
  private val repositoryActions: MainRepositoryActions,
  private val zimReaderFactory: ZimFileReader.Factory,
  private val libkiwixBookFactory: LibkiwixBookFactory,
  private val processSelectedZimFilesForStandalone: ProcessSelectedZimFilesForStandalone,
  private val processSelectedZimFilesForPlayStore: ProcessSelectedZimFilesForPlayStore,
  private val kiwixPermissionChecker: KiwixPermissionChecker,
  @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  @param:MainDispatcher private val mainDispatcher: MainCoroutineDispatcher
) : SelectedZimFileCallback {
  private var activityRef: WeakReference<KiwixMainActivity>? = null
  private var lifecycleScope: CoroutineScope? = null
  private var pendingUri: Uri? = null

  private val _requestReadWritePermission = MutableSharedFlow<Unit>()
  val requestReadWritePermission: SharedFlow<Unit> = _requestReadWritePermission.asSharedFlow()

  fun init(activity: KiwixMainActivity, coroutineScope: CoroutineScope) {
    activityRef = WeakReference(activity)
    lifecycleScope = coroutineScope
    processSelectedZimFilesForStandalone.init(this)
    processSelectedZimFilesForPlayStore.init(
      lifecycleScope = coroutineScope,
      alertDialogShower = activity.alertDialogShower,
      snackBarHostState = activity.snackBarHostState,
      selectedZimFileCallback = this
    )
  }

  private fun requireLifecycleScope(): CoroutineScope =
    requireNotNull(lifecycleScope) {
      "Lifecycle scope is not set. Check the ExternalZimIntentHandler.init method"
    }

  private fun requireMainActivity(): KiwixMainActivity = requireNotNull(activityRef?.get()) {
    "MainActivity reference is null. Ensure init() is called before using ExternalZimIntentHandler."
  }

  fun handleIntent(intent: Intent?) {
    val uri = intent?.data ?: return
    checkPermissionsAndProceed(uri)
    requireMainActivity().clearIntentDataAndAction()
  }

  fun handlePendingUri() {
    val uri = pendingUri ?: return
    checkPermissionsAndProceed(uri)
  }

  // Unlike handlePendingUri, never re-prompts for a still-missing permission, otherwise the
  // permission/rationale dialog would keep reappearing on every activity resume.
  fun resumePendingImport() {
    val uri = pendingUri ?: return
    requireLifecycleScope().launch {
      if (hasRequiredStoragePermissions()) {
        pendingUri = null
        importZim(uri)
      }
    }
  }

  private suspend fun hasRequiredStoragePermissions(): Boolean =
    kiwixPermissionChecker.hasWriteExternalStoragePermission() &&
      (
        !kiwixPermissionChecker.isAndroid11OrAbove() ||
          kiwixPermissionChecker.isManageExternalStoragePermissionGranted()
      )

  fun onReadWriteRationalPermission() {
    val activity = requireMainActivity()
    ReadPermissionRequiredDialog(activity.alertDialogShower).invokeWith(activity)
  }

  private fun checkPermissionsAndProceed(uri: Uri) {
    val activity = requireMainActivity()
    requireLifecycleScope().launch {
      when {
        !kiwixPermissionChecker.hasWriteExternalStoragePermission() -> {
          pendingUri = uri
          _requestReadWritePermission.emit(Unit)
        }

        !kiwixPermissionChecker.isManageExternalStoragePermissionGranted() &&
          kiwixPermissionChecker.isAndroid11OrAbove() -> {
          pendingUri = uri
          ManageExternalFilesPermissionDialog(activity.alertDialogShower).invokeWith(activity)
        }

        else -> {
          pendingUri = null
          importZim(uri)
        }
      }
    }
  }

  private suspend fun importZim(uri: Uri) {
    val uris = listOf(uri)
    when {
      // Process the ZIM file for standalone app.
      processSelectedZimFilesForStandalone.canHandleUris() ->
        processSelectedZimFilesForStandalone.processSelectedFiles(uris)

      /**
       *  Process the ZIM file for PlayStore app.
       *  For the playStore variant there is already a feature which ignores
       *  the already available ZIM file in our app-specific directory so we don't need to
       *  check for that here.
       *  See [ProcessSelectedZimFilesForPlayStore.processSelectedFiles] for more details.
       */
      processSelectedZimFilesForPlayStore.canHandleUris() ->
        processSelectedZimFilesForPlayStore.processSelectedFiles(uris)
    }
  }

  override fun navigateToReaderScreen(file: File) {
    requireLifecycleScope().launch(mainDispatcher) {
      requireMainActivity().openZimFromFilePath(file.path)
    }
    addBookToLibkiwixBookOnDisk(file)
  }

  override fun addBookToLibkiwixBookOnDisk(file: File) {
    requireLifecycleScope().launch(ioDispatcher) {
      runCatching {
        zimReaderFactory.create(ZimReaderSource(file), false)
          ?.let { zimFileReader ->
            val book = libkiwixBookFactory.create().apply { update(zimFileReader.jniKiwixReader) }
            repositoryActions.saveBook(book)
            zimFileReader.dispose()
          }
      }.onFailure {
        Log.e("ExternalZimIntentHandler", "Failed to save book: ${file.path}", it)
      }
    }
  }

  override fun showFileCopyMoveErrorDialog(errorMessage: String, callBack: suspend () -> Unit) {
    val activity = requireMainActivity()
    requireLifecycleScope().launch(mainDispatcher) {
      activity.alertDialogShower.show(
        KiwixDialog.FileCopyMoveError(errorMessage),
        { requireLifecycleScope().launch(ioDispatcher) { callBack.invoke() } }
      )
    }
  }

  override fun showStorageSelectionDialog(dialogConfig: StorageSelectDialogConfig) {
    val activity = requireMainActivity()
    requireLifecycleScope().launch(mainDispatcher) {
      ShowStorageSelectionDialog(activity.alertDialogShower, dialogConfig).invokeWith(activity)
    }
  }
}
