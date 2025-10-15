/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.error

import org.kiwix.kiwixmobile.core.R

class DiagnosticReportActivity : ErrorActivity() {
  override val crashTitle: Int = R.string.diagnostic_report
  override val crashDescription: Int = R.string.diagnostic_report_message

  /**
   * Overrides this method to hide the `Details of The Crash`.
   * Since this screen for sending the diagnostic report.
   */
  override fun getDiagnosticDetailsItems(): List<Int> =
    listOf(
      R.string.crash_checkbox_language,
      R.string.crash_checkbox_logs,
      R.string.crash_checkbox_zimfiles,
      R.string.crash_checkbox_device,
      R.string.crash_checkbox_file_system
    )

  override fun restartApp() {
    finish()
  }

  override val subject = "Somebody has sent a Diagnostic Report  "

  override val initialBody =
    """
    Hi Kiwix Developers,
    I am having an issue with the app and would like you to check these details
    
    """.trimIndent()
}
