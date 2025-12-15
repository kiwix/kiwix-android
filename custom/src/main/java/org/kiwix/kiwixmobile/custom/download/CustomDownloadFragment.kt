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

package org.kiwix.kiwixmobile.custom.download

import android.Manifest.permission.POST_NOTIFICATIONS
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.hasNotificationPermission
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.requestNotificationPermission
import org.kiwix.kiwixmobile.core.extensions.runSafelyInLifecycleScope
import org.kiwix.kiwixmobile.core.extensions.update
import org.kiwix.kiwixmobile.core.extensions.viewModel
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.navigateToAppSettings
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.DialogHost
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.custom.customActivityComponent
import org.kiwix.kiwixmobile.custom.download.Action.ClickedDownload
import org.kiwix.kiwixmobile.custom.download.Action.ClickedRetry
import javax.inject.Inject

class CustomDownloadFragment : BaseFragment(), FragmentActivityExtensions {
  private val downloadViewModel by lazy {
    viewModel<CustomDownloadViewModel>(viewModelFactory)
  }

  @JvmField
  @Inject
  var alertDialogShower: DialogShower? = null

  @JvmField
  @Inject
  var kiwixDataStore: KiwixDataStore? = null

  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
  private var composeView: ComposeView? = null
  private var downloadState = mutableStateOf<State>(State.DownloadRequired)
  override fun inject(baseActivity: BaseActivity) {
    baseActivity.customActivityComponent.inject(this)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    super.onCreate(savedInstanceState)
    composeView = ComposeView(requireContext()).apply {
      setContent {
        CustomDownloadScreen(
          state = downloadState.value,
          onDownloadClick = { downloadButtonClick() },
          onRetryClick = { retryButtonClick() }
        )
        DialogHost(alertDialogShower as AlertDialogShower)
      }
    }
    val activity = requireActivity() as CoreMainActivity
    viewLifecycleOwner.lifecycleScope.launch {
      downloadViewModel.state.collect { state ->
        downloadState.update { state }
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      downloadViewModel.effects
        .collect { effect ->
          effect.invokeWith(activity)
        }
    }
    return composeView
  }

  private fun downloadButtonClick() {
    lifecycleScope.runSafelyInLifecycleScope {
      if (requireActivity().hasNotificationPermission(kiwixDataStore)) {
        performAction(ClickedDownload)
      } else {
        requestNotificationPermission()
      }
    }
  }

  private fun retryButtonClick() {
    lifecycleScope.runSafelyInLifecycleScope {
      if (requireActivity().hasNotificationPermission(kiwixDataStore)) {
        performAction(ClickedRetry)
      } else {
        requestNotificationPermission()
      }
    }
  }

  private fun performAction(action: Action) {
    viewLifecycleOwner.lifecycleScope.launch {
      downloadViewModel.actions.emit(action)
    }
  }

  private fun requestNotificationPermission() {
    if (!ActivityCompat.shouldShowRequestPermissionRationale(
        requireActivity(),
        POST_NOTIFICATIONS
      )
    ) {
      requireActivity().requestNotificationPermission()
    } else {
      alertDialogShower?.show(
        KiwixDialog.NotificationPermissionDialog,
        requireActivity()::navigateToAppSettings
      )
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    activity?.finish()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    composeView?.disposeComposition()
    composeView = null
  }
}
