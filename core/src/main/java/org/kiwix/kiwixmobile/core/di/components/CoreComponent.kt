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
package org.kiwix.kiwixmobile.core.di.components

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import dagger.BindsInstance
import dagger.Component
import eu.mhutti1.utils.storage.StorageSelectDialog
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.StorageObserver
import org.kiwix.kiwixmobile.core.dao.FetchDownloadDao
import org.kiwix.kiwixmobile.core.dao.NewBookDao
import org.kiwix.kiwixmobile.core.dao.NewLanguagesDao
import org.kiwix.kiwixmobile.core.data.DataModule
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.di.modules.ApplicationModule
import org.kiwix.kiwixmobile.core.di.modules.JNIModule
import org.kiwix.kiwixmobile.core.di.modules.NetworkModule
import org.kiwix.kiwixmobile.core.downloader.Downloader
import org.kiwix.kiwixmobile.core.main.AddNoteDialog
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.reader.ZimContentProvider
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.AutoCompleteAdapter
import org.kiwix.kiwixmobile.core.settings.CorePrefsFragment
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Singleton

@Singleton
@Component(
  modules = [
    ApplicationModule::class,
    NetworkModule::class,
    JNIModule::class,
    DataModule::class]
)
interface CoreComponent {

  @Component.Builder
  interface Builder {

    @BindsInstance fun context(context: Context): Builder

    fun build(): CoreComponent
  }

  fun zimReaderContainer(): ZimReaderContainer
  fun sharedPrefUtil(): SharedPreferenceUtil
  fun zimFileReaderFactory(): ZimFileReader.Factory
  fun storageObserver(): StorageObserver
  fun kiwixService(): KiwixService
  fun application(): Application
  fun bookUtils(): BookUtils
  fun dataSource(): DataSource
  fun fetchDownloadDao(): FetchDownloadDao
  fun newBookDao(): NewBookDao
  fun newLanguagesDao(): NewLanguagesDao
  fun connectivityManager(): ConnectivityManager
  fun context(): Context
  fun downloader(): Downloader
  fun notificationManager(): NotificationManager

  fun inject(application: CoreApp)
  fun inject(zimContentProvider: ZimContentProvider)
  fun inject(kiwixWebView: KiwixWebView)
  fun inject(prefsFragment: CorePrefsFragment)
  fun inject(autoCompleteAdapter: AutoCompleteAdapter)
  fun inject(storageSelectDialog: StorageSelectDialog)
  fun inject(addNoteDialog: AddNoteDialog)
}
