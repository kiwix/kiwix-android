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
package org.kiwix.kiwixmobile.di.components

import android.app.Activity
import dagger.BindsInstance
import dagger.Subcomponent
import org.kiwix.kiwixmobile.di.modules.ActivityModule
import org.kiwix.kiwixmobile.downloader.DownloadFragment
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.ZimFileSelectFragment
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.effects.DeleteFiles
import org.kiwix.kiwixmobile.zim_manager.library_view.LibraryFragment
import org.kiwix.kiwixmobile.zim_manager.local_file_transfer.LocalFileTransferActivity

@Subcomponent(modules = [ActivityModule::class])
interface ActivityComponent {
  fun inject(downloadFragment: DownloadFragment)

  fun inject(libraryFragment: LibraryFragment)

  fun inject(zimFileSelectFragment: ZimFileSelectFragment)

  fun inject(deleteFiles: DeleteFiles)

  fun inject(localFileTransferActivity: LocalFileTransferActivity)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance fun activity(activity: Activity): Builder

    fun build(): ActivityComponent
  }
}
