package org.kiwix.kiwixmobile.zim_manager

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.database.DownloadDao
import org.kiwix.kiwixmobile.downloader.model.DownloadItem
import org.kiwix.kiwixmobile.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.downloader.model.DownloadState.Successful
import org.kiwix.kiwixmobile.downloader.model.DownloadStatus
import org.kiwix.kiwixmobile.downloader.Downloader
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
class ZimManageViewModel @Inject constructor(
  val downloadDao: DownloadDao,
  val downloader: Downloader
) : ViewModel() {
  val downloadItems: MutableLiveData<List<DownloadItem>> = MutableLiveData()
  private val compositeDisposable = CompositeDisposable()

  init {
    val downloadStatuses = downloadStatuses()
    compositeDisposable.addAll(
        updateDownloadItems(downloadStatuses),
        removeCompletedDownloadsFromDb(downloadStatuses)
    )
  }

  private fun removeCompletedDownloadsFromDb(downloadStatuses: Flowable<List<DownloadStatus>>) =
    downloadStatuses.subscribe(
        {
          downloadDao.delete(
              *it.filter { status -> status.state == Successful }.map { status -> status.downloadId }.toTypedArray()
          )
        },
        Throwable::printStackTrace
    )

  private fun updateDownloadItems(downloadStatuses: Flowable<List<DownloadStatus>>) =
    downloadStatuses
        .map { statuses -> statuses.map { DownloadItem(it) } }
        .subscribe(
            downloadItems::postValue,
            Throwable::printStackTrace
        )

  private fun downloadStatuses() = Flowable.combineLatest(
      downloadDao.downloads(),
      Flowable.interval(1, SECONDS),
      BiFunction { downloadModels: List<DownloadModel>, _: Long -> downloadModels }
  )
      .subscribeOn(Schedulers.io())
      .map(downloader::queryStatus)
      .distinctUntilChanged()

  override fun onCleared() {
    compositeDisposable.clear()
    super.onCleared()
  }
}