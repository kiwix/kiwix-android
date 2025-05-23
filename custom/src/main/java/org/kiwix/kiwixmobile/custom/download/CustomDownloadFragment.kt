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
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.data.remote.isAuthenticationUrl
import org.kiwix.kiwixmobile.core.downloader.model.DownloadItem
import org.kiwix.kiwixmobile.core.downloader.model.DownloadState
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.hasNotificationPermission
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.requestNotificationPermission
import org.kiwix.kiwixmobile.core.extensions.setDistinctDisplayedChild
import org.kiwix.kiwixmobile.core.extensions.viewModel
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.navigateToAppSettings
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.custom.customActivityComponent
import org.kiwix.kiwixmobile.custom.databinding.FragmentCustomDownloadBinding
import org.kiwix.kiwixmobile.custom.download.Action.ClickedDownload
import org.kiwix.kiwixmobile.custom.download.Action.ClickedRetry
import org.kiwix.kiwixmobile.custom.download.State.DownloadComplete
import org.kiwix.kiwixmobile.custom.download.State.DownloadFailed
import org.kiwix.kiwixmobile.custom.download.State.DownloadInProgress
import org.kiwix.kiwixmobile.custom.download.State.DownloadRequired
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
  var sharedPreferenceUtil: SharedPreferenceUtil? = null

  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

  private var fragmentCustomDownloadBinding: FragmentCustomDownloadBinding? = null
  override fun inject(baseActivity: BaseActivity) {
    baseActivity.customActivityComponent.inject(this)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    super.onCreate(savedInstanceState)
    fragmentCustomDownloadBinding =
      FragmentCustomDownloadBinding.inflate(inflater, container, false)
    val activity = requireActivity() as CoreMainActivity
    viewLifecycleOwner.lifecycleScope.launch {
      downloadViewModel.state.collect { state ->
        render(state)
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      downloadViewModel.effects
        .collect { effect ->
          effect.invokeWith(activity)
        }
    }
    return fragmentCustomDownloadBinding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    fragmentCustomDownloadBinding?.apply {
      customDownloadRequired.cdDownloadButton.setOnClickListener {
        if (requireActivity().hasNotificationPermission(sharedPreferenceUtil)) {
          performAction(ClickedDownload)
        } else {
          requestNotificationPermission()
        }
      }
      customDownloadError.cdRetryButton.setOnClickListener {
        if (requireActivity().hasNotificationPermission(sharedPreferenceUtil)) {
          performAction(ClickedRetry)
        } else {
          requestNotificationPermission()
        }
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

  private fun render(state: State): Unit? {
    return when (state) {
      DownloadRequired ->
        fragmentCustomDownloadBinding?.cdViewAnimator?.setDistinctDisplayedChild(0)

      is DownloadInProgress -> {
        fragmentCustomDownloadBinding?.cdViewAnimator?.setDistinctDisplayedChild(1)
        showDownloadingProgress(state.downloads[0])
      }

      is DownloadFailed -> {
        fragmentCustomDownloadBinding?.cdViewAnimator?.setDistinctDisplayedChild(2)
        val errorMessage = context?.let { context ->
          if (state.downloadState.zimUrl?.isAuthenticationUrl == false) {
            return@let getErrorMessageFromDownloadState(state.downloadState, context)
          }

          val defaultErrorMessage = getErrorMessageFromDownloadState(state.downloadState, context)
          // Check if `REQUEST_NOT_SUCCESSFUL` indicates an unsuccessful response from the server.
          // If the server does not respond to the URL, we will display a custom message to the user.
          if (defaultErrorMessage == context.getString(
              R.string.failed_state,
              "REQUEST_NOT_SUCCESSFUL"
            )
          ) {
            context.getString(
              R.string.failed_state,
              context.getString(R.string.custom_download_error_message_for_authentication_failed)
            )
          } else {
            defaultErrorMessage
          }
        }
        fragmentCustomDownloadBinding?.customDownloadError?.cdErrorText?.text = errorMessage
      }

      DownloadComplete ->
        fragmentCustomDownloadBinding?.cdViewAnimator?.setDistinctDisplayedChild(3)
    }
  }

  private fun getErrorMessageFromDownloadState(
    downloadState: DownloadState,
    context: Context
  ): String = "${downloadState.toReadableState(context)}"

  private fun showDownloadingProgress(downloadItem: DownloadItem) {
    fragmentCustomDownloadBinding?.customDownloadInProgress?.apply {
      cdDownloadState.text = downloadItem.readableEta
      cdEta.text = context?.let(downloadItem.downloadState::toReadableState)
      cdProgress.progress = downloadItem.progress
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    fragmentCustomDownloadBinding?.root?.removeAllViews()
    fragmentCustomDownloadBinding = null
  }
}
