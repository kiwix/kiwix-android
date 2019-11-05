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

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.FetchDownloadDao
import org.kiwix.kiwixmobile.core.downloader.model.DownloadItem
import org.kiwix.kiwixmobile.core.downloader.model.DownloadState.Failed
import org.kiwix.kiwixmobile.custom.download.Action.ClickedDownload
import org.kiwix.kiwixmobile.custom.download.Action.ClickedRetry
import org.kiwix.kiwixmobile.custom.download.Action.DatabaseEmission
import org.kiwix.kiwixmobile.custom.download.State.DownloadComplete
import org.kiwix.kiwixmobile.custom.download.State.DownloadFailed
import org.kiwix.kiwixmobile.custom.download.State.DownloadInProgress
import org.kiwix.kiwixmobile.custom.download.State.DownloadRequired
import org.kiwix.kiwixmobile.custom.download.effects.DownloadCustom
import org.kiwix.kiwixmobile.custom.download.effects.FinishAndStartMain
import org.kiwix.kiwixmobile.custom.download.effects.SetPreferredStorageWithMostSpace
import javax.inject.Inject

class CustomDownloadViewModel @Inject constructor(
  downloadDao: FetchDownloadDao,
  setPreferredStorageWithMostSpace: SetPreferredStorageWithMostSpace,
  private val downloadCustom: DownloadCustom
) : ViewModel() {

  val state = MutableLiveData<State>().apply { value = DownloadRequired }
  val actions = PublishProcessor.create<Action>()
  private val _effects = PublishProcessor.create<SideEffect<*>>()
  val effects = _effects.startWith(setPreferredStorageWithMostSpace)

  private val compositeDisposable = CompositeDisposable()

  init {
    compositeDisposable.addAll(
      reducer(),
      downloadsAsActions(downloadDao)
    )
  }

  private fun reducer() = actions.map { reduce(it, state.value!!) }
    .distinctUntilChanged()
    .subscribe(state::postValue, Throwable::printStackTrace)

  private fun downloadsAsActions(downloadDao: FetchDownloadDao) =
    downloadDao.downloads()
      .map { it.map(::DownloadItem) }
      .subscribe(
        { actions.offer(DatabaseEmission(it)) },
        Throwable::printStackTrace
      )

  private fun reduce(action: Action, state: State): State {
    return when (action) {
      is DatabaseEmission -> reduceDatabaseEmission(state, action)
      ClickedRetry,
      ClickedDownload -> state.also { _effects.offer(downloadCustom) }
    }
  }

  private fun reduceDatabaseEmission(state: State, action: DatabaseEmission) = when (state) {
    is DownloadFailed,
    DownloadRequired ->
      if (action.downloads.isNotEmpty()) DownloadInProgress(action.downloads)
      else state
    is DownloadInProgress ->
      if (action.downloads.isNotEmpty())
        if (action.downloads[0].downloadState is Failed)
          DownloadFailed(action.downloads[0].downloadState)
        else
          DownloadInProgress(action.downloads)
      else
        DownloadComplete.also { _effects.offer(FinishAndStartMain()) }
    DownloadComplete -> state
  }
}
