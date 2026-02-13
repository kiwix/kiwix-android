package org.kiwix.kiwixmobile.nav.destination.library.local

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.setNavigationResultOnCurrent
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.main.ZIM_FILE_URI_KEY
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.libkiwix.Book
import java.io.File
import java.io.IOException

@Composable
fun rememberSelectedZimFileCallback(
  activity: KiwixMainActivity,
  navController: NavHostController,
  dialogShower: AlertDialogShower,
  coroutineScope: CoroutineScope,
  zimReaderFactory: ZimFileReader.Factory = remember { CoreApp.coreComponent.zimFileReaderFactory() },
  mainRepositoryActions: MainRepositoryActions
): SelectedZimFileCallback {
  return remember(activity, navController, dialogShower, coroutineScope, zimReaderFactory, mainRepositoryActions) {
    object : SelectedZimFileCallback {
      override fun showFileCopyMoveErrorDialog(errorMessage: String, callBack: suspend () -> Unit) {
        dialogShower.show(
          KiwixDialog.FileCopyMoveError(errorMessage),
          {
            coroutineScope.launch {
              callBack()
            }
          }
        )
      }

      override fun navigateToReaderFragment(file: File) {
        if (!file.canRead()) {
          activity.toast(R.string.unable_to_read_zim_file)
        } else {
          @Suppress("InjectDispatcher")
          coroutineScope.launch(Dispatchers.IO) {
            try {
              zimReaderFactory.create(ZimReaderSource(file), false)?.let { zimFileReader ->
                val book = Book().apply { update(zimFileReader.jniKiwixReader) }
                mainRepositoryActions.saveBook(book)
                zimFileReader.dispose()
              }
            } catch (e: IOException) {
              Log.e("LocalLibraryRoute", "File operation failed", e)
            } catch (e: SecurityException) {
              Log.e("LocalLibraryRoute", "Permission denied", e)
            }
          }
          activity.runOnUiThread {
            val navOptions = androidx.navigation.NavOptions.Builder()
              .setPopUpTo(KiwixDestination.Reader.route, false)
              .build()
            navController.navigate(KiwixDestination.Reader.route, navOptions)
            activity.setNavigationResultOnCurrent(file.toUri().toString(), ZIM_FILE_URI_KEY)
          }
        }
      }

      override fun addBookToLibkiwixBookOnDisk(file: File) {
        @Suppress("InjectDispatcher")
        coroutineScope.launch(Dispatchers.IO) {
          try {
            zimReaderFactory.create(ZimReaderSource(file), false)?.let { zimFileReader ->
              val book = Book().apply { update(zimFileReader.jniKiwixReader) }
              mainRepositoryActions.saveBook(book)
              zimFileReader.dispose()
            }
          } catch (e: IOException) {
            Log.e("LocalLibraryRoute", "File operation failed", e)
          } catch (e: SecurityException) {
            Log.e("LocalLibraryRoute", "Permission denied", e)
          }
        }
      }
    }
  }
}
