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
import android.app.Application

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import androidx.compose.material3.ExperimentalMaterial3Api
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.utils.ExternalLinkOpener
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU], manifest = Config.NONE, application = TestApplication::class)
class ExternalLinkOpenerTest {
  private lateinit var kiwixDataStore: KiwixDataStore
  private val alertDialogShower = AlertDialogShower()
  private lateinit var activity: Activity
  private lateinit var activityController: ActivityController<Activity>
  private val testDispatcher = StandardTestDispatcher()
  private val coroutineScope = CoroutineScope(testDispatcher)

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    kiwixDataStore = mockk()
    activityController = Robolectric.buildActivity(Activity::class.java)
    activity = activityController.setup().get()
  }

  @After
  fun tearDown() {
    activityController.pause().stop().destroy()
    Dispatchers.resetMain()
  }

  @Test
  fun alertDialogShowerOpensLinkIfConfirmButtonIsClicked() = runBlocking {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) return@runBlocking
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/"))
    Shadows.shadowOf(activity.packageManager).addResolveInfoForIntent(
      intent,
      ResolveInfo().apply {
        activityInfo = ActivityInfo().apply {
          packageName = "com.example"
          name = "ExampleActivity"
        }
      }
    )
    every { kiwixDataStore.externalLinkPopup } returns flowOf(true)
    val externalLinkOpener = ExternalLinkOpener(activity, kiwixDataStore).apply {
      setAlertDialogShower(alertDialogShower)
    }
    externalLinkOpener.openExternalUrl(intent, lifecycleScope = coroutineScope)
    val dialogData = alertDialogShower.dialogState.value
    assertNotNull(dialogData)
    val (dialog, listeners, _) = dialogData!!
    assert(dialog == KiwixDialog.ExternalLinkPopup)
    listeners[0].invoke()
    // In Robolectric, we can check the next started activity
    val startedIntent = Shadows.shadowOf(activity).nextStartedActivity
    assertNotNull(startedIntent)
    assert(startedIntent.action == Intent.ACTION_VIEW)
    assert(startedIntent.dataString == "https://github.com/")
  }

  @Test
  fun alertDialogShowerOpensLinkIfGeoProtocolAdded() = runBlocking {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) return@runBlocking
    val uri = Uri.parse("geo:28.61388888888889,77.20833333333334")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    Shadows.shadowOf(activity.packageManager).addResolveInfoForIntent(
      intent,
      ResolveInfo().apply {
        activityInfo = ActivityInfo().apply {
          packageName = "com.example"
          name = "ExampleActivity"
        }
      }
    )
    every { kiwixDataStore.externalLinkPopup } returns flowOf(true)
    val externalLinkOpener = ExternalLinkOpener(activity, kiwixDataStore).apply {
      setAlertDialogShower(alertDialogShower)
    }
    externalLinkOpener.openExternalUrl(intent, lifecycleScope = coroutineScope)
    val dialogData = alertDialogShower.dialogState.value
    assertNotNull(dialogData)
    val (dialog, listeners, _) = dialogData!!
    assert(dialog == KiwixDialog.ExternalLinkPopup)
    listeners[0].invoke()
    val startedIntent = Shadows.shadowOf(activity).nextStartedActivity
    assertNotNull(startedIntent)
    assert(startedIntent.data == uri)
  }

  @Test
  fun alertDialogShowerDoesNoOpenLinkIfNegativeButtonIsClicked() = runBlocking {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) return@runBlocking
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/"))
    Shadows.shadowOf(activity.packageManager).addResolveInfoForIntent(
      intent,
      ResolveInfo().apply {
        activityInfo = ActivityInfo().apply {
          packageName = "com.example"
          name = "ExampleActivity"
        }
      }
    )
    every { kiwixDataStore.externalLinkPopup } returns flowOf(true)
    val externalLinkOpener = ExternalLinkOpener(activity, kiwixDataStore).apply {
      setAlertDialogShower(alertDialogShower)
    }
    externalLinkOpener.openExternalUrl(intent, lifecycleScope = coroutineScope)
    val dialogData = alertDialogShower.dialogState.value
    assertNotNull(dialogData)
    val (dialog, listeners, _) = dialogData!!
    assert(dialog == KiwixDialog.ExternalLinkPopup)
    listeners[1].invoke()
    val startedIntent = Shadows.shadowOf(activity).nextStartedActivity
    assertNull(startedIntent)
  }

  @Test
  fun alertDialogShowerOpensLinkAndSavesPreferencesIfNeutralButtonIsClicked() =
    runBlocking {
      if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) return@runBlocking
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/"))
      Shadows.shadowOf(activity.packageManager).addResolveInfoForIntent(
        intent,
        ResolveInfo().apply {
          activityInfo = ActivityInfo().apply {
            packageName = "com.example"
            name = "ExampleActivity"
          }
        }
      )
      every { kiwixDataStore.externalLinkPopup } returns flowOf(true)
      coJustRun { kiwixDataStore.setExternalLinkPopup(any()) }
      val externalLinkOpener = ExternalLinkOpener(activity, kiwixDataStore).apply {
        setAlertDialogShower(alertDialogShower)
      }
      externalLinkOpener.openExternalUrl(intent, lifecycleScope = coroutineScope)
      val dialogData = alertDialogShower.dialogState.value
      assertNotNull(dialogData)
      val (dialog, listeners, _) = dialogData!!
      assert(dialog == KiwixDialog.ExternalLinkPopup)
      listeners[2].invoke()
      testDispatcher.scheduler.advanceUntilIdle()
      coVerify {
        kiwixDataStore.setExternalLinkPopup(false)
      }
      val startedIntent = Shadows.shadowOf(activity).nextStartedActivity
      assertNotNull(startedIntent)
    }

  @Test
  fun intentIsStartedIfExternalLinkPopupPreferenceIsFalse() = runBlocking {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) return@runBlocking
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/"))
    Shadows.shadowOf(activity.packageManager).addResolveInfoForIntent(
      intent,
      ResolveInfo().apply {
        activityInfo = ActivityInfo().apply {
          packageName = "com.example"
          name = "ExampleActivity"
        }
      }
    )
    every { kiwixDataStore.externalLinkPopup } returns flowOf(false)
    val externalLinkOpener = ExternalLinkOpener(activity, kiwixDataStore).apply {
      setAlertDialogShower(alertDialogShower)
    }
    externalLinkOpener.openExternalUrl(intent, lifecycleScope = coroutineScope)
    val startedIntent = Shadows.shadowOf(activity).nextStartedActivity
    assertNotNull(startedIntent)
    assert(startedIntent.dataString == "https://github.com/")
  }

  @Test
  internal fun toastIfPackageManagerIsNull() = runBlocking {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) return@runBlocking
    // In Robolectric, PackageManager is not null, but we can make it return null for resolveActivity
    // or just not add any resolves.
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/"))
    // Don't add resolve info.
    val externalLinkOpener = ExternalLinkOpener(activity, kiwixDataStore).apply {
      setAlertDialogShower(alertDialogShower)
    }

    externalLinkOpener.openExternalUrl(intent, lifecycleScope = coroutineScope)
    val startedIntent = Shadows.shadowOf(activity).nextStartedActivity
    assertNull(startedIntent)
  }

  @Test
  fun openExternalLinkWithDialog_showsDialogIfIntentIsResolvable() = runBlocking {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) return@runBlocking
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/"))
    Shadows.shadowOf(activity.packageManager).addResolveInfoForIntent(
      intent,
      ResolveInfo().apply {
        activityInfo = ActivityInfo().apply {
          packageName = "com.example"
          name = "ExampleActivity"
        }
      }
    )
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
  fun openExternalLinkWithDialog_showsToastIfIntentIsNotResolvable() = runBlocking {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) return@runBlocking
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/"))
    // No resolve info.
    val externalLinkOpener = ExternalLinkOpener(activity, kiwixDataStore).apply {
      setAlertDialogShower(alertDialogShower)
    }
    externalLinkOpener.openExternalLinkWithDialog(
      intent,
      "donation platform"
    )
    val startedIntent = Shadows.shadowOf(activity).nextStartedActivity
    assertNull(startedIntent)
    val dialogData = alertDialogShower.dialogState.value
    assertNull(dialogData)
  }

  @Test
  fun testDialogButtonTextDoesNotWrap() {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) return
    val longText = "This is a very long button text"
    val dialog = KiwixDialog.ExternalRedirectDialog(longText)
    alertDialogShower.show(dialog, uri = Uri.parse("https://example.com"))

    val dialogData = alertDialogShower.dialogState.value
    assertNotNull(dialogData)
    assert(dialogData!!.first == dialog)
  }
}

class TestApplication : Application()
