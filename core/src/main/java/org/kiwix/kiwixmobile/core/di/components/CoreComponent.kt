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
import android.content.Context
import dagger.BindsInstance
import dagger.Component
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.MainCoroutineDispatcher
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.data.DataModule
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.di.MainDispatcher
import org.kiwix.kiwixmobile.core.di.modules.ApplicationModule
import org.kiwix.kiwixmobile.core.di.modules.JNIModule
import org.kiwix.kiwixmobile.core.di.modules.KiwixPermissionModule
import org.kiwix.kiwixmobile.core.di.modules.MutexModule
import org.kiwix.kiwixmobile.core.di.modules.NetworkModule
import org.kiwix.kiwixmobile.core.di.modules.ReaderModule
import org.kiwix.kiwixmobile.core.di.modules.SearchModule
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import javax.inject.Singleton

@Singleton
@Component(
  modules = [
    ApplicationModule::class,
    NetworkModule::class,
    JNIModule::class,
    DataModule::class,
    SearchModule::class,
    MutexModule::class,
    KiwixPermissionModule::class,
    ReaderModule::class
  ]
)
interface CoreComponent {
  @Component.Builder
  interface Builder {
    @BindsInstance fun context(context: Context): Builder

    @BindsInstance fun application(application: Application): Builder

    fun build(): CoreComponent
  }

  // Kept for KiwixReaderScreenTest and ObjectBoxToLibkiwixMigratorTest (app/src/androidTest),
  // which still resolve these off TestComponent (: CoreComponent) directly.
  fun zimReaderContainer(): ZimReaderContainer
  fun libkiwixBookmarks(): LibkiwixBookmarks
  fun libkiwixBooks(): LibkiwixBookOnDisk

  @IoDispatcher
  fun provideIoDispatcher(): CoroutineDispatcher

  @MainDispatcher
  fun provideMainDispatcher(): MainCoroutineDispatcher
}
