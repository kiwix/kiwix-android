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

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_kiwix_error.allowCrash
import kotlinx.android.synthetic.main.activity_kiwix_error.messageText
import kotlinx.android.synthetic.main.activity_kiwix_error.textView2
import org.kiwix.kiwixmobile.core.R

class DiagnosticReportActivity : ErrorActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    textView2.setText(R.string.diagnostic_report)
    messageText.setText(R.string.diagnostic_report_message)
    allowCrash.visibility = View.GONE
  }

  override fun restartApp() {
    finish()
  }

  override val subject = "Somebody has sent a Diagnostic Report  "

  override val initialBody = """
    Hi Kiwix Developers,
    I am having an issue with the app and would like you to check these details
    
  """.trimIndent()
}
