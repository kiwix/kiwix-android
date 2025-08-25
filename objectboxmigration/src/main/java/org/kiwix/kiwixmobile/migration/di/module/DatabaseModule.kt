/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.migration.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import io.objectbox.BoxStore
import org.kiwix.kiwixmobile.migration.entities.MyObjectBox
import javax.inject.Singleton

@Module
class DatabaseModule {
  companion object {
    var boxStore: BoxStore? = null
  }

  @Suppress("UnsafeCallOnNullableType")
  // NOT RECOMMENDED TODO use custom runner to load TestApplication
  @Provides @Singleton fun providesBoxStore(context: Context): BoxStore {
    if (boxStore == null) {
      boxStore = MyObjectBox.builder().androidContext(context).build()
    }
    return boxStore!!
  }
}
