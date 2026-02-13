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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
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

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val activity = requireActivity() as CoreMainActivity
    super.onViewCreated(view, savedInstanceState)
    composeView?.setContent {
      KiwixTheme {
        val context = LocalContext.current
        UpdateScreen(
          state = updateViewModel.state.value,
          events = updateViewModel::event,
          onUpdateClick = {
            onUpdateClick()
          },
          onUpdateCancel = {
            updateViewModel.cancelDownload()
          },
          onInstallApk = {
            installApk(
              context,
              updateViewModel.state.value
            )
          },
          navigationIcon = {
            NavigationIcon(
              iconItem = IconItem.Drawable(
                R.drawable.ic_close_white_24dp
              ),
              onClick = {
                activity.onBackPressedDispatcher.onBackPressed()
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

  @Suppress("all")
  @SuppressLint("RequestInstallPackagesPolicy")
  fun installApk(
    context: Context,
    states: UpdateStates
  ) {
    val apkFile =
      File("/storage/emulated/0/Android/media/org.kiwix.kiwixmobile/Kiwix/org.kiwix.kiwixmobile.standalone-3.14.0.apk")
    val apkUri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      apkFile
    )
    if (canOpenApk(apkFile)) {
      @Suppress("DEPRECATION")
      val installerIntent = Intent(Intent.ACTION_INSTALL_PACKAGE)
      installerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
      installerIntent.setDataAndType(
        apkUri,
        "application/vnd.android.package-archive",
      )
      installerIntent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
      context.startActivity(installerIntent)
    } else {
      alertDialogShower.show(
        KiwixDialog.ShowReDownloadDialog,
        {
          updateViewModel.downloadApk()
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
