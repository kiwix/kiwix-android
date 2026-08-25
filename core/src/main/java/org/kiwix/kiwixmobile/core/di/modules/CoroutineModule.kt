/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.di.MainDispatcher

// #5023: legacy singleton graph - see HiltCoreComponentBridgeModule for the Hilt-side bindings.
@DisableInstallInCheck
@Module
class CoroutineModule {
  @Provides
  @IoDispatcher
  @Suppress("InjectDispatcher")
  fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

  @Provides
  @MainDispatcher
  @Suppress("InjectDispatcher")
  fun provideMainDispatcher(): MainCoroutineDispatcher = Dispatchers.Main
}
