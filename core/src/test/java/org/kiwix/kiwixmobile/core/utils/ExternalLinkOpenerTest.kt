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

package org.kiwix.kiwixmobile.core.utils

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.toast

internal class ExternalLinkOpenerTest {
  private val sharedPreferenceUtil: SharedPreferenceUtil = mockk()
  private val alertDialogShower: AlertDialogShower = mockk()
  private val intent: Intent = mockk()
  private val activity: Activity = mockk()

  @Test
  internal fun `requestOpenLink is called if external link popup preference is true`() {
    every { intent.resolveActivity(activity.packageManager) } returns mockk()
    every { sharedPreferenceUtil.prefExternalLinkPopup } returns true
    val externalLinkOpener =
      spyk(
        ExternalLinkOpener(activity, sharedPreferenceUtil, alertDialogShower),
        recordPrivateCalls = true
      )
    externalLinkOpener.openExternalUrl(intent)
    verify { externalLinkOpener["requestOpenLink"](intent) }
  }

  @Test
  internal fun `openLink is called if external link popup preference is false`() {
    every { intent.resolveActivity(activity.packageManager) } returns mockk()
    every { sharedPreferenceUtil.prefExternalLinkPopup } returns false
    val externalLinkOpener =
      spyk(
        ExternalLinkOpener(activity, sharedPreferenceUtil, alertDialogShower),
        recordPrivateCalls = true
      )
    externalLinkOpener.openExternalUrl(intent)
    verify { externalLinkOpener["openLink"](intent) }
  }

  @Test
  internal fun `toast if packageManager is null`() {
    every { intent.resolveActivity(activity.packageManager) } returns null
    val externalLinkOpener =
      spyk(
        ExternalLinkOpener(activity, sharedPreferenceUtil, alertDialogShower),
        recordPrivateCalls = true
      )
    mockkStatic(Toast::class)
    every {
      Toast.makeText(activity, R.string.no_reader_application_installed, Toast.LENGTH_LONG).show()
    } just Runs
    externalLinkOpener.openExternalUrl(intent)
    verify { activity.toast(R.string.no_reader_application_installed) }
  }
}
