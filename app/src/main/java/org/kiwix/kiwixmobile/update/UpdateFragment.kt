/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.update

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.cachedComponent
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.hasNotificationPermission
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.requestNotificationPermission
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.extensions.viewModel
import org.kiwix.kiwixmobile.core.navigateToAppSettings
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.utils.NetworkUtils
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.DialogHost
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.update.viewmodel.UpdateStates
import org.kiwix.kiwixmobile.update.viewmodel.UpdateViewModel
import java.io.File
import javax.inject.Inject

class UpdateFragment : BaseFragment() {
  private val updateViewModel by lazy { viewModel<UpdateViewModel>(viewModelFactory) }

  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

  @Inject lateinit var alertDialogShower: AlertDialogShower

  @Inject lateinit var kiwixDataStore: KiwixDataStore

  private var composeView: ComposeView? = null

  private val isNotConnected: Boolean
    get() = !NetworkUtils.isNetworkAvailable(requireActivity())

  override fun inject(baseActivity: BaseActivity) {
    baseActivity.cachedComponent.inject(this)
  }

  private fun requestNotificationPermission() {
    if (!shouldShowRationale(Manifest.permission.POST_NOTIFICATIONS)) {
      requireActivity().requestNotificationPermission()
    } else {
      alertDialogShower.show(
        KiwixDialog.NotificationPermissionDialog,
        requireActivity()::navigateToAppSettings
      )
    }
  }

  private fun shouldShowRationale(permission: String) =
    ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permission)

  private fun noInternetSnackbar() {
    updateViewModel.state.value.snackbarHostState.snack(
      message = getString(string.no_network_connection),
      actionLabel = getString(string.menu_settings),
      lifecycleScope = lifecycleScope,
      actionClick = { openNetworkSettings() }
    )
  }

  private fun openNetworkSettings() {
    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
  }

  private lateinit var backPressedCallback: OnBackPressedCallback

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    backPressedCallback = object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        onNavigationBack(updateViewModel.state.value)
      }
    }
    requireActivity().onBackPressedDispatcher.addCallback(
      viewLifecycleOwner,
      backPressedCallback
    )
    composeView?.setContent {
      KiwixTheme {
        val state = updateViewModel.state.value
        val context = LocalContext.current
        UpdateScreen(
          state = state,
          onUpdateClick = {
            onUpdateClick()
          },
          onUpdateCancel = {
            onStopButtonClick(
              state = state
            )
          },
          onInstallApk = {
            installApk(
              context = context,
              state = state
            )
          },
          content = {
            NavigationIcon(
              iconItem = IconItem.Drawable(
                R.drawable.ic_close_white_24dp
              ),
              onClick = {
                onNavigationBack(state)
              }
            )
          }
        )
        DialogHost(alertDialogShower)
      }
    }
  }

  private fun onUpdateClick() {
    lifecycleScope.launch {
      if (requireActivity().hasNotificationPermission(kiwixDataStore)) {
        when {
          isNotConnected -> {
            noInternetSnackbar()
            return@launch
          }

          else -> updateViewModel.downloadApk()
        }
      } else {
        requestNotificationPermission()
      }
    }
  }

  private fun onStopButtonClick(state: UpdateStates) {
    val downloadState = state.downloadApkItem
    if (downloadState.currentDownloadState == Status.FAILED) {
      when (downloadState.downloadError) {
        Error.UNKNOWN_IO_ERROR,
        Error.CONNECTION_TIMED_OUT,
        Error.UNKNOWN -> {
          // Retry the download if it can be retried.
          // For other failure reasons, retrying is not possible.
          if (isNotConnected) {
            noInternetSnackbar()
          } else {
            // downloader.retryDownload(item.downloadId)
          }
        }

        // For other errors such as REQUEST_DOES_NOT_EXIST, EMPTY_RESPONSE_FROM_SERVER,
        // REQUEST_NOT_SUCCESSFUL, etc., the download cannot be retried.
        // In such cases, allow the user to stop the failed download manually.
        else -> showStopDownloadDialog()
      }
    } else {
      // If the download is not in FAILED state, simply show the stop dialog when user clicks on stop button.
      showStopDownloadDialog()
    }
  }

  private fun onNavigationBack(state: UpdateStates) {
    val downloadState = state.downloadApkItem.currentDownloadState
    if (downloadState == Status.QUEUED ||
      downloadState == Status.DOWNLOADING
    ) {
      showStopDownloadDialog()
    } else {
      backPressedCallback.isEnabled = false
      requireActivity().onBackPressedDispatcher.onBackPressed()
    }
  }

  @SuppressLint("RequestInstallPackagesPolicy")
  fun installApk(
    context: Context,
    state: UpdateStates
  ) {
    val authority = "${context.packageName}.fileprovider"
    val mimeType = "application/vnd.android.package-archive"
    val fileLoc = state.downloadApkItem.file ?: return
    val apkFile =
      File(fileLoc)
    val apkUri = FileProvider.getUriForFile(
      context,
      authority,
      apkFile
    )
    /*This flag prevents user to install the apk,
    if they removed the apk file from storage but the download status is set to COMPLETED*/
    if (canOpenApk(apkFile)) {
      @Suppress("DEPRECATION")
      val installerIntent = Intent(Intent.ACTION_INSTALL_PACKAGE)
      installerIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
      installerIntent.setDataAndType(
        apkUri,
        mimeType,
      )
      installerIntent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
      context.startActivity(installerIntent)
    } else {
      alertDialogShower.show(
        KiwixDialog.ShowReDownloadDialog,
        {
          onUpdateClick()
        },
        {
        }
      )
    }
  }

  private fun canOpenApk(apkFile: File): Boolean {
    return when {
      apkFile.exists() -> true
      else -> false
    }
  }

  private fun showStopDownloadDialog() {
    alertDialogShower.show(
      KiwixDialog.YesNoDialog.StopDownload,
      { updateViewModel.cancelDownload() }
    )
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return ComposeView(requireContext()).also {
      composeView = it
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    composeView?.disposeComposition()
    composeView = null
  }
}
