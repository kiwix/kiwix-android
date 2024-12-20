/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.downloader.downloadManager

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2core.DownloadBlock
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.dao.entities.DownloadRoomEntity
import org.kiwix.kiwixmobile.core.utils.files.Log
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DownloadMonitorService : Service() {
  private val updater = PublishSubject.create<() -> Unit>()
  private var updaterDisposable: Disposable? = null
  private var monitoringDisposable: Disposable? = null
  private val lock = Any()

  @Inject
  lateinit var fetch: Fetch

  @Inject
  lateinit var downloadRoomDao: DownloadRoomDao

  override fun onCreate() {
    CoreApp.coreComponent
      .coreServiceComponent()
      .service(this)
      .build()
      .inject(this)
    super.onCreate()
    fetch.addListener(fetchListener, true)
    setupUpdater()
    startMonitoringDownloads()
  }

  private fun startMonitoringDownloads() {
    // Check if monitoring is already active. If it is, do nothing.
    if (monitoringDisposable?.isDisposed == false) return
    monitoringDisposable = Observable.interval(ZERO.toLong(), FIVE.toLong(), TimeUnit.SECONDS)
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .subscribe(
        {
          try {
            synchronized(lock) {
              if (getActiveDownloads().isEmpty()) {
                stopForegroundServiceForDownloads()
              }
            }
          } catch (ignore: Exception) {
            Log.e(
              "DOWNLOAD_MONITOR",
              "Couldn't get the downloads update. Original exception = $ignore"
            )
          }
        },
        Throwable::printStackTrace
      )
  }

  private fun getActiveDownloads(): List<DownloadRoomEntity> =
    downloadRoomDao.downloadRoomEntity().blockingFirst().filter { it.status != Status.PAUSED }

  private fun setupUpdater() {
    updaterDisposable = updater.subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe(
      { it.invoke() },
      Throwable::printStackTrace
    )
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

  private val fetchListener = object : FetchListener {
    override fun onAdded(download: Download) {
      // Do nothing
    }

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
      updater.onNext { downloadRoomDao.update(download) }
    }

    private fun delete(download: Download) {
      updater.onNext { downloadRoomDao.delete(download) }
    }
  }

  private fun stopForegroundServiceForDownloads() {
    // foreGroundServiceInformation = true to DEFAULT_INT_VALUE
    Log.e(
      "DOWNLOAD_MANAGER_MONITOR",
      "Stopping DownloadMonitorService as there is no more download available to track"
    )
    monitoringDisposable?.dispose()
    updaterDisposable?.dispose()
    fetch.removeListener(fetchListener)
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }
}
