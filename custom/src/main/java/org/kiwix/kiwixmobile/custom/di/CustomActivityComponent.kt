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

package org.kiwix.kiwixmobile.custom.di

import android.app.Activity
import dagger.BindsInstance
import dagger.Subcomponent
import org.kiwix.kiwixmobile.core.di.ActivityScope
import org.kiwix.kiwixmobile.core.di.components.CoreActivityComponent
import org.kiwix.kiwixmobile.core.webserver.ZimHostModule
import org.kiwix.kiwixmobile.custom.download.CustomDownloadFragment
import org.kiwix.kiwixmobile.custom.main.CustomMainActivity
import org.kiwix.kiwixmobile.custom.main.CustomReaderFragment
import org.kiwix.kiwixmobile.custom.settings.CustomSettingsFragment

@ActivityScope
@Subcomponent(modules = [CustomActivityModule::class, ZimHostModule::class])
interface CustomActivityComponent : CoreActivityComponent {
  fun inject(customMainActivity: CustomMainActivity)
  fun inject(customSettingsFragment: CustomSettingsFragment)
  fun inject(customDownloadFragment: CustomDownloadFragment)
  fun inject(customReaderFragment: CustomReaderFragment)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance fun activity(activity: Activity): Builder

    fun build(): CustomActivityComponent
  }
}
