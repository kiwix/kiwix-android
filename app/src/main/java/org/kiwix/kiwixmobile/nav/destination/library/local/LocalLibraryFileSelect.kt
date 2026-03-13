package org.kiwix.kiwixmobile.nav.destination.library.local

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.isManageExternalStoragePermissionGranted
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.navigateToSettings
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import java.io.IOException

@Composable
fun rememberFileSelectLauncher(
  activity: KiwixMainActivity,
  coroutineScope: CoroutineScope,
  processSelectedZimFilesForStandalone: ProcessSelectedZimFilesForStandalone,
  processSelectedZimFilesForPlayStore: ProcessSelectedZimFilesForPlayStore
): ManagedActivityResultLauncher<Intent, ActivityResult> {
  return rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val intent = result.data
      val urisList = arrayListOf<Uri>()

      // Handle multiple selection
      intent?.clipData?.let { clipData ->
        val count = clipData.itemCount
        for (i in 0 until count) {
          clipData.getItemAt(i)?.uri?.let { uri ->
            takePersistableUriPermission(activity, uri)
            urisList.add(uri)
          }
        }
      }

      // Handle single selection (fallback or if clipData is null but data is present)
      if (urisList.isEmpty()) {
        intent?.data?.let { uri ->
          takePersistableUriPermission(activity, uri)
          urisList.add(uri)
        }
      }

      if (urisList.isNotEmpty()) {
        coroutineScope.launch {
          try {
            when {
              processSelectedZimFilesForStandalone.canHandleUris() ->
                processSelectedZimFilesForStandalone.processSelectedFiles(urisList)

              activity.kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove() ->
                processSelectedZimFilesForPlayStore.processSelectedFiles(urisList)
            }
          } catch (e: IOException) {
            Log.e("LocalLibraryRoute", "Error processing selected files", e)
          } catch (e: IllegalStateException) {
            Log.e("LocalLibraryRoute", "Illegal state processing selected files", e)
          } catch (e: IllegalArgumentException) {
            Log.e("LocalLibraryRoute", "Illegal argument processing selected files", e)
          }
        }
      }
    }
  }
}

private fun takePersistableUriPermission(activity: Activity, uri: Uri) {
  try {
    activity.contentResolver.takePersistableUriPermission(
      uri,
      Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    )
  } catch (e: SecurityException) {
    Log.e("LocalLibraryRoute", "Permission denied for URI: $uri", e)
  } catch (e: IllegalStateException) {
    Log.e("LocalLibraryRoute", "Illegal state taking permission for URI: $uri", e)
  } catch (e: IllegalArgumentException) {
    Log.e("LocalLibraryRoute", "Illegal argument taking permission for URI: $uri", e)
  }
}

@Composable
fun rememberFilePickerAction(
  activity: KiwixMainActivity,
  coroutineScope: CoroutineScope,
  dialogShower: AlertDialogShower,
  kiwixDataStore: KiwixDataStore,
  fileSelectLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
): () -> Unit {
  return remember(activity, coroutineScope, dialogShower, kiwixDataStore, fileSelectLauncher) {
    {
      coroutineScope.launch {
        if (!activity.isManageExternalStoragePermissionGranted(kiwixDataStore)) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            dialogShower.show(
              KiwixDialog.ManageExternalFilesPermissionDialog,
              {
                activity.navigateToSettings()
              }
            )
          }
        } else {
          val intent = Intent().apply {
            action = Intent.ACTION_OPEN_DOCUMENT
            type = "application/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
          }
          try {
            fileSelectLauncher.launch(Intent.createChooser(intent, "Select a zim file"))
          } catch (_: ActivityNotFoundException) {
            activity.toast(
              activity.getString(R.string.no_app_found_to_open),
              Toast.LENGTH_SHORT
            )
          }
        }
      }
    }
  }
}
