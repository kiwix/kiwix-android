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

package org.kiwix.kiwixmobile.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.utils.ExternalLinkOpener
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import java.net.URL

internal class ExternalLinkOpenerTest {
  private val kiwixDataStore: KiwixDataStore = mockk()
  private val alertDialogShower: AlertDialogShower = mockk(relaxed = true)
  private val intent: Intent = mockk()
  private val activity: Activity = mockk()
  private val coroutineScope = CoroutineScope(Dispatchers.Main)

  @Test
  internal fun alertDialogShowerOpensLinkIfConfirmButtonIsClicked() = runTest {
    every { intent.resolveActivity(activity.packageManager) } returns mockk()
    every { kiwixDataStore.externalLinkPopup } returns flowOf(true)
    val url = URL("https://github.com/")
    every { intent.data } returns Uri.parse(url.toString())
    val lambdaSlot = slot<() -> Unit>()
    val externalLinkOpener = ExternalLinkOpener(activity, kiwixDataStore).apply {
      setAlertDialogShower(alertDialogShower)
    }
    externalLinkOpener.openExternalUrl(intent, lifecycleScope = coroutineScope)
    coVerify {
      alertDialogShower.show(
        KiwixDialog.ExternalLinkPopup,
        capture(lambdaSlot),
        any(),
        any()
      )
    }
    lambdaSlot.captured.invoke()
    verify { activity.startActivity(intent) }
  }

  @Test
  internal fun alertDialogShowerOpensLinkIfGeoProtocolAdded() = runTest {
    every { intent.resolveActivity(activity.packageManager) } returns mockk()
    every { kiwixDataStore.externalLinkPopup } returns flowOf(true)
    val uri = Uri.parse("geo:28.61388888888889,77.20833333333334")
    every { intent.data } returns uri
    val lambdaSlot = slot<() -> Unit>()
    val externalLinkOpener = ExternalLinkOpener(activity, kiwixDataStore).apply {
      setAlertDialogShower(alertDialogShower)
    }
    externalLinkOpener.openExternalUrl(intent, lifecycleScope = coroutineScope)
    coVerify {
      alertDialogShower.show(
        KiwixDialog.ExternalLinkPopup,
        capture(lambdaSlot),
        any(),
        any()
      )
    }
    lambdaSlot.captured.invoke()
    verify { activity.startActivity(intent) }
  }

  @Test
  internal fun alertDialogShowerDoesNoOpenLinkIfNegativeButtonIsClicked() = runTest {
    every { intent.resolveActivity(activity.packageManager) } returns mockk()
    every { kiwixDataStore.externalLinkPopup } returns flowOf(true)
    every { intent.data } returns Uri.parse("https://github.com/")
    val lambdaSlot = slot<() -> Unit>()
    val externalLinkOpener = ExternalLinkOpener(activity, kiwixDataStore).apply {
      setAlertDialogShower(alertDialogShower)
    }
    externalLinkOpener.openExternalUrl(intent, lifecycleScope = coroutineScope)
    coVerify {
      alertDialogShower.show(
        KiwixDialog.ExternalLinkPopup,
        any(),
        capture(lambdaSlot),
        any()
      )
    }
    lambdaSlot.captured.invoke()
    verify(exactly = 0) { activity.startActivity(intent) }
  }

  @Test
  internal fun alertDialogShowerOpensLinkAndSavesPreferencesIfNeutralButtonIsClicked() = runTest {
    every { intent.resolveActivity(activity.packageManager) } returns mockk()
    every { kiwixDataStore.externalLinkPopup } returns flowOf(true)
    every { intent.data } returns Uri.parse("https://github.com/")
    val lambdaSlot = slot<() -> Unit>()
    val externalLinkOpener = ExternalLinkOpener(activity, kiwixDataStore).apply {
      setAlertDialogShower(alertDialogShower)
    }
    externalLinkOpener.openExternalUrl(intent, lifecycleScope = coroutineScope)
    coVerify {
      alertDialogShower.show(
        KiwixDialog.ExternalLinkPopup,
        any(),
        any(),
        capture(lambdaSlot)
      )
    }
    lambdaSlot.captured.invoke()
    coVerify {
      kiwixDataStore.setExternalLinkPopup(false)
      activity.startActivity(intent)
    }
  }

  @Test
  internal fun intentIsStartedIfExternalLinkPopupPreferenceIsFalse() = runTest {
    every { intent.resolveActivity(activity.packageManager) } returns mockk()
    every { kiwixDataStore.externalLinkPopup } returns flowOf(false)
    val externalLinkOpener = ExternalLinkOpener(activity, kiwixDataStore).apply {
      setAlertDialogShower(alertDialogShower)
    }
    externalLinkOpener.openExternalUrl(intent, lifecycleScope = coroutineScope)
    coVerify { activity.startActivity(intent) }
  }

  @Test
  internal fun toastIfPackageManagerIsNull() = runTest {
    every { intent.resolveActivity(activity.packageManager) } returns null
    val externalLinkOpener = ExternalLinkOpener(activity, kiwixDataStore).apply {
      setAlertDialogShower(alertDialogShower)
    }
    mockkStatic(Toast::class)
    justRun {
      Toast.makeText(activity, R.string.no_reader_application_installed, Toast.LENGTH_LONG).show()
    }
    externalLinkOpener.openExternalUrl(intent, lifecycleScope = coroutineScope)
    coVerify { activity.toast(R.string.no_reader_application_installed) }
  }
}
