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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_custom_download.cd_view_animator
import kotlinx.android.synthetic.main.layout_custom_download_error.cd_error_text
import kotlinx.android.synthetic.main.layout_custom_download_error.cd_retry_button
import kotlinx.android.synthetic.main.layout_custom_download_in_progress.cd_download_state
import kotlinx.android.synthetic.main.layout_custom_download_in_progress.cd_eta
import kotlinx.android.synthetic.main.layout_custom_download_in_progress.cd_progress
import kotlinx.android.synthetic.main.layout_custom_download_required.cd_download_button
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.downloader.model.DownloadItem
import org.kiwix.kiwixmobile.core.extensions.setDistinctDisplayedChild
import org.kiwix.kiwixmobile.core.extensions.viewModel
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.custom.R
import org.kiwix.kiwixmobile.custom.customActivityComponent
import org.kiwix.kiwixmobile.custom.download.Action.ClickedDownload
import org.kiwix.kiwixmobile.custom.download.Action.ClickedRetry
import org.kiwix.kiwixmobile.custom.download.State.DownloadComplete
import org.kiwix.kiwixmobile.custom.download.State.DownloadFailed
import org.kiwix.kiwixmobile.custom.download.State.DownloadInProgress
import org.kiwix.kiwixmobile.custom.download.State.DownloadRequired
import javax.inject.Inject

class CustomDownloadFragment : BaseFragment() {

  private val downloadViewModel by lazy {
    viewModel<CustomDownloadViewModel>(viewModelFactory)
  }

  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

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
    val root = inflater.inflate(R.layout.activity_custom_download, container, false)
    return root
  }

  /**
   * Called immediately after [.onCreateView]
   * has returned, but before any saved state has been restored in to the view.
   * This gives subclasses a chance to initialize themselves once
   * they know their view hierarchy has been completely created.  The fragment's
   * view hierarchy is not however attached to its parent at this point.
   * @param view The View returned by [.onCreateView].
   * @param savedInstanceState If non-null, this fragment is being re-constructed
   * from a previous saved state as given here.
   */
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val activity = requireActivity() as CoreMainActivity
    downloadViewModel.state.observe(viewLifecycleOwner, Observer(::render))
    compositeDisposable.add(
      downloadViewModel.effects.subscribe(
        { it.invokeWith(activity) },
        Throwable::printStackTrace
      )
    )
    cd_download_button.setOnClickListener { downloadViewModel.actions.offer(ClickedDownload) }
    cd_retry_button.setOnClickListener { downloadViewModel.actions.offer(ClickedRetry) }
  }

  override fun onDestroy() {
    super.onDestroy()
    compositeDisposable.clear()
  }

  private fun render(state: State) {
    return when (state) {
      DownloadRequired -> cd_view_animator.setDistinctDisplayedChild(0)
      is DownloadInProgress -> {
        cd_view_animator.setDistinctDisplayedChild(1)
        render(state.downloads[0])
      }
      is DownloadFailed -> {
        cd_view_animator.setDistinctDisplayedChild(2)
        cd_error_text.text = context?.let(state.downloadState::toReadableState)
      }
      DownloadComplete -> cd_view_animator.setDistinctDisplayedChild(3)
    }
  }

  private fun render(downloadItem: DownloadItem) {
    cd_progress.progress = downloadItem.progress
    cd_eta.text = downloadItem.readableEta
    cd_download_state.text = context?.let(downloadItem.downloadState::toReadableState)
  }
}
