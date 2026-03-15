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

package org.kiwix.kiwixmobile.core.utils.files

import android.app.Activity
import android.content.pm.PackageManager
import io.mockk.Called
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.dao.DownloadApkDao
import org.kiwix.kiwixmobile.core.data.KiwixRoomDatabase
import org.kiwix.kiwixmobile.core.main.THREE_DAYS_IN_MILLISECONDS
import org.kiwix.kiwixmobile.core.main.UpdateDialogHandler
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore

@ExperimentalCoroutinesApi
class UpdateDialogHandlerTest {
  private lateinit var activity: Activity
  private lateinit var kiwixRoomDatabase: KiwixRoomDatabase
  private lateinit var apkDao: DownloadApkDao
  private lateinit var kiwixDataStore: KiwixDataStore
  private lateinit var updateDialogHandler: UpdateDialogHandler
  private lateinit var showUpdateDialogCallback: UpdateDialogHandler.ShowUpdateDialogCallback
  private lateinit var packageManager: PackageManager

  @BeforeEach
  fun setup() {
    activity = mockk(relaxed = true)
    packageManager = mockk(relaxed = true)
    every { activity.packageManager } returns packageManager
    every { activity.packageName } returns "org.kiwix.kiwixmobile"
    kiwixDataStore = mockk(relaxed = true)
    kiwixRoomDatabase = mockk(relaxed = true)
    showUpdateDialogCallback = mockk(relaxed = true)
    apkDao = kiwixRoomDatabase.downloadApkDao()
    updateDialogHandler =
      UpdateDialogHandler(kiwixRoomDatabase.downloadApkDao(), kiwixDataStore)
    updateDialogHandler.setUpdateDialogCallBack(showUpdateDialogCallback)
  }

  @Test
  fun `should show initial update popup when update is available`() =
    runTest {
      coEvery { apkDao.getDownload()!!.lastDialogShownInMilliSeconds } returns 0L
      coEvery { apkDao.getDownload()!!.laterClickedMilliSeconds } returns 0L
      coEvery { apkDao.getDownload()!!.version } returns "100.100.100"
      coEvery { kiwixDataStore.isPlayStoreBuild } returns flowOf(false)
      updateDialogHandler.attemptToShowUpdatePopup()
      coVerify { showUpdateDialogCallback.showUpdateDialog() }
    }

  @Test
  fun `should not show initial update popup when update is available but is play store build`() =
    runTest {
      coEvery { apkDao.getDownload()!!.lastDialogShownInMilliSeconds } returns 0L
      coEvery { apkDao.getDownload()!!.laterClickedMilliSeconds } returns 0L
      coEvery { apkDao.getDownload()!!.version } returns "100.100.100"
      coEvery { kiwixDataStore.isPlayStoreBuild } returns flowOf(true)
      updateDialogHandler.attemptToShowUpdatePopup()
      coVerify { showUpdateDialogCallback wasNot Called }
    }

  @Test
  fun `should not show initial update popup when update is not available`() =
    runTest {
      coEvery { apkDao.getDownload()!!.lastDialogShownInMilliSeconds } returns 0L
      coEvery { apkDao.getDownload()!!.laterClickedMilliSeconds } returns 0L
      coEvery { apkDao.getDownload()!!.version } returns "0.0.0"
      coEvery { kiwixDataStore.isPlayStoreBuild } returns flowOf(false)
      updateDialogHandler.attemptToShowUpdatePopup()
      coVerify { showUpdateDialogCallback wasNot Called }
    }

  @Test
  fun `test should show update popup when update clicked time exceeds three days`() =
    runTest {
      updateDialogHandler = spyk(updateDialogHandler)
      val currentMilliSeconds = System.currentTimeMillis()
      coEvery { apkDao.getDownload()!!.laterClickedMilliSeconds } returns 0L
      coEvery { apkDao.getDownload()!!.version } returns "100.100.100"
      coEvery { kiwixDataStore.isPlayStoreBuild } returns flowOf(false)
      coEvery { apkDao.getDownload()!!.lastDialogShownInMilliSeconds } returns currentMilliSeconds - (THREE_DAYS_IN_MILLISECONDS + 1000)
      updateDialogHandler.attemptToShowUpdatePopup()
      coVerify { showUpdateDialogCallback.showUpdateDialog() }
    }

