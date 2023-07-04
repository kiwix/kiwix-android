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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.reactivex.disposables.CompositeDisposable
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.downloader.model.DownloadItem
import org.kiwix.kiwixmobile.core.extensions.setDistinctDisplayedChild
import org.kiwix.kiwixmobile.core.extensions.viewModel
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
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

  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

  private var fragmentCustomDownloadBinding: FragmentCustomDownloadBinding? = null

  private val compositeDisposable = CompositeDisposable()
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
    downloadViewModel.state.observe(viewLifecycleOwner, Observer(::render))
    compositeDisposable.add(
      downloadViewModel.effects.subscribe(
        { it.invokeWith(activity) },
        Throwable::printStackTrace
      )
    )
    return fragmentCustomDownloadBinding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    fragmentCustomDownloadBinding?.customDownloadRequired
      ?.cdDownloadButton
      ?.setOnClickListener {
        downloadViewModel.actions.offer(
          ClickedDownload
        )
      }
    fragmentCustomDownloadBinding?.customDownloadError
      ?.cdRetryButton
      ?.setOnClickListener {
        downloadViewModel.actions.offer(
          ClickedRetry
        )
      }
  }

  override fun onDestroy() {
    super.onDestroy()
    compositeDisposable.clear()
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
        fragmentCustomDownloadBinding?.customDownloadError?.cdErrorText?.text =
          context?.let(state.downloadState::toReadableState)
      }

      DownloadComplete ->
        fragmentCustomDownloadBinding?.cdViewAnimator?.setDistinctDisplayedChild(3)
    }
  }

  private fun showDownloadingProgress(downloadItem: DownloadItem) {
    fragmentCustomDownloadBinding?.customDownloadInProgress?.apply {
      cdDownloadState.text = downloadItem.readableEta
      cdEta.text = context?.let(downloadItem.downloadState::toReadableState)
      cdProgress.progress = downloadItem.progress
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    fragmentCustomDownloadBinding = null
  }
}
