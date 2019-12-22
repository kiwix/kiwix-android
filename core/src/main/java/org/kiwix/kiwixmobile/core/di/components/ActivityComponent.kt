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

import android.app.Activity
import dagger.BindsInstance
import dagger.Subcomponent
import org.kiwix.kiwixmobile.core.bookmark.BookmarksActivity
import org.kiwix.kiwixmobile.core.bookmark.BookmarksModule
import org.kiwix.kiwixmobile.core.di.ActivityScope
import org.kiwix.kiwixmobile.core.di.modules.ActivityModule
import org.kiwix.kiwixmobile.core.history.HistoryActivity
import org.kiwix.kiwixmobile.core.history.HistoryModule
import org.kiwix.kiwixmobile.core.webserver.ZimHostActivity
import org.kiwix.kiwixmobile.core.webserver.ZimHostModule

@ActivityScope
@Subcomponent(
  modules = [
    BookmarksModule::class,
    HistoryModule::class,
    ZimHostModule::class,
    ActivityModule::class]
)
interface ActivityComponent {

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance fun activity(activity: Activity): Builder

    fun build(): ActivityComponent
  }

  fun inject(zimHostActivity: ZimHostActivity)
  fun inject(historyActivity: HistoryActivity)
  fun inject(bookmarksActivity: BookmarksActivity)
}