  @Test
  fun `test should not show update popup when update clicked time exceeds three days but is play store build`() =
    runTest {
      updateDialogHandler = spyk(updateDialogHandler)
      val currentMilliSeconds = System.currentTimeMillis()
      coEvery { apkDao.getDownload()!!.laterClickedMilliSeconds } returns 0L
      coEvery { apkDao.getDownload()!!.version } returns "100.100.100"
      coEvery { kiwixDataStore.isPlayStoreBuild } returns flowOf(true)
      coEvery { apkDao.getDownload()!!.lastDialogShownInMilliSeconds } returns currentMilliSeconds - (THREE_DAYS_IN_MILLISECONDS + 1000)
      updateDialogHandler.attemptToShowUpdatePopup()
      coVerify { showUpdateDialogCallback wasNot Called }
    }

  @Test
  fun `test should not show update popup when update clicked time is less than three days`() =
    runTest {
      updateDialogHandler = spyk(updateDialogHandler)
      val currentMilliSeconds = System.currentTimeMillis()
      coEvery { apkDao.getDownload()!!.laterClickedMilliSeconds } returns 0L
      coEvery { apkDao.getDownload()!!.version } returns "100.100.100"
      coEvery { kiwixDataStore.isPlayStoreBuild } returns flowOf(false)
      coEvery { apkDao.getDownload()!!.lastDialogShownInMilliSeconds } returns currentMilliSeconds - (THREE_DAYS_IN_MILLISECONDS / 2)
      updateDialogHandler.attemptToShowUpdatePopup()
      coVerify { showUpdateDialogCallback wasNot Called }
    }

  @Test
  fun `test should not show update popup when update clicked time is less than three days and update is not available`() =
    runTest {
      updateDialogHandler = spyk(updateDialogHandler)
      val currentMilliSeconds = System.currentTimeMillis()
      coEvery { apkDao.getDownload()!!.laterClickedMilliSeconds } returns 0L
      coEvery { apkDao.getDownload()!!.version } returns "0.0.0"
      coEvery { kiwixDataStore.isPlayStoreBuild } returns flowOf(false)
      coEvery { apkDao.getDownload()!!.lastDialogShownInMilliSeconds } returns currentMilliSeconds - (THREE_DAYS_IN_MILLISECONDS + 1000)
      updateDialogHandler.attemptToShowUpdatePopup()
      coVerify { showUpdateDialogCallback wasNot Called }
    }

  @Test
  fun `test should show update popup when later clicked time exceeds three days`() =
    runTest {
      updateDialogHandler = spyk(updateDialogHandler)
      val currentMilliSeconds = System.currentTimeMillis()
      coEvery { apkDao.getDownload()!!.lastDialogShownInMilliSeconds } returns 0L
      coEvery { apkDao.getDownload()!!.version } returns "100.100.100"
      coEvery { kiwixDataStore.isPlayStoreBuild } returns flowOf(false)
      coEvery { apkDao.getDownload()!!.laterClickedMilliSeconds } returns currentMilliSeconds - (THREE_DAYS_IN_MILLISECONDS + 1000)
      updateDialogHandler.attemptToShowUpdatePopup()
      coVerify { showUpdateDialogCallback.showUpdateDialog() }
    }

  @Test
  fun `test should not show update popup when later clicked time exceeds three days but is play store build`() =
    runTest {
      updateDialogHandler = spyk(updateDialogHandler)
      val currentMilliSeconds = System.currentTimeMillis()
      coEvery { apkDao.getDownload()!!.lastDialogShownInMilliSeconds } returns 0L
      coEvery { apkDao.getDownload()!!.version } returns "100.100.100"
      coEvery { kiwixDataStore.isPlayStoreBuild } returns flowOf(true)
      coEvery { apkDao.getDownload()!!.laterClickedMilliSeconds } returns currentMilliSeconds - (THREE_DAYS_IN_MILLISECONDS + 1000)
      updateDialogHandler.attemptToShowUpdatePopup()
      coVerify { showUpdateDialogCallback wasNot Called }
    }

  @Test
  fun `test should not show update popup when later clicked time is less than three days`() =
    runTest {
      updateDialogHandler = spyk(updateDialogHandler)
      val currentMilliSeconds = System.currentTimeMillis()
      coEvery { apkDao.getDownload()!!.lastDialogShownInMilliSeconds } returns 0L
      coEvery { apkDao.getDownload()!!.version } returns "100.100.100"
      coEvery { apkDao.getDownload()!!.lastDialogShownInMilliSeconds } returns 0L
      coEvery { apkDao.getDownload()!!.laterClickedMilliSeconds } returns currentMilliSeconds - 10000L
      updateDialogHandler.attemptToShowUpdatePopup()
      coVerify { showUpdateDialogCallback wasNot Called }
    }

