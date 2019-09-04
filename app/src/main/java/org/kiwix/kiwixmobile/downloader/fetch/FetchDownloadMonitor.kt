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
package org.kiwix.kiwixmobile.downloader.fetch

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2core.DownloadBlock
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.kiwix.kiwixmobile.database.newdb.dao.FetchDownloadDao
import org.kiwix.kiwixmobile.downloader.DownloadMonitor
import javax.inject.Inject

class FetchDownloadMonitor @Inject constructor(fetch: Fetch, fetchDownloadDao: FetchDownloadDao) :
  DownloadMonitor {
  private val updater = PublishSubject.create<() -> Unit>()
  private val fetchListener = object : FetchListener {
    override fun onAdded(download: Download) {}

    override fun onCancelled(download: Download) {
      delete(download)
    }

    override fun onCompleted(download: Download) {
      update(download)
    }

    override fun onDeleted(download: Download) {
      delete(download)
    }

    override fun onDownloadBlockUpdated(
      download: Download,
      downloadBlock: DownloadBlock,
      totalBlocks: Int
    ) {
      update(download)
    }

    override fun onError(download: Download, error: Error, throwable: Throwable?) {
      update(download)
    }

    override fun onPaused(download: Download) {
      update(download)
    }

    override fun onProgress(
      download: Download,
      etaInMilliSeconds: Long,
      downloadedBytesPerSecond: Long
    ) {
      update(download)
    }

    override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
      update(download)
    }

    override fun onRemoved(download: Download) {
      delete(download)
    }

    override fun onResumed(download: Download) {
      update(download)
    }

    override fun onStarted(
      download: Download,
      downloadBlocks: List<DownloadBlock>,
      totalBlocks: Int
    ) {
      update(download)
    }

    override fun onWaitingNetwork(download: Download) {
      update(download)
    }

    private fun update(download: Download) {
      updater.onNext { fetchDownloadDao.update(download) }
    }

    private fun delete(download: Download) {
      updater.onNext { fetchDownloadDao.delete(download) }
    }
  }

  init {
    fetch.addListener(fetchListener, true)
    updater.subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe(
      {
        it.invoke()
      },
      Throwable::printStackTrace
    )
  }

  override fun init() {
    // empty method to so class does not get reported unused
  }
}
