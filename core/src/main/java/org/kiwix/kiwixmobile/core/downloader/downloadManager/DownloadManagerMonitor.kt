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

import android.app.DownloadManager
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.kiwix.kiwixmobile.core.dao.FetchDownloadDao
import org.kiwix.kiwixmobile.core.downloader.DownloadMonitor
import javax.inject.Inject

class DownloadManagerMonitor @Inject constructor(
  private val downloadManager: DownloadManager,
  private val fetchDownloadDao: FetchDownloadDao
) :
  DownloadMonitor, DownloadManagerBroadcastReceiver.Callback {
  private val updater = PublishSubject.create<() -> Unit>()

  override fun downloadInformation() {}

  init {
    updater.subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe(
      { it.invoke() },
      Throwable::printStackTrace
    )
  }

  override fun init() {
    // empty method to so class does not get reported unused}
  }
}
