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

package org.kiwix.kiwixmobile.core.di

import javax.inject.Qualifier

/**
 * Whether this build flavor should show spell-checked search suggestions. Bound once per
 * flavor (see `KiwixModule`/`BrandedModule`) and injected both into [org.kiwix.kiwixmobile
 * .core.main.reader.CoreReaderViewModel] and into code outside the reader view model
 * hierarchy (e.g. [org.kiwix.kiwixmobile.core.search.viewmodel.SearchViewModel] restoring
 * the archive after process death), so both paths open the ZIM file with the same flag
 * instead of guessing a default.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ShowSearchSuggestionsSpellChecked
