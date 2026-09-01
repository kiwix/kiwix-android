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

package org.kiwix.kiwixmobile.testutils

import dagger.hilt.android.testing.CustomTestApplication
import org.kiwix.kiwixmobile.core.CoreApp

/**
 * Generates a Hilt test [android.app.Application] (`KiwixHiltTestApplication_Application`)
 * that still extends [CoreApp], so `CoreApp.onCreate()` runs for instrumented tests exactly
 * as it does for the real app - setting `CoreApp.instance`, constructing `JNIKiwix` (which
 * loads libkiwix's native libraries), and initializing `ThemeConfig`/`FileLogger`. Plain
 * `HiltTestApplication` does not extend `CoreApp`, so all of that was silently skipped;
 * tests that reached `CoreApp.instance` or a native `Library`/`Manager`/`Book` before
 * something else happened to trigger it would fail.
 */
@CustomTestApplication(CoreApp::class)
interface KiwixHiltTestApplication
