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

package org.kiwix.kiwixmobile.core.utils.dialog

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.LifecycleCoroutineScope
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.compat.CompatHelper
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.getPackageInformation
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.NetworkUtils
import io.mockk.every
import io.mockk.coEvery
import io.mockk.mockkConstructor
import io.mockk.verify
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@ExperimentalCoroutinesApi
class RateDialogHandlerTest {
  private lateinit var activity: CoreMainActivity
  private lateinit var kiwixDataStore: KiwixDataStore
  private lateinit var libkiwixBookOnDisk: LibkiwixBookOnDisk
  private lateinit var rateDialogHandler: RateDialogHandler
  private lateinit var packageManager: PackageManager

  @BeforeEach
  fun setup() {
    activity = mockk(relaxed = true)
    packageManager = mockk(relaxed = true)
    every { activity.packageManager } returns packageManager
    every { activity.packageName } returns "org.kiwix.kiwixmobile"
    kiwixDataStore = mockk(relaxed = true)
    coEvery { kiwixDataStore.isPlayStoreBuild } returns flowOf(true)
    libkiwixBookOnDisk = mockk(relaxed = true)
    mockkObject(CompatHelper.Companion)
    mockkConstructor(RateAppCounter::class)
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(any()) } returns true

    rateDialogHandler =
      RateDialogHandler(activity, libkiwixBookOnDisk, kiwixDataStore)

