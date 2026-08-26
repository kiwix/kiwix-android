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

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

// TODO(#5023): temporary bridge. Hilt only provides `Context` qualified as `@ApplicationContext`,
// but this codebase's existing @Provides methods across many modules (ApplicationModule,
// DatabaseModule, DownloaderModule, JNIModule, CoreServiceModule, ...) inject plain
// unqualified `Context`, following the old manual `CoreComponent.Builder.context()` binding.
// Remove this once those injection sites are swept to request `@ApplicationContext Context`
// directly instead.
@InstallIn(SingletonComponent::class)
@Module
object HiltContextModule {
  @Provides
  fun provideContext(@ApplicationContext context: Context): Context = context
}
