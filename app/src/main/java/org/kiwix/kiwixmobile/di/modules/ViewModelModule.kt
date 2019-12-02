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

package org.kiwix.kiwixmobile.di.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import org.kiwix.kiwixmobile.KiwixViewModelFactory
import org.kiwix.kiwixmobile.language.viewmodel.LanguageViewModel
import org.kiwix.kiwixmobile.core.di.ViewModelKey
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel

@Module
abstract class ViewModelModule {
  @Binds
  @IntoMap
  @ViewModelKey(ZimManageViewModel::class)
  abstract fun bindZimManageViewModel(zimManageViewModel: ZimManageViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(LanguageViewModel::class)
  abstract fun bindLanguageViewModel(languageViewModel: LanguageViewModel): ViewModel

  @Binds
  abstract fun bindViewModelFactory(factory: KiwixViewModelFactory):
    ViewModelProvider.Factory
}
