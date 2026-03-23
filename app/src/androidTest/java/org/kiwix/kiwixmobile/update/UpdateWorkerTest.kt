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

package org.kiwix.kiwixmobile.update

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.dao.DownloadApkDao
import org.kiwix.kiwixmobile.core.dao.entities.DownloadApkEntity
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.data.remote.update.Channel
import org.kiwix.kiwixmobile.core.data.remote.update.Items
import org.kiwix.kiwixmobile.core.data.remote.update.UpdateFeed
import org.kiwix.kiwixmobile.core.di.modules.KIWIX_UPDATE_URL
import org.kiwix.kiwixmobile.core.utils.workManager.UpdateWorkManager

@RunWith(AndroidJUnit4::class)
class UpdateWorkerTest {
  private lateinit var context: Context
  private lateinit var apkDao: DownloadApkDao
  private lateinit var kiwixService: KiwixService
  private lateinit var worker: UpdateWorkManager

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    apkDao = mockk(relaxed = true)
    kiwixService = mockk()
    worker = buildWorker()
  }

  @After
  fun tearDown() {
    unmockkObject(KiwixService.ServiceCreator)
  }

  @Test
  fun doWork_returnsSuccess_andInsertsApkInfo_whenNoPreviousDownloadExists() {
    runBlocking {
      coEvery { kiwixService.getUpdates() } returns updateFeed(
        title = "Kiwix Android 100.100.100"
      )
      coEvery { apkDao.getDownload() } returns null

      val result = worker.doWork()

      assertThat(result, `is`(ListenableWorker.Result.success()))
      verify(exactly = 1) {
        KiwixService.ServiceCreator.newHackListService(any(), KIWIX_UPDATE_URL)
      }
      verify(exactly = 1) {
        apkDao.addApkInfoItem(
          match {
            it.name == "Kiwix Android 100.100.100" &&
              it.version == "100.100.100" &&
              it.url == APK_URL
          }
        )
      }
      verify(exactly = 0) { apkDao.addLatestAppVersion(any()) }
    }
  }

  @Test
  fun doWork_returnsSuccess_andUpdatesExistingVersion_whenPreviousDownloadExists() {
    runBlocking {
      coEvery { kiwixService.getUpdates() } returns updateFeed(
        title = "Kiwix Android 101.0.0"
      )
      coEvery { apkDao.getDownload() } returns existingDownload()

      val result = worker.doWork()

      assertThat(result, `is`(ListenableWorker.Result.success()))
      verify { apkDao.addLatestAppVersion("101.0.0") }
      verify { apkDao.addApkInfoItem(any()) }
    }
  }

  @Test
  fun doWork_returnsFailure_whenFeedContainsNoItems() {
    runBlocking {
      coEvery { kiwixService.getUpdates() } returns UpdateFeed()

      val result = worker.doWork()

      assertThat(result, `is`(ListenableWorker.Result.failure()))
      verify { apkDao.addApkInfoItem(any()) }
      verify { apkDao.addLatestAppVersion(any()) }
    }
  }

  @Test
  fun doWork_returnsFailure_whenFetchingUpdatesThrows() {
    runBlocking {
      coEvery { kiwixService.getUpdates() } throws RuntimeException("network error")

      val result = worker.doWork()

      assertThat(result, `is`(ListenableWorker.Result.failure()))
      verify { apkDao.addApkInfoItem(any()) }
      verify { apkDao.addLatestAppVersion(any()) }
    }
  }

  private fun buildWorker(): UpdateWorkManager =
    TestListenableWorkerBuilder<UpdateWorkManager>(context)
      .setWorkerFactory(
        object : WorkerFactory() {
          override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
          ): ListenableWorker? {
            return if (workerClassName == UpdateWorkManager::class.java.name) {
              UpdateWorkManager(appContext, workerParameters, kiwixService, apkDao)
            } else {
              null
            }
          }
        }
      )
      .build()

  private fun updateFeed(title: String): UpdateFeed {
    val item = Items().apply {
      this.title = title
      link = APK_URL
    }
    val channel = Channel().apply {
      items = listOf(item)
    }
    return UpdateFeed().apply {
      this.channel = channel
    }
  }

  private fun existingDownload() = DownloadApkEntity(
    name = "Kiwix Android 99.0.0",
    version = "99.0.0",
    url = APK_URL
  )

  private companion object {
    const val APK_URL = "https://download.kiwix.org/kiwix.apk"
  }
}
