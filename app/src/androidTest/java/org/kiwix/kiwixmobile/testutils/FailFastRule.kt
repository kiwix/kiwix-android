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

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File

class FailFastRule : TestRule {
  override fun apply(base: Statement, description: Description): Statement =
    statement(base, description)

  private fun statement(base: Statement, description: Description): Statement =
    object : Statement() {
      @Throws(Throwable::class)
      override fun evaluate() {
        val markerFile = failFastMarkerFile()
        if (markerFile.exists()) {
          throw AssumptionViolatedException(
            "Skipping ${description.displayName}: an earlier test already failed"
          )
        }
        try {
          base.evaluate()
        } catch (t: AssumptionViolatedException) {
          // A test's own Assume check (e.g. "TTS not available on this device") is an
          // intentional skip, not a failure - must not trip the fail-fast marker.
          throw t
        } catch (t: Throwable) {
          markerFile.createNewFile()
          throw t
        }
      }
    }

  companion object {
    private const val FAIL_FAST_MARKER_FILE_NAME = "fail_fast_marker"

    private fun failFastMarkerFile(): File =
      File(
        InstrumentationRegistry.getInstrumentation().targetContext.filesDir,
        FAIL_FAST_MARKER_FILE_NAME
      )
  }
}
