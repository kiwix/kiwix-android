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
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.dao.NewBookDao
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions

class DonationDialogHandlerTest {
  private lateinit var activity: Activity
  private lateinit var sharedPreferenceUtil: SharedPreferenceUtil
  private lateinit var newBookDao: NewBookDao
  private lateinit var donationDialogHandler: DonationDialogHandler
  private lateinit var showDonationDialogCallback: DonationDialogHandler.ShowDonationDialogCallback

  @BeforeEach
  fun setup() {
    activity = mockk(relaxed = true)
    sharedPreferenceUtil = mockk(relaxed = true)
    newBookDao = mockk(relaxed = true)
    showDonationDialogCallback = mockk(relaxed = true)
    donationDialogHandler = DonationDialogHandler(activity, sharedPreferenceUtil, newBookDao)
    donationDialogHandler.setDonationDialogCallBack(showDonationDialogCallback)
  }

  @Test
  fun `test should show initial popup`() {
    every { sharedPreferenceUtil.lastDonationPopupShownInMilliSeconds } returns 0L
    every { newBookDao.getBooks() } returns listOf(mockk())
    donationDialogHandler.attemptToShowDonationPopup()
    verify { showDonationDialogCallback.showDonationDialog() }
  }

  @Test
  fun `test should not show popup when time difference is less than 3 months`() {
    val currentMilliSeconds = System.currentTimeMillis()
    every {
      sharedPreferenceUtil.lastDonationPopupShownInMilliSeconds
    } returns currentMilliSeconds - (THREE_MONTHS_IN_MILLISECONDS / 2)
    every { newBookDao.getBooks() } returns listOf(mockk())
    donationDialogHandler.attemptToShowDonationPopup()
    verify(exactly = 0) { showDonationDialogCallback.showDonationDialog() }
  }

  @Test
  fun `test should show popup when time difference is more than 3 months`() {
    val currentMilliSeconds = System.currentTimeMillis()
    every {
      sharedPreferenceUtil.lastDonationPopupShownInMilliSeconds
    } returns currentMilliSeconds - (THREE_MONTHS_IN_MILLISECONDS + 1000)
    every { newBookDao.getBooks() } returns listOf(mockk())
    donationDialogHandler.attemptToShowDonationPopup()
    verify { showDonationDialogCallback.showDonationDialog() }
  }

  @Test
  fun `test donate later saves the correct time`() {
    val currentMilliSeconds = System.currentTimeMillis()
    every { sharedPreferenceUtil.laterClickedMilliSeconds = any() } just Runs
    donationDialogHandler.donateLater(currentMilliSeconds)
    verify { sharedPreferenceUtil.laterClickedMilliSeconds = currentMilliSeconds }
  }

  @Test
  fun `test reset donate later sets value to zero`() {
    every { sharedPreferenceUtil.laterClickedMilliSeconds = 0L } just Runs
    donationDialogHandler.resetDonateLater()
    verify { sharedPreferenceUtil.laterClickedMilliSeconds = 0L }
  }

  @Test
  fun `test isZimFilesAvailableInLibrary returns true when isCustomApp is true`() {
    with(mockk<ActivityExtensions>()) {
      every { activity.packageName } returns "org.kiwix.kiwixcustom"
      every { activity.isCustomApp() } returns true
      val result = donationDialogHandler.isZimFilesAvailableInLibrary()
      assertTrue(result)
    }
  }

  @Test
  fun `test isZimFilesAvailableInLibrary returns false when no books and isCustomApp is false`() {
    with(mockk<ActivityExtensions>()) {
      every { activity.packageName } returns "org.kiwix.kiwixmobile"
      every { activity.isCustomApp() } returns false
      every { newBookDao.getBooks() } returns emptyList()
      val result = donationDialogHandler.isZimFilesAvailableInLibrary()
      assertFalse(result)
    }
  }

  @Test
  fun `isZimFilesAvailableInLibrary returns true when books available and isCustomApp is false`() {
    with(mockk<ActivityExtensions>()) {
      every { activity.packageName } returns "org.kiwix.kiwixmobile"
      every { activity.isCustomApp() } returns false
      every { newBookDao.getBooks() } returns listOf(mockk())
      val result = donationDialogHandler.isZimFilesAvailableInLibrary()
      assertTrue(result)
    }
  }
}
