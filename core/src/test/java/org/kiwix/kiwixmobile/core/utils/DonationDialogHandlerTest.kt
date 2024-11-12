/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.mockk.Called
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.getPackageInformation
import org.kiwix.kiwixmobile.core.dao.NewBookDao
import org.kiwix.kiwixmobile.core.downloader.downloadManager.ZERO
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions

@ExperimentalCoroutinesApi
class DonationDialogHandlerTest {
  private lateinit var activity: Activity
  private lateinit var sharedPreferenceUtil: SharedPreferenceUtil
  private lateinit var newBookDao: NewBookDao
  private lateinit var donationDialogHandler: DonationDialogHandler
  private lateinit var showDonationDialogCallback: DonationDialogHandler.ShowDonationDialogCallback
  private lateinit var packageManager: PackageManager

  @BeforeEach
  fun setup() {
    activity = mockk(relaxed = true)
    packageManager = mockk(relaxed = true)
    every { activity.packageManager } returns packageManager
    every { activity.packageName } returns "org.kiwix.kiwixmobile"
    sharedPreferenceUtil = mockk(relaxed = true)
    newBookDao = mockk(relaxed = true)
    showDonationDialogCallback = mockk(relaxed = true)
    donationDialogHandler =
      DonationDialogHandler(activity, sharedPreferenceUtil, newBookDao)
    donationDialogHandler.setDonationDialogCallBack(showDonationDialogCallback)
  }

  @Test
  fun `should show initial donation popup when app is three month old`() = runTest {
    donationDialogHandler = spyk(donationDialogHandler)
    every { sharedPreferenceUtil.lastDonationPopupShownInMilliSeconds } returns 0L
    every { sharedPreferenceUtil.laterClickedMilliSeconds } returns 0L
    coEvery { newBookDao.getBooks() } returns listOf(mockk())
    every {
      donationDialogHandler.shouldShowInitialPopup(any())
    } returns true
    donationDialogHandler.attemptToShowDonationPopup()
    verify { showDonationDialogCallback.showDonationDialog() }
  }

  @Test
  fun `should not show donation popup if app is not three month old`() = runTest {
    every { sharedPreferenceUtil.lastDonationPopupShownInMilliSeconds } returns 0L
    coEvery { newBookDao.getBooks() } returns listOf(mockk())
    val currentMillis = System.currentTimeMillis()
    val installTime = currentMillis - 1000
    val packageInfo = PackageInfo().apply {
      firstInstallTime = installTime
    }
    every {
      packageManager.getPackageInformation(activity.packageName, ZERO)
    } returns packageInfo
    donationDialogHandler.attemptToShowDonationPopup()
    verify { showDonationDialogCallback wasNot Called }
  }

  @Test
  fun `should not show donation popup when no ZIM files available in library`() = runTest {
    every { sharedPreferenceUtil.lastDonationPopupShownInMilliSeconds } returns 0L
    coEvery { newBookDao.getBooks() } returns emptyList()
    val currentMillis = System.currentTimeMillis()
    val threeMonthsAgo = currentMillis - THREE_MONTHS_IN_MILLISECONDS
    val packageInfo = PackageInfo().apply {
      firstInstallTime = threeMonthsAgo
    }

    every { packageManager.getPackageInformation(activity.packageName, ZERO) } returns packageInfo
    donationDialogHandler.attemptToShowDonationPopup()
    verify { showDonationDialogCallback wasNot Called }
  }

  @Test
  fun `should not show popup if time since last popup is less than three month`() = runTest {
    val currentMilliSeconds = System.currentTimeMillis()
    every {
      sharedPreferenceUtil.lastDonationPopupShownInMilliSeconds
    } returns currentMilliSeconds - (THREE_MONTHS_IN_MILLISECONDS / 2)
    coEvery { newBookDao.getBooks() } returns listOf(mockk())
    donationDialogHandler.attemptToShowDonationPopup()
    verify(exactly = 0) { showDonationDialogCallback.showDonationDialog() }
  }

  @Test
  fun `should show donation popup if time since last popup exceeds three months`() = runTest {
    val currentMilliSeconds = System.currentTimeMillis()
    every {
      sharedPreferenceUtil.lastDonationPopupShownInMilliSeconds
    } returns currentMilliSeconds - (THREE_MONTHS_IN_MILLISECONDS + 1000)
    coEvery { newBookDao.getBooks() } returns listOf(mockk())
    donationDialogHandler.attemptToShowDonationPopup()
    verify { showDonationDialogCallback.showDonationDialog() }
  }

  @Test
  fun `test should show donation popup when later clicked time exceeds three months`() = runTest {
    donationDialogHandler = spyk(donationDialogHandler)
    val currentMilliSeconds = System.currentTimeMillis()
    every {
      sharedPreferenceUtil.lastDonationPopupShownInMilliSeconds
    } returns 0L
    every { donationDialogHandler.shouldShowInitialPopup(any()) } returns true
    coEvery { newBookDao.getBooks() } returns listOf(mockk())
    every {
      sharedPreferenceUtil.laterClickedMilliSeconds
    } returns currentMilliSeconds - (THREE_MONTHS_IN_MILLISECONDS + 1000)
    donationDialogHandler.attemptToShowDonationPopup()
    verify { showDonationDialogCallback.showDonationDialog() }
  }

