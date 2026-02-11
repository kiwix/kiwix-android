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
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.widget.Toast
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Test
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
  private val packageManager: PackageManager = mockk()
  private val activity: Activity = mockk()
  private val coroutineScope = CoroutineScope(Dispatchers.Main)

  @Test
  internal fun alertDialogShowerOpensLinkIfConfirmButtonIsClicked() = runTest {
    every { activity.packageManager } returns packageManager
    every { packageManager.resolveActivity(any(), any<Int>()) } returns ResolveInfo()
    every { kiwixDataStore.externalLinkPopup } returns flowOf(true)
    val url = URL("https://github.com/")
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.toString()))
    justRun { activity.startActivity(intent) }
    val externalLinkOpener = ExternalLinkOpener(activity, kiwixDataStore).apply {
      setAlertDialogShower(alertDialogShower)
    }
    externalLinkOpener.openExternalUrl(intent, lifecycleScope = coroutineScope)
    val dialogData = alertDialogShower.dialogState.value
    assertNotNull(dialogData)
    val (dialog, listeners, _) = dialogData!!
    assert(dialog == KiwixDialog.ExternalLinkPopup)
    // Invoke confirm button (index 0)
    @Suppress("UNCHECKED_CAST")
    (listeners[0] as () -> Unit).invoke()
    verify { activity.startActivity(intent) }
  }

  @Test
  internal fun alertDialogShowerOpensLinkIfGeoProtocolAdded() = runTest {
    every { activity.packageManager } returns packageManager
    every { packageManager.resolveActivity(any(), any<Int>()) } returns ResolveInfo()
    every { kiwixDataStore.externalLinkPopup } returns flowOf(true)
    val uri = Uri.parse("geo:28.61388888888889,77.20833333333334")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    justRun { activity.startActivity(intent) }
    val externalLinkOpener = ExternalLinkOpener(activity, kiwixDataStore).apply {
      setAlertDialogShower(alertDialogShower)
    }
    externalLinkOpener.openExternalUrl(intent, lifecycleScope = coroutineScope)
    val dialogData = alertDialogShower.dialogState.value
    assertNotNull(dialogData)
    val (dialog, listeners, _) = dialogData!!
    assert(dialog == KiwixDialog.ExternalLinkPopup)
    // Invoke confirm button (index 0)
    @Suppress("UNCHECKED_CAST")
    (listeners[0] as () -> Unit).invoke()
    verify { activity.startActivity(intent) }
  }

  @Test
  internal fun alertDialogShowerDoesNoOpenLinkIfNegativeButtonIsClicked() = runTest {
    every { activity.packageManager } returns packageManager
    every { packageManager.resolveActivity(any(), any<Int>()) } returns ResolveInfo()
    every { kiwixDataStore.externalLinkPopup } returns flowOf(true)
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/"))
    val externalLinkOpener = ExternalLinkOpener(activity, kiwixDataStore).apply {
      setAlertDialogShower(alertDialogShower)
    }
    externalLinkOpener.openExternalUrl(intent, lifecycleScope = coroutineScope)
    val dialogData = alertDialogShower.dialogState.value
    assertNotNull(dialogData)
    val (dialog, listeners, _) = dialogData!!
    assert(dialog == KiwixDialog.ExternalLinkPopup)
    // Invoke dismiss button (index 1)
    @Suppress("UNCHECKED_CAST")
    (listeners[1] as () -> Unit).invoke()
    verify(exactly = 0) { activity.startActivity(intent) }
  }

  @Test
  internal fun alertDialogShowerOpensLinkAndSavesPreferencesIfNeutralButtonIsClicked() = runTest {
    every { activity.packageManager } returns packageManager
    every { packageManager.resolveActivity(any(), any<Int>()) } returns ResolveInfo()
    every { kiwixDataStore.externalLinkPopup } returns flowOf(true)
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/"))
    justRun { activity.startActivity(intent) }
    val externalLinkOpener = ExternalLinkOpener(activity, kiwixDataStore).apply {
      setAlertDialogShower(alertDialogShower)
    }
    externalLinkOpener.openExternalUrl(intent, lifecycleScope = coroutineScope)
    val dialogData = alertDialogShower.dialogState.value
    assertNotNull(dialogData)
    val (dialog, listeners, _) = dialogData!!
    assert(dialog == KiwixDialog.ExternalLinkPopup)
    // Invoke neutral button (index 2)
    @Suppress("UNCHECKED_CAST")
    (listeners[2] as () -> Unit).invoke()
    coVerify {
      kiwixDataStore.setExternalLinkPopup(false)
      activity.startActivity(intent)
    }
  }

  @Test
  internal fun intentIsStartedIfExternalLinkPopupPreferenceIsFalse() = runTest {
    every { activity.packageManager } returns packageManager
    every { packageManager.resolveActivity(any(), any<Int>()) } returns ResolveInfo()
    every { kiwixDataStore.externalLinkPopup } returns flowOf(false)
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/"))
    val externalLinkOpener = ExternalLinkOpener(activity, kiwixDataStore).apply {
      setAlertDialogShower(alertDialogShower)
    }
    externalLinkOpener.openExternalUrl(intent, lifecycleScope = coroutineScope)
    coVerify { activity.startActivity(intent) }
  }

  @Test
  internal fun toastIfPackageManagerIsNull() = runTest {
    every { activity.packageManager } returns packageManager
    every { packageManager.resolveActivity(any(), any<Int>()) } returns null
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/"))
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

  @Test
  internal fun openExternalLinkWithDialog_showsDialogIfIntentIsResolvable() {
    every { activity.packageManager } returns packageManager
    every { packageManager.resolveActivity(any(), any<Int>()) } returns ResolveInfo()
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/"))
    val externalLinkOpener = ExternalLinkOpener(activity, kiwixDataStore).apply {
      setAlertDialogShower(alertDialogShower)
    }
    externalLinkOpener.openExternalLinkWithDialog(
      intent,
      "donation platform"
    )
    verify {
      alertDialogShower.show(
        KiwixDialog.ExternalRedirectDialog("donation platform"),
        any()
      )
    }
  }

  @Test
  internal fun openExternalLinkWithDialog_showsToastIfIntentIsNotResolvable() {
    every { activity.packageManager } returns packageManager
    every { packageManager.resolveActivity(any(), any<Int>()) } returns null
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/"))
    mockkStatic(Toast::class)
    justRun {
      Toast.makeText(
        activity,
        R.string.no_reader_application_installed,
        Toast.LENGTH_LONG
      ).show()
    }
    val externalLinkOpener = ExternalLinkOpener(activity, kiwixDataStore).apply {
      setAlertDialogShower(alertDialogShower)
    }
    externalLinkOpener.openExternalLinkWithDialog(
      intent,
      "donation platform"
    )
    verify { activity.toast(R.string.no_reader_application_installed) }
    verify(exactly = 0) {
      alertDialogShower.show(any(), any())
    }
  }

  @Test
  internal fun clickingUriTextOpensLinkSameAsConfirmButton() = runTest {
    every { activity.packageManager } returns packageManager
    every { packageManager.resolveActivity(any(), any<Int>()) } returns ResolveInfo()
    every { kiwixDataStore.externalLinkPopup } returns flowOf(true)

    val url = URL("https://example.com/")
    val uri = Uri.parse(url.toString())
    val intent = Intent(Intent.ACTION_VIEW, uri)

    justRun { activity.startActivity(intent) }

    val externalLinkOpener = ExternalLinkOpener(activity, kiwixDataStore).apply {
      setAlertDialogShower(alertDialogShower)
    }

    externalLinkOpener.openExternalUrl(intent, lifecycleScope = coroutineScope)

    val dialogData = alertDialogShower.dialogState.value
    assertNotNull(dialogData)
    val (dialog, listeners, _) = dialogData!!
    assert(dialog == KiwixDialog.ExternalLinkPopup)
    // Invoke confirm button (index 0) — same behavior as clicking the URI text
    @Suppress("UNCHECKED_CAST")
    (listeners[0] as () -> Unit).invoke()
    verify { activity.startActivity(intent) }
  }
}
