/*
 * Kiwix Android
 * Copyright (c) 2022 Kiwix <android.kiwix.org>
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

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.kiwix.kiwixmobile.testutils.TestUtils.RETRY_COUNT_FOR_FLAKY_TEST
import java.util.Objects

class RetryRule : TestRule {

  override fun apply(base: Statement, description: Description): Statement =
    statement(base, description)

  private fun statement(base: Statement, description: Description): Statement {
    return object : Statement() {
      @Throws(Throwable::class) override fun evaluate() {
        var caughtThrowable: Throwable? = null
        for (i in 0 until RETRY_COUNT_FOR_FLAKY_TEST) {
          try {
            base.evaluate()
            return
          } catch (t: Throwable) {
            caughtThrowable = t
            System.err.println(description.displayName + ": run " + (i + 1) + " failed.")
          }
        }
        System.err.println(
          description.displayName + ": Giving up after " +
            RETRY_COUNT_FOR_FLAKY_TEST + " failures."
        )
        throw Objects.requireNonNull(caughtThrowable!!)
      }
    }
  }
}
