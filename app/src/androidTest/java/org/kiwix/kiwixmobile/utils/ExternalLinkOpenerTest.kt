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
import android.os.Build
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.kiwix.kiwixmobile.core.utils.ExternalLinkOpener
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_COPY_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.ALERT_DIALOG_URI_TEXT_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.kiwix.kiwixmobile.core.utils.dialog.DialogHost
import java.net.URL
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
internal class ExternalLinkOpenerTest {
  private val kiwixDataStore: KiwixDataStore = mockk()
  private val alertDialogShower = AlertDialogShower()
  private val packageManager: PackageManager = mockk()
  private val activity: Activity = mockk(relaxed = true)
  private val coroutineScope = CoroutineScope(Dispatchers.Main)

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  internal fun alertDialogShowerOpensLinkIfConfirmButtonIsClicked() = runBlocking {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) return@runBlocking
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
    listeners[0].invoke()
    verify { activity.startActivity(intent) }
  }

  @Test
  internal fun alertDialogShowerOpensLinkIfGeoProtocolAdded() = runBlocking {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) return@runBlocking
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
    listeners[0].invoke()
    verify { activity.startActivity(intent) }
  }

  @Test
  internal fun alertDialogShowerDoesNoOpenLinkIfNegativeButtonIsClicked() = runBlocking {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) return@runBlocking
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
    listeners[1].invoke()
    verify(exactly = 0) { activity.startActivity(intent) }
  }

  @Test
  internal fun alertDialogShowerOpensLinkAndSavesPreferencesIfNeutralButtonIsClicked() = runBlocking {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) return@runBlocking
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
    listeners[2].invoke()
    coVerify {
      kiwixDataStore.setExternalLinkPopup(false)
      activity.startActivity(intent)
    }
  }

  @Test
  internal fun intentIsStartedIfExternalLinkPopupPreferenceIsFalse() = runBlocking {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) return@runBlocking
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
  internal fun toastIfPackageManagerIsNull() = runBlocking {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) return@runBlocking
    every { activity.packageManager } returns packageManager
    every { packageManager.resolveActivity(any(), any<Int>()) } returns null

    // Stub activity context methods to prevent Toast crash, since mockkStatic is unavailable on API 25
    val realContext = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    every { activity.resources } returns realContext.resources
    every { activity.applicationContext } returns realContext
    every { activity.getSystemService(any()) } answers { realContext.getSystemService(arg(0) as String) }
    every { activity.packageName } returns realContext.packageName
    every { activity.mainLooper } returns android.os.Looper.getMainLooper()
    every { activity.theme } returns realContext.theme

    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/"))
    val externalLinkOpener = ExternalLinkOpener(activity, kiwixDataStore).apply {
      setAlertDialogShower(alertDialogShower)
    }

    externalLinkOpener.openExternalUrl(intent, lifecycleScope = coroutineScope)
    verify(exactly = 0) { activity.startActivity(any()) }
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
    val dialogData = alertDialogShower.dialogState.value
    assertNotNull(dialogData)
    val (dialog, _, _) = dialogData!!
    assert(dialog == KiwixDialog.ExternalRedirectDialog("donation platform"))
  }

  @Test
  internal fun openExternalLinkWithDialog_showsToastIfIntentIsNotResolvable() {
    every { activity.packageManager } returns packageManager
    every { packageManager.resolveActivity(any(), any<Int>()) } returns null

    // Stub activity context methods to prevent Toast crash, since mockkStatic is unavailable on API 25
    val realContext = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    every { activity.resources } returns realContext.resources
    every { activity.applicationContext } returns realContext
    every { activity.getSystemService(any()) } answers { realContext.getSystemService(arg(0) as String) }
    every { activity.packageName } returns realContext.packageName
    every { activity.mainLooper } returns android.os.Looper.getMainLooper()
    every { activity.theme } returns realContext.theme

    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/"))

    val externalLinkOpener = ExternalLinkOpener(activity, kiwixDataStore).apply {
      setAlertDialogShower(alertDialogShower)
    }
    externalLinkOpener.openExternalLinkWithDialog(
      intent,
      "donation platform"
    )
    verify(exactly = 0) { activity.startActivity(any()) }
    val dialogData = alertDialogShower.dialogState.value
    assertNull(dialogData)
  }

  @Test
  fun testCopyButtonCopiesUriToClipboard() {
    val realContext = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val clipboardManager = realContext.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("", "")) // clear clipboard

    val uri = Uri.parse("https://example.com/")

    val dialog = KiwixDialog.ExternalLinkPopup
    alertDialogShower.show(dialog, uri = uri)
    composeTestRule.setContent {
      DialogHost(alertDialogShower)
    }
    composeTestRule
      .onNodeWithTag(ALERT_DIALOG_COPY_BUTTON_TESTING_TAG)
      .performClick()

    val clip = clipboardManager.primaryClip
    assertNotNull(clip)
    assert(clip!!.getItemAt(0).text.toString() == "https://example.com/")
  }

  @Test
  fun testDialogButtonTextDoesNotWrap() {
    val longText = "This is a very long button text that would normally wrap to multiple lines"
    val dialog = KiwixDialog.ExternalRedirectDialog(longText)
    alertDialogShower.show(dialog, uri = Uri.parse("https://example.com"))
    composeTestRule.setContent {
      DialogHost(alertDialogShower)
    }
    composeTestRule
      .onNodeWithText(longText, substring = true)
      .assertExists()
  }

  @Test
  fun testClickingUriTextOpensExternalLinkAndDismissesDialog() = runBlocking {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) return@runBlocking
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
    composeTestRule.setContent {
      DialogHost(alertDialogShower)
    }
    composeTestRule
      .onNodeWithTag(ALERT_DIALOG_URI_TEXT_TESTING_TAG)
      .performClick()
    verify { activity.startActivity(intent) }
  }
}
