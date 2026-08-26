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

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.kiwix.kiwixmobile.core.di.modules.CoreViewModelModule

// All of :app's own ViewModels are now @HiltViewModel and resolved via hiltViewModel() (#5023).
// This module still includes CoreViewModelModule because :app's nav graph also uses several
// core-shared ViewModels (HistoryViewModel, NotesViewModel, BookmarkViewModel, AddNoteViewModel,
// SearchViewModel, ValidateZimViewModel, ...) via the legacy `viewModel(factory = viewModelFactory)`
// path - those can't move to @HiltViewModel until :core itself is converted.
@InstallIn(SingletonComponent::class)
@Module(includes = [CoreViewModelModule::class])
abstract class KiwixViewModelModule