  @Test
  fun `test should show popup if later clicked time is less than three months`() = runTest {
    donationDialogHandler = spyk(donationDialogHandler)
    val currentMilliSeconds = System.currentTimeMillis()
    every {
      sharedPreferenceUtil.lastDonationPopupShownInMilliSeconds
    } returns 0L
    every { donationDialogHandler.shouldShowInitialPopup(any()) } returns true
    coEvery { newBookDao.getBooks() } returns listOf(mockk())
    every {
      sharedPreferenceUtil.laterClickedMilliSeconds
    } returns currentMilliSeconds - 10000L
    donationDialogHandler.attemptToShowDonationPopup()
    verify { showDonationDialogCallback wasNot Called }
  }

  @Test
  fun `donate later saves the correct time`() {
    val currentMilliSeconds = System.currentTimeMillis()
    every { sharedPreferenceUtil.laterClickedMilliSeconds = any() } just Runs
    donationDialogHandler.donateLater(currentMilliSeconds)
    verify { sharedPreferenceUtil.laterClickedMilliSeconds = currentMilliSeconds }
  }

  @Test
  fun `reset donate later sets value to zero`() {
    every { sharedPreferenceUtil.laterClickedMilliSeconds = 0L } just Runs
    donationDialogHandler.resetDonateLater()
    verify { sharedPreferenceUtil.laterClickedMilliSeconds = 0L }
  }

  @Test
  fun `updateLastDonationPopupShownTime sets correct time`() {
    every { sharedPreferenceUtil.lastDonationPopupShownInMilliSeconds = any() } just Runs
    val currentTime = System.currentTimeMillis()
    donationDialogHandler.updateLastDonationPopupShownTime()
    verify { sharedPreferenceUtil.lastDonationPopupShownInMilliSeconds = more(currentTime - 1000) }
  }

  @Test
  fun `isTimeToShowDonation should returns false when laterClicked time is recent`() {
    val currentMillis = System.currentTimeMillis()
    every { sharedPreferenceUtil.laterClickedMilliSeconds } returns currentMillis - 1000
    val result = donationDialogHandler.isTimeToShowDonation(currentMillis)
    assertFalse(result)
  }

  @Test
  fun `test isTimeToShowDonation should returns true if laterClicked time is over three month`() {
    val currentMillis = System.currentTimeMillis()
    every {
      sharedPreferenceUtil.laterClickedMilliSeconds
    } returns currentMillis - THREE_MONTHS_IN_MILLISECONDS
    val result = donationDialogHandler.isTimeToShowDonation(currentMillis)
    assertTrue(result)
  }

  @Test
  fun `isThreeMonthsElapsed should return false when lastPopupMillis is zero`() {
    val currentMillis = System.currentTimeMillis()
    val result = donationDialogHandler.isThreeMonthsElapsed(currentMillis, 0L)
    assertFalse(result)
  }

  @Test
  fun `isThreeMonthsElapsed returns true when lastPopup shows three month before`() {
    val currentMillis = System.currentTimeMillis()
    val lastPopupMillis = currentMillis - THREE_MONTHS_IN_MILLISECONDS
    val result = donationDialogHandler.isThreeMonthsElapsed(currentMillis, lastPopupMillis)
    assertTrue(result)
  }

  @Test
  fun `isZimFilesAvailableInLibrary returns true when isCustomApp is true`() = runTest {
    with(mockk<ActivityExtensions>()) {
      every { activity.packageName } returns "org.kiwix.kiwixcustom"
      every { activity.isCustomApp() } returns true
      val result = donationDialogHandler.isZimFilesAvailableInLibrary()
      assertTrue(result)
    }
  }

  @Test
  fun `isZimFilesAvailableInLibrary returns false when no books and isCustomApp is false`() =
    runTest {
      with(mockk<ActivityExtensions>()) {
        every { activity.packageName } returns "org.kiwix.kiwixmobile"
        every { activity.isCustomApp() } returns false
        coEvery { newBookDao.getBooks() } returns emptyList()
        val result = donationDialogHandler.isZimFilesAvailableInLibrary()
        assertFalse(result)
      }
    }

  @Test
  fun `isZimFilesAvailableInLibrary returns true when books available and isCustomApp is false`() =
    runTest {
      with(mockk<ActivityExtensions>()) {
        every { activity.packageName } returns "org.kiwix.kiwixmobile"
        every { activity.isCustomApp() } returns false
        coEvery { newBookDao.getBooks() } returns listOf(mockk())
        val result = donationDialogHandler.isZimFilesAvailableInLibrary()
        assertTrue(result)
      }
    }
}