    mockkStatic("androidx.lifecycle.LifecycleOwnerKt")
    val mockLifecycleScope = mockk<LifecycleCoroutineScope>(relaxed = true)
    every { mockLifecycleScope.coroutineContext } returns UnconfinedTestDispatcher()
    every { activity.lifecycleScope } returns mockLifecycleScope
  }

  @AfterEach
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `shouldShowRateDialog returns false for non-playStore variant`() = runTest {
    coEvery { kiwixDataStore.isPlayStoreBuild } returns flowOf(false)
    rateDialogHandler = spyk(rateDialogHandler)
    val result = rateDialogHandler.shouldShowRateDialog()
    assertFalse(result)
  }

  @Test
  fun `shouldShowRateDialog returns false for standalone variant`() = runTest {
    // Standalone variant should never show the rate dialog since there is
    // no Play Store listing for the .standalone package.
    coEvery { kiwixDataStore.isPlayStoreBuild } returns flowOf(false)
    every { activity.packageName } returns "org.kiwix.kiwixmobile.standalone"
    rateDialogHandler = spyk(rateDialogHandler)
    val result = rateDialogHandler.shouldShowRateDialog()
    assertFalse(result)
  }

  @Test
  fun `shouldShowRateDialog returns true when all conditions are met`() = runTest {
    every { anyConstructed<RateAppCounter>().count } returns 19
    every { anyConstructed<RateAppCounter>().noThanksState } returns false

    rateDialogHandler = spyk(rateDialogHandler)
    coEvery { rateDialogHandler.isPlayStoreVariant() } returns true
    every { rateDialogHandler.isTwoWeekPassed() } returns true
    coEvery { rateDialogHandler.isZimFilesAvailableInLibrary() } returns true

    rateDialogHandler.checkForRateDialog(0)

    val result = rateDialogHandler.shouldShowRateDialog()
    assertTrue(result)
  }

  @Test
  fun `shouldShowRateDialog returns false when visit count is less than 20`() = runTest {
    every { anyConstructed<RateAppCounter>().count } returns 18 // Increments to 19 (less than 20)
    every { anyConstructed<RateAppCounter>().noThanksState } returns false

    rateDialogHandler = spyk(rateDialogHandler)
    coEvery { rateDialogHandler.isPlayStoreVariant() } returns true
    every { rateDialogHandler.isTwoWeekPassed() } returns true
    coEvery { rateDialogHandler.isZimFilesAvailableInLibrary() } returns true

    rateDialogHandler.checkForRateDialog(0)

    val result = rateDialogHandler.shouldShowRateDialog()
    assertFalse(result)
  }

  @Test
  fun `shouldShowRateDialog returns false when noThanksState is true`() = runTest {
    every { anyConstructed<RateAppCounter>().count } returns 19
    every { anyConstructed<RateAppCounter>().noThanksState } returns true

    rateDialogHandler = spyk(rateDialogHandler)
    coEvery { rateDialogHandler.isPlayStoreVariant() } returns true
    every { rateDialogHandler.isTwoWeekPassed() } returns true
    coEvery { rateDialogHandler.isZimFilesAvailableInLibrary() } returns true

    rateDialogHandler.checkForRateDialog(0)

    val result = rateDialogHandler.shouldShowRateDialog()
    assertFalse(result)
  }

  @Test
  fun `shouldShowRateDialog returns false when two weeks have not passed`() = runTest {
    every { anyConstructed<RateAppCounter>().count } returns 19
    every { anyConstructed<RateAppCounter>().noThanksState } returns false

    rateDialogHandler = spyk(rateDialogHandler)
    coEvery { rateDialogHandler.isPlayStoreVariant() } returns true
    every { rateDialogHandler.isTwoWeekPassed() } returns false
    coEvery { rateDialogHandler.isZimFilesAvailableInLibrary() } returns true

    rateDialogHandler.checkForRateDialog(0)

    val result = rateDialogHandler.shouldShowRateDialog()
    assertFalse(result)
  }

  @Test
  fun `shouldShowRateDialog returns false when no zim files are available`() = runTest {
    every { anyConstructed<RateAppCounter>().count } returns 19
    every { anyConstructed<RateAppCounter>().noThanksState } returns false

    rateDialogHandler = spyk(rateDialogHandler)
    coEvery { rateDialogHandler.isPlayStoreVariant() } returns true
    every { rateDialogHandler.isTwoWeekPassed() } returns true
    coEvery { rateDialogHandler.isZimFilesAvailableInLibrary() } returns false

    rateDialogHandler.checkForRateDialog(0)

    val result = rateDialogHandler.shouldShowRateDialog()
    assertFalse(result)
  }

  @Test
  fun `checkForRateDialog increments visit count`() = runTest {
    every { anyConstructed<RateAppCounter>().count } returns 5
    every { anyConstructed<RateAppCounter>().noThanksState } returns false

    rateDialogHandler.checkForRateDialog(0)

    verify { anyConstructed<RateAppCounter>().count = 6 }
  }

  @Test
  fun `checkForRateDialog shows rate dialog when all conditions are met and network is available`() =
    runTest {
      every { anyConstructed<RateAppCounter>().count } returns 19 // increments to 20
      every { anyConstructed<RateAppCounter>().noThanksState } returns false

      rateDialogHandler = spyk(rateDialogHandler)
      coEvery { rateDialogHandler.isPlayStoreVariant() } returns true
      every { rateDialogHandler.isTwoWeekPassed() } returns true
      coEvery { rateDialogHandler.isZimFilesAvailableInLibrary() } returns true

      val alertDialogShower = mockk<AlertDialogShower>(relaxed = true)
      rateDialogHandler.setAlertDialogShower(alertDialogShower)

      rateDialogHandler.checkForRateDialog(0)

      verify { alertDialogShower.show(any(), any(), any(), any()) }
    }

  @Test
  fun `checkForRateDialog does not show rate dialog when network is unavailable`() = runTest {
    every { anyConstructed<RateAppCounter>().count } returns 19
    every { anyConstructed<RateAppCounter>().noThanksState } returns false

    rateDialogHandler = spyk(rateDialogHandler)
    coEvery { rateDialogHandler.isPlayStoreVariant() } returns true
    every { rateDialogHandler.isTwoWeekPassed() } returns true
    coEvery { rateDialogHandler.isZimFilesAvailableInLibrary() } returns true

    every { NetworkUtils.isNetworkAvailable(any()) } returns false

    val alertDialogShower = mockk<AlertDialogShower>(relaxed = true)
    rateDialogHandler.setAlertDialogShower(alertDialogShower)

    rateDialogHandler.checkForRateDialog(0)

    verify(exactly = 0) { alertDialogShower.show(any(), any(), any(), any()) }
  }

  @Test
  fun `checkForRateDialog does not show rate dialog when shouldShowRateDialog is false`() =
    runTest {
      every { anyConstructed<RateAppCounter>().count } returns 5 // Less than 20
      every { anyConstructed<RateAppCounter>().noThanksState } returns false

      rateDialogHandler = spyk(rateDialogHandler)
      coEvery { rateDialogHandler.isPlayStoreVariant() } returns true
      every { rateDialogHandler.isTwoWeekPassed() } returns true
      coEvery { rateDialogHandler.isZimFilesAvailableInLibrary() } returns true

      val alertDialogShower = mockk<AlertDialogShower>(relaxed = true)
      rateDialogHandler.setAlertDialogShower(alertDialogShower)

      rateDialogHandler.checkForRateDialog(0)

      verify(exactly = 0) { alertDialogShower.show(any(), any(), any(), any()) }
    }

  @Test
  fun `isPlayStoreVariant returns true for playStore build`() = runTest {
    coEvery { kiwixDataStore.isPlayStoreBuild } returns flowOf(true)
    val result = rateDialogHandler.isPlayStoreVariant()
    assertTrue(result)
  }

  @Test
  fun `isPlayStoreVariant returns false for non-playStore build`() = runTest {
    coEvery { kiwixDataStore.isPlayStoreBuild } returns flowOf(false)
    val result = rateDialogHandler.isPlayStoreVariant()
    assertFalse(result)
  }

  @Test
  fun `isZimFilesAvailableInLibrary returns true when isBrandedApp is true`() =
    runTest {
      with(mockk<ActivityExtensions>()) {
        every { activity.packageName } returns "org.kiwix.kiwixcustom"
        every { activity.isBrandedApp() } returns true
        val result = rateDialogHandler.isZimFilesAvailableInLibrary()
        assertTrue(result)
      }
    }

  @Test
  fun `isZimFilesAvailableInLibrary returns false when no books and isBrandedApp is false`() =
    runTest {
      with(mockk<ActivityExtensions>()) {
        every { activity.packageName } returns "org.kiwix.kiwixmobile"
        every { activity.isBrandedApp() } returns false
        coEvery { libkiwixBookOnDisk.getBooks() } returns emptyList()
        val result = rateDialogHandler.isZimFilesAvailableInLibrary()
        assertFalse(result)
      }
    }

  @Test
  fun `isZimFilesAvailableInLibrary returns true when books available and isBrandedApp is false`() =
    runTest {
      with(mockk<ActivityExtensions>()) {
        every { activity.packageName } returns "org.kiwix.kiwixmobile"
        every { activity.isBrandedApp() } returns false
        coEvery { libkiwixBookOnDisk.getBooks() } returns listOf(mockk())
        val result = rateDialogHandler.isZimFilesAvailableInLibrary()
        assertTrue(result)
      }
    }

  @Test
  fun `isTwoWeekPassed returns true when install time is over two weeks ago`() {
    val twoWeeksInMillis = 14 * 24 * 60 * 60 * 1000L
    val installTime = System.currentTimeMillis() - twoWeeksInMillis - 1000L
    val packageInfo = PackageInfo().apply { firstInstallTime = installTime }
    every {
      packageManager.getPackageInformation(any<String>(), any<Int>())
    } returns packageInfo
    val result = rateDialogHandler.isTwoWeekPassed()
    assertTrue(result)
  }

  @Test
  fun `isTwoWeekPassed returns false when install time is less than two weeks ago`() {
    val installTime = System.currentTimeMillis() - 1000L
    val packageInfo = PackageInfo().apply { firstInstallTime = installTime }
    every {
      packageManager.getPackageInformation(any<String>(), any<Int>())
    } returns packageInfo
    val result = rateDialogHandler.isTwoWeekPassed()
    assertFalse(result)
  }

  @Test
  fun `shouldShowRateDialog returns true when downloadCompletedState is true`() = runTest {
    every { anyConstructed<RateAppCounter>().count } returns 5 // Less than 20
    every { anyConstructed<RateAppCounter>().noThanksState } returns false
    every { anyConstructed<RateAppCounter>().downloadCompletedState } returns true

    rateDialogHandler = spyk(rateDialogHandler)
    coEvery { rateDialogHandler.isPlayStoreVariant() } returns true
    every { rateDialogHandler.isTwoWeekPassed() } returns true
    coEvery { rateDialogHandler.isZimFilesAvailableInLibrary() } returns true

    val result = rateDialogHandler.shouldShowRateDialog()
    assertTrue(result)
  }

  @Test
  fun `shouldShowRateDialog returns true when readingCount is greater than or equal to threshold`() = runTest {
    every { anyConstructed<RateAppCounter>().count } returns 5 // Less than 20
    every { anyConstructed<RateAppCounter>().noThanksState } returns false
    every { anyConstructed<RateAppCounter>().downloadCompletedState } returns false
    every { anyConstructed<RateAppCounter>().readingCount } returns 10 // Equal to threshold

    rateDialogHandler = spyk(rateDialogHandler)
    coEvery { rateDialogHandler.isPlayStoreVariant() } returns true
    every { rateDialogHandler.isTwoWeekPassed() } returns true
    coEvery { rateDialogHandler.isZimFilesAvailableInLibrary() } returns true

    val result = rateDialogHandler.shouldShowRateDialog()
    assertTrue(result)
  }

  @Test
  fun `Later click resets all triggers`() = runTest {
    every { anyConstructed<RateAppCounter>().count } returns 19
    every { anyConstructed<RateAppCounter>().noThanksState } returns false

    val alertDialogShower = mockk<AlertDialogShower>(relaxed = true)
    rateDialogHandler.setAlertDialogShower(alertDialogShower)

    rateDialogHandler = spyk(rateDialogHandler)
    coEvery { rateDialogHandler.isPlayStoreVariant() } returns true
    every { rateDialogHandler.isTwoWeekPassed() } returns true
    coEvery { rateDialogHandler.isZimFilesAvailableInLibrary() } returns true

    rateDialogHandler.checkForRateDialog(0)

    val laterCallbackSlot = io.mockk.slot<() -> Unit>()
    verify { alertDialogShower.show(any(), any(), any(), capture(laterCallbackSlot)) }

    laterCallbackSlot.captured.invoke()

    verify { anyConstructed<RateAppCounter>().resetTriggers() }
  }
}
