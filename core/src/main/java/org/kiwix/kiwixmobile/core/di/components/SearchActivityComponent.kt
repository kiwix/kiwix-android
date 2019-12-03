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

import dagger.Subcomponent
import org.kiwix.kiwixmobile.core.di.ActivityScope
import org.kiwix.kiwixmobile.core.search.SearchActivity

// @subcomponent lets Dagger know this interface is a Dagger subcomponent
@ActivityScope
@Subcomponent
interface SearchActivityComponent {

  // subcomponent builder creates instance of this subcomponent
  @Subcomponent.Builder
  interface Builder {
    fun build(): SearchActivityComponent
  }

  // inject tells Dagger that SearchActivity and all its dependencies requests injection from search
  // activityComponent.  This subcomponent object graph needs to satisfy all dependencies which the injected
  // fields require.
  fun inject(searchActivity: SearchActivity)
}
