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

package org.kiwix.sharedFunctions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
  val dispatcher: TestDispatcher = StandardTestDispatcher()
) :
  TestWatcher(), BeforeEachCallback, AfterEachCallback {
  private var isMainOverridden = false

  override fun starting(description: Description?) {
    if (shouldOverrideMain()) {
      Dispatchers.setMain(dispatcher)
      isMainOverridden = true
    }
  }

  override fun finished(description: Description?) {
    if (isMainOverridden) {
      Dispatchers.resetMain()
      isMainOverridden = false
    }
  }

  override fun beforeEach(context: ExtensionContext?) {
    if (shouldOverrideMain()) {
      Dispatchers.setMain(dispatcher)
      isMainOverridden = true
    }
  }

  override fun afterEach(context: ExtensionContext?) {
    if (isMainOverridden) {
      Dispatchers.resetMain()
      isMainOverridden = false
    }
  }

  private fun shouldOverrideMain(): Boolean {
    return try {
      val runtime = System.getProperty("java.runtime.name")
      val isAndroidRuntime = runtime?.contains("Android", ignoreCase = true) == true
      if (isAndroidRuntime) {
        val fingerprint = android.os.Build.FINGERPRINT
        fingerprint?.contains("robolectric", ignoreCase = true) == true
      } else {
        true
      }
    } catch (_: Throwable) {
      true
    }
  }
}
