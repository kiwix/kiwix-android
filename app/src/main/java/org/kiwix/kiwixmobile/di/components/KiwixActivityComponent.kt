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
import org.kiwix.kiwixmobile.core.di.components.CoreActivityComponent
import org.kiwix.kiwixmobile.webserver.ZimHostModule
import org.kiwix.kiwixmobile.webserver.ZimHostFragment
import org.kiwix.kiwixmobile.di.modules.KiwixActivityModule
import org.kiwix.kiwixmobile.intro.IntroFragment
import org.kiwix.kiwixmobile.intro.IntroModule
import org.kiwix.kiwixmobile.language.LanguageFragment
import org.kiwix.kiwixmobile.localFileTransfer.LocalFileTransferFragment
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryFragment
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryFragment
import org.kiwix.kiwixmobile.nav.destination.reader.KiwixReaderFragment
import org.kiwix.kiwixmobile.settings.KiwixSettingsFragment
import org.kiwix.kiwixmobile.update.UpdateFragment
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.DeleteFiles
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.ValidateZIMFiles

@ActivityScope
@Subcomponent(
  modules = [
    KiwixActivityModule::class,
    ZimHostModule::class,
    IntroModule::class
  ]
)
interface KiwixActivityComponent : CoreActivityComponent {
  fun inject(readerFragment: KiwixReaderFragment)
  fun inject(localLibraryFragment: LocalLibraryFragment)
  fun inject(deleteFiles: DeleteFiles)
  fun inject(validateZIMFiles: ValidateZIMFiles)
  fun inject(localFileTransferFragment: LocalFileTransferFragment)
  fun inject(languageFragment: LanguageFragment)
  fun inject(zimHostFragment: ZimHostFragment)
  fun inject(kiwixSettingsFragment: KiwixSettingsFragment)
  fun inject(introActivity: IntroFragment)
  fun inject(updateFragment: UpdateFragment)
  fun inject(kiwixMainActivity: KiwixMainActivity)
  fun inject(onlineLibraryFragment: OnlineLibraryFragment)

  @Subcomponent.Builder
  interface Builder {
    @BindsInstance fun activity(activity: Activity): Builder

    fun build(): KiwixActivityComponent
  }
}
