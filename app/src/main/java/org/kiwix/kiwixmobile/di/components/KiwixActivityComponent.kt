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
package org.kiwix.kiwixmobile.di.components

import android.app.Activity
import dagger.BindsInstance
import dagger.Subcomponent
import org.kiwix.kiwixmobile.core.di.ActivityScope
import org.kiwix.kiwixmobile.di.modules.KiwixActivityModule
import org.kiwix.kiwixmobile.intro.IntroActivity
import org.kiwix.kiwixmobile.intro.IntroModule
import org.kiwix.kiwixmobile.language.LanguageActivity
import org.kiwix.kiwixmobile.local_file_transfer.LocalFileTransferActivity
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.settings.KiwixSettingsActivity
import org.kiwix.kiwixmobile.splash.KiwixSplashActivity
import org.kiwix.kiwixmobile.webserver.ZimHostActivity
import org.kiwix.kiwixmobile.webserver.ZimHostModule
import org.kiwix.kiwixmobile.zim_manager.ZimManageActivity
import org.kiwix.kiwixmobile.zim_manager.download_view.DownloadFragment
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.ZimFileSelectFragment
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.effects.DeleteFiles
import org.kiwix.kiwixmobile.zim_manager.library_view.LibraryFragment

@ActivityScope
@Subcomponent(
  modules = [
    KiwixActivityModule::class,
    ZimHostModule::class,
    IntroModule::class
  ]
)
interface KiwixActivityComponent {
  fun inject(downloadFragment: DownloadFragment)
  fun inject(libraryFragment: LibraryFragment)
  fun inject(zimFileSelectFragment: ZimFileSelectFragment)
  fun inject(deleteFiles: DeleteFiles)
  fun inject(localFileTransferActivity: LocalFileTransferActivity)
  fun inject(zimManageActivity: ZimManageActivity)
  fun inject(languageActivity: LanguageActivity)
  fun inject(kiwixMainActivity: KiwixMainActivity)
  fun inject(kiwixSettingsActivity: KiwixSettingsActivity)
  fun inject(zimHostActivity: ZimHostActivity)
  fun inject(introActivity: IntroActivity)
  fun inject(kiwixSplashActivity: KiwixSplashActivity)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance fun activity(activity: Activity): Builder

    fun build(): KiwixActivityComponent
  }
}
