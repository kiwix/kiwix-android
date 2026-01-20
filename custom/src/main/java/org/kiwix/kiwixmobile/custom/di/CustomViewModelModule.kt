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

import androidx.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import org.kiwix.kiwixmobile.core.di.ViewModelKey
import org.kiwix.kiwixmobile.core.di.modules.CoreViewModelModule
import org.kiwix.kiwixmobile.custom.download.CustomDownloadViewModel
import org.kiwix.kiwixmobile.custom.help.CustomHelpViewModel
import org.kiwix.kiwixmobile.custom.settings.CustomSettingsViewModel

@Module(includes = [CoreViewModelModule::class])
abstract class CustomViewModelModule {
  @Binds
  @IntoMap
  @ViewModelKey(CustomDownloadViewModel::class)
  abstract fun bindCustomDownloadViewModel(zimManageViewModel: CustomDownloadViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(CustomHelpViewModel::class)
  abstract fun bindCustomHelpViewModel(customHelpViewModel: CustomHelpViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(CustomSettingsViewModel::class)
  abstract fun bindCustomSettingsViewModel(customSettingsViewModel: CustomSettingsViewModel): ViewModel
}
