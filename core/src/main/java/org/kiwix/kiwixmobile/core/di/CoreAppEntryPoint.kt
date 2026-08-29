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

import dagger.hilt.InstallIn
import dagger.hilt.android.EarlyEntryPoint
import dagger.hilt.components.SingletonComponent
import org.kiwix.kiwixmobile.core.JNIInitialiser
import org.kiwix.kiwixmobile.core.ThemeConfig
import org.kiwix.kiwixmobile.core.utils.files.FileLogger

/**
 * Lets [org.kiwix.kiwixmobile.core.CoreApp] fetch its dependencies via
 * [dagger.hilt.android.EarlyEntryPoints] in `onCreate()` instead of `@Inject` fields.
 * `@CustomTestApplication` (used by instrumented tests to still run `CoreApp.onCreate()`,
 * and with it native-library initialization, against a test-specific Hilt component) refuses
 * to generate a test application for any base class that declares `@Inject` fields, so those
 * fields were moved behind this entry point. It has to be an [EarlyEntryPoint] rather than a
 * plain `@EntryPoint`: Hilt's test `SingletonComponent` isn't built until a test's
 * `HiltAndroidRule` runs (it has to see that test's `@BindValue`/`@UninstallModules`
 * overrides first), which is *after* `Application.onCreate()` - a plain entry point accessed
 * from `onCreate()` would throw `IllegalStateException: The component was not created`.
 * `@EarlyEntryPoint` resolves against a separate, eagerly-built component instead, at the
 * cost of not seeing any per-test overrides - acceptable here since none of these bindings
 * are ever overridden in tests.
 */
@EarlyEntryPoint
@InstallIn(SingletonComponent::class)
internal interface CoreAppEntryPoint {
  fun themeConfig(): ThemeConfig
  fun jniInitialiser(): JNIInitialiser
  fun fileLogger(): FileLogger
}
