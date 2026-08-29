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

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * Swaps in the generated [KiwixHiltTestApplication_Application] instead of
 * [org.kiwix.kiwixmobile.KiwixApp] so instrumented tests annotated with `@HiltAndroidTest` can
 * obtain a test-specific Hilt component instead of the real one. That generated application
 * still extends [org.kiwix.kiwixmobile.core.CoreApp] (see [KiwixHiltTestApplication]), so
 * `CoreApp.onCreate()` runs the same way it does in production. Wired up via
 * `testInstrumentationRunner` in `app/build.gradle.kts`.
 */
class HiltTestRunner : AndroidJUnitRunner() {
  override fun newApplication(
    cl: ClassLoader?,
    className: String?,
    context: Context?
  ): Application =
    super.newApplication(cl, KiwixHiltTestApplication_Application::class.java.name, context)
}
