/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.di.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import org.kiwix.kiwixmobile.core.ViewModelFactory
import org.kiwix.kiwixmobile.core.di.ViewModelKey
import org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.BookmarkViewModel
import org.kiwix.kiwixmobile.core.page.history.viewmodel.HistoryViewModel
import org.kiwix.kiwixmobile.core.page.notes.viewmodel.NotesViewModel
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchViewModel

@Module
abstract class CoreViewModelModule {
  @Binds
  @IntoMap
  @ViewModelKey(SearchViewModel::class)
  abstract fun bindSearchViewModel(searchViewModel: SearchViewModel): ViewModel

  @Binds
  abstract fun bindViewModelFactory(factory: ViewModelFactory):
    ViewModelProvider.Factory

  @Binds
  @IntoMap
  @ViewModelKey(HistoryViewModel::class)
  abstract fun bindHistoryViewModel(historyViewModel: HistoryViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(NotesViewModel::class)
  abstract fun bindNotesViewModel(notesViewModel: NotesViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(BookmarkViewModel::class)
  abstract fun bindBookmarksViewModel(bookmarksViewModel: BookmarkViewModel): ViewModel
}