  @Test
  fun `test should not show update popup when later clicked time exceeds three days and update is not available`() =
    runTest {
      updateDialogHandler = spyk(updateDialogHandler)
      val currentMilliSeconds = System.currentTimeMillis()
      coEvery { apkDao.getDownload()!!.lastDialogShownInMilliSeconds } returns 0L
      coEvery { apkDao.getDownload()!!.version } returns "0.0.0"
      coEvery { kiwixDataStore.isPlayStoreBuild } returns flowOf(false)
      coEvery { apkDao.getDownload()!!.laterClickedMilliSeconds } returns currentMilliSeconds - (THREE_DAYS_IN_MILLISECONDS + 1000)
      updateDialogHandler.attemptToShowUpdatePopup()
      coVerify { showUpdateDialogCallback wasNot Called }
    }

  @Test
  fun `update later saves the correct time`() = runTest {
    val currentMilliSeconds = System.currentTimeMillis()
    coEvery { apkDao.addLaterClickedInfo(any()) } just Runs
    updateDialogHandler.updateLater(currentMilliSeconds)
    coVerify { apkDao.addLaterClickedInfo(currentMilliSeconds) }
  }

  @Test
  fun `reset update later sets value to zero`() = runTest {
    coEvery { apkDao.addLaterClickedInfo(0L) } just Runs
    updateDialogHandler.resetUpdateLater()
    coVerify { apkDao.addLaterClickedInfo(0L) }
  }

  @Test
  fun `updateLastUpdatePopupShownTime sets correct time`() = runTest {
    coEvery { apkDao.addLastDialogShownInfo(any()) } just Runs
    val currentTime = System.currentTimeMillis()
    updateDialogHandler.updateLastUpdatePopupShownTime()
    coVerify { apkDao.addLastDialogShownInfo(more(currentTime - 1000)) }
  }

  @Test
  fun `isTimeToShowUpdate should returns false when laterClicked time is recent`() = runTest {
    val currentMillis = System.currentTimeMillis()
    coEvery { apkDao.getDownload()!!.laterClickedMilliSeconds } returns currentMillis - 1000
    val result = updateDialogHandler.isTimeToShowUpdate(currentMillis)
    assertFalse(result)
  }

  @Test
  fun `test isTimeToShowUpdate should returns true if laterClicked time is over three month`() =
    runTest {
      val currentMillis = System.currentTimeMillis()
      coEvery { apkDao.getDownload()!!.laterClickedMilliSeconds } returns currentMillis - THREE_DAYS_IN_MILLISECONDS
      val result = updateDialogHandler.isTimeToShowUpdate(currentMillis)
      assertTrue(result)
    }

  @Test
  fun `isThreeDaysElapsed should return false when lastPopupMillis is zero`() {
    val currentMillis = System.currentTimeMillis()
    val result = updateDialogHandler.isThreeDaysElapsed(currentMillis, 0L)
    assertFalse(result)
  }

  @Test
  fun `isUpdateAvailable should return false when available version is less than current version`() {
    coEvery { kiwixDataStore.isPlayStoreBuild } returns flowOf(false)
    val result = updateDialogHandler.isUpdateAvailable("0.0.0")
    assertFalse(result)
  }

  @Test
  fun `isUpdateAvailable should return true when available version is more than current version`() {
    coEvery { kiwixDataStore.isPlayStoreBuild } returns flowOf(false)
    val result = updateDialogHandler.isUpdateAvailable("100.100.100")
    assertTrue(result)
  }

  @Test
  fun `isUpdateAvailable should return false when available version is empty`() {
    coEvery { kiwixDataStore.isPlayStoreBuild } returns flowOf(false)
    val result = updateDialogHandler.isUpdateAvailable("")
    assertFalse(result)
  }

  @Test
  fun `isUpdateAvailable should return false when available version is more than current version and is play store build`() {
    coEvery { kiwixDataStore.isPlayStoreBuild } returns flowOf(true)
    val result = updateDialogHandler.isUpdateAvailable("100.100.100")
    assertFalse(result)
  }
}
