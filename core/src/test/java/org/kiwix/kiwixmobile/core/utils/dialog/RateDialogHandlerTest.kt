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
import io.mockk.every
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import androidx.lifecycle.lifecycleScope
import org.kiwix.kiwixmobile.core.compat.CompatHelper
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.getPackageInformation
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.NetworkUtils

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
    coEvery { kiwixDataStore.rateAppCount } returns flowOf(0)
    coEvery { kiwixDataStore.rateAppDownloadCompleted } returns flowOf(false)
    coEvery { kiwixDataStore.rateAppReadingCount } returns flowOf(0)
    libkiwixBookOnDisk = mockk(relaxed = true)
    mockkObject(CompatHelper.Companion)
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
    val result = rateDialogHandler.shouldShowRateDialog(20)
    assertFalse(result)
  }

  @Test
  fun `shouldShowRateDialog returns true when all conditions are met`() = runTest {
    rateDialogHandler = spyk(rateDialogHandler)
    coEvery { rateDialogHandler.isPlayStoreVariant() } returns true
    every { rateDialogHandler.isTwoWeekPassed() } returns true
    coEvery { rateDialogHandler.isZimFilesAvailableInLibrary() } returns true

    val result = rateDialogHandler.shouldShowRateDialog(20)
    assertTrue(result)
  }

  @Test
  fun `shouldShowRateDialog returns false when visit count is less than 20`() = runTest {
    rateDialogHandler = spyk(rateDialogHandler)
    coEvery { rateDialogHandler.isPlayStoreVariant() } returns true
    every { rateDialogHandler.isTwoWeekPassed() } returns true
    coEvery { rateDialogHandler.isZimFilesAvailableInLibrary() } returns true

    val result = rateDialogHandler.shouldShowRateDialog(19)
    assertFalse(result)
  }

  @Test
  fun `shouldShowRateDialog returns false when two weeks have not passed`() = runTest {
    rateDialogHandler = spyk(rateDialogHandler)
    coEvery { rateDialogHandler.isPlayStoreVariant() } returns true
    every { rateDialogHandler.isTwoWeekPassed() } returns false
    coEvery { rateDialogHandler.isZimFilesAvailableInLibrary() } returns true

    val result = rateDialogHandler.shouldShowRateDialog(20)
    assertFalse(result)
  }

  @Test
  fun `shouldShowRateDialog returns false when no zim files are available`() = runTest {
    rateDialogHandler = spyk(rateDialogHandler)
    coEvery { rateDialogHandler.isPlayStoreVariant() } returns true
    every { rateDialogHandler.isTwoWeekPassed() } returns true
    coEvery { rateDialogHandler.isZimFilesAvailableInLibrary() } returns false

    val result = rateDialogHandler.shouldShowRateDialog(20)
    assertFalse(result)
  }

  @Test
  fun `checkForRateDialog increments visit count`() = runTest {
    coEvery { kiwixDataStore.incrementRateAppVisitCount() } returns 6

    rateDialogHandler.checkForRateDialog()

    coVerify { kiwixDataStore.incrementRateAppVisitCount() }
  }

  @Test
  fun `checkForRateDialog launches review flow when all conditions are met and network is available`() =
    runTest {
      coEvery { kiwixDataStore.incrementRateAppVisitCount() } returns 20

      rateDialogHandler = spyk(rateDialogHandler)
      coEvery { rateDialogHandler.shouldShowRateDialog(20) } returns true
      every { rateDialogHandler.launchInAppReviewFlow() } returns Unit

      rateDialogHandler.checkForRateDialog()

      verify { rateDialogHandler.launchInAppReviewFlow() }
      coVerify { kiwixDataStore.resetRateAppTriggers() }
    }

  @Test
  fun `checkForRateDialog does not launch review flow when network is unavailable`() = runTest {
    coEvery { kiwixDataStore.incrementRateAppVisitCount() } returns 20
    every { NetworkUtils.isNetworkAvailable(any()) } returns false

    rateDialogHandler = spyk(rateDialogHandler)
    coEvery { rateDialogHandler.shouldShowRateDialog(20) } returns true
    every { rateDialogHandler.launchInAppReviewFlow() } returns Unit

    rateDialogHandler.checkForRateDialog()

    verify(exactly = 0) { rateDialogHandler.launchInAppReviewFlow() }
    coVerify(exactly = 0) { kiwixDataStore.resetRateAppTriggers() }
  }

  @Test
  fun `checkForRateDialog does not launch review flow when shouldShowRateDialog is false`() =
    runTest {
      coEvery { kiwixDataStore.incrementRateAppVisitCount() } returns 6

      rateDialogHandler = spyk(rateDialogHandler)
      coEvery { rateDialogHandler.shouldShowRateDialog(6) } returns false
      every { rateDialogHandler.launchInAppReviewFlow() } returns Unit

      rateDialogHandler.checkForRateDialog()

      verify(exactly = 0) { rateDialogHandler.launchInAppReviewFlow() }
      coVerify(exactly = 0) { kiwixDataStore.resetRateAppTriggers() }
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
    coEvery { kiwixDataStore.rateAppDownloadCompleted } returns flowOf(true)

    rateDialogHandler = spyk(rateDialogHandler)
    coEvery { rateDialogHandler.isPlayStoreVariant() } returns true
    every { rateDialogHandler.isTwoWeekPassed() } returns true
    coEvery { rateDialogHandler.isZimFilesAvailableInLibrary() } returns true

    val result = rateDialogHandler.shouldShowRateDialog(5)
    assertTrue(result)
  }

  @Test
  fun `shouldShowRateDialog returns true when readingCount is greater than or equal to threshold`() = runTest {
    coEvery { kiwixDataStore.rateAppReadingCount } returns flowOf(10)

    rateDialogHandler = spyk(rateDialogHandler)
    coEvery { rateDialogHandler.isPlayStoreVariant() } returns true
    every { rateDialogHandler.isTwoWeekPassed() } returns true
    coEvery { rateDialogHandler.isZimFilesAvailableInLibrary() } returns true

    val result = rateDialogHandler.shouldShowRateDialog(5)
    assertTrue(result)
  }
}
