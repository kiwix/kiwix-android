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
package org.kiwix.kiwixmobile.di.modules

import dagger.Binds
import dagger.Module
import org.kiwix.kiwixmobile.downloader.DownloadManagerRequester
import org.kiwix.kiwixmobile.downloader.DownloadRequester
import org.kiwix.kiwixmobile.downloader.Downloader
import org.kiwix.kiwixmobile.downloader.DownloaderImpl

@Module
abstract class DownloaderModule {
  @Binds
  abstract fun bindDownloader(downloaderImpl: DownloaderImpl): Downloader

  @Binds
  abstract fun bindDownloaderRequester(downloaderImpl: DownloadManagerRequester): DownloadRequester
}
