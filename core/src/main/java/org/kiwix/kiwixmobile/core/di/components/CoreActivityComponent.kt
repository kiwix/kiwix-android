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

package org.kiwix.kiwixmobile.core.di.components

import android.app.Activity
import dagger.BindsInstance
import dagger.Subcomponent
import org.kiwix.kiwixmobile.core.di.ActivityScope
import org.kiwix.kiwixmobile.core.di.modules.ActivityModule
import org.kiwix.kiwixmobile.core.main.AddNoteDialog
import org.kiwix.kiwixmobile.core.page.bookmark.BookmarksFragment
import org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.effects.ShowDeleteBookmarksDialog
import org.kiwix.kiwixmobile.core.page.history.HistoryFragment
import org.kiwix.kiwixmobile.core.page.history.viewmodel.effects.ShowDeleteHistoryDialog
import org.kiwix.kiwixmobile.core.search.SearchActivity
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.ShowDeleteSearchDialog
import org.kiwix.kiwixmobile.core.settings.CorePrefsFragment

@ActivityScope
@Subcomponent(modules = [ActivityModule::class])
interface CoreActivityComponent {
  fun inject(searchActivity: SearchActivity)
  fun inject(showDeleteSearchDialog: ShowDeleteSearchDialog)
  fun inject(showDeleteBookmarksDialog: ShowDeleteBookmarksDialog)
  fun inject(showDeleteHistoryDialog: ShowDeleteHistoryDialog)
  fun inject(corePrefsFragment: CorePrefsFragment)
  fun inject(historyActivity: HistoryFragment)
  fun inject(bookmarksActivity: BookmarksFragment)
  fun inject(addNoteDialog: AddNoteDialog)

  @Subcomponent.Builder
  interface Builder {
    @BindsInstance
    fun activity(activity: Activity): CoreActivityComponent.Builder

    fun build(): CoreActivityComponent
  }
}
