/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */

package org.kiwix.kiwixmobile.zim_manager

import android.app.Application
import com.jraska.livedata.test
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Scheduler
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.InstantExecutorExtension
import org.kiwix.kiwixmobile.database.newdb.dao.NewBookDao
import org.kiwix.kiwixmobile.database.newdb.dao.NewDownloadDao
import org.kiwix.kiwixmobile.database.newdb.dao.NewLanguagesDao
import org.kiwix.kiwixmobile.downloader.Downloader
import org.kiwix.kiwixmobile.downloader.model.BookOnDisk
import org.kiwix.kiwixmobile.downloader.model.DownloadItem
import org.kiwix.kiwixmobile.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.downloader.model.DownloadState
import org.kiwix.kiwixmobile.downloader.model.DownloadStatus
import org.kiwix.kiwixmobile.downloader.model.UriToFileConverter
import org.kiwix.kiwixmobile.network.KiwixService
import org.kiwix.kiwixmobile.utils.BookUtils
import org.kiwix.kiwixmobile.zim_manager.Fat32Checker.FileSystemState
import org.kiwix.kiwixmobile.zim_manager.NetworkState.NOT_CONNECTED
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.StorageObserver
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.Language
import java.io.File
import java.util.concurrent.TimeUnit.SECONDS

@ExtendWith(InstantExecutorExtension::class)
class ZimManageViewModelTest {

  private val newDownloadDao: NewDownloadDao = mockk()
  private val newBookDao: NewBookDao = mockk()
  private val newLanguagesDao: NewLanguagesDao = mockk()
  private val downloader: Downloader = mockk()
  private val storageObserver: StorageObserver = mockk()
  private val kiwixService: KiwixService = mockk()
  private val application: Application = mockk()
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver = mockk()
  private val bookUtils: BookUtils = mockk()
  private val fat32Checker: Fat32Checker = mockk()
  private val uriToFileConverter: UriToFileConverter = mockk()
  lateinit var viewModel: ZimManageViewModel

  private val downloads: PublishProcessor<List<DownloadModel>> = PublishProcessor.create()
  private val books: PublishProcessor<List<BookOnDisk>> = PublishProcessor.create()
  private val languages: PublishProcessor<List<Language>> = PublishProcessor.create()
  private val fileSystemStates: PublishProcessor<FileSystemState> = PublishProcessor.create()
  private val networkStates: PublishProcessor<NetworkState> = PublishProcessor.create()

  private val testScheduler = TestScheduler()

  init {
    setScheduler(testScheduler)
  }

  private fun setScheduler(replacementScheduler: Scheduler) {
    RxJavaPlugins.setIoSchedulerHandler { scheduler -> replacementScheduler }
    RxJavaPlugins.setComputationSchedulerHandler { scheduler -> replacementScheduler }
    RxJavaPlugins.setNewThreadSchedulerHandler { scheduler -> replacementScheduler }
    RxAndroidPlugins.setInitMainThreadSchedulerHandler { scheduler -> Schedulers.trampoline() }
  }

  @AfterAll
  fun teardown() {
    RxJavaPlugins.reset()
    RxAndroidPlugins.reset();
  }

  @BeforeEach
  fun init() {
    clearMocks(
        newDownloadDao, newBookDao, newLanguagesDao, downloader,
        storageObserver, kiwixService, application, connectivityBroadcastReceiver, bookUtils,
        fat32Checker, uriToFileConverter
    )
    every { connectivityBroadcastReceiver.action } returns "test"
    every { newDownloadDao.downloads() } returns downloads
    every { newBookDao.books() } returns books
    every { newLanguagesDao.languages() } returns languages
    every { fat32Checker.fileSystemStates } returns fileSystemStates
    every { connectivityBroadcastReceiver.networkStates } returns networkStates
    every { application.registerReceiver(any(), any()) } returns mockk()
    viewModel = ZimManageViewModel(
        newDownloadDao, newBookDao, newLanguagesDao, downloader,
        storageObserver, kiwixService, application, connectivityBroadcastReceiver, bookUtils,
        fat32Checker, uriToFileConverter
    )
    testScheduler.triggerActions()
  }

  @Nested
  inner class Context {
    @Test
    fun `registers broadcastReceiver in init`() {
      verify {
        application.registerReceiver(connectivityBroadcastReceiver, any())
      }
    }

    @Test
    fun `unregisters broadcastReceiver in onCleared`() {
      every { application.unregisterReceiver(any()) } returns mockk()
      viewModel.onClearedExposed()
      verify {
        application.unregisterReceiver(connectivityBroadcastReceiver)
      }
    }
  }

  @Nested
  inner class Downloads {
    @Test
    fun `on emission from database query and render downloads`() {
      val expectedStatus = DownloadStatus()
      expectStatus(expectedStatus)
      viewModel.downloadItems
          .test()
          .assertValue(listOf(DownloadItem(expectedStatus)))
          .dispose()
    }

    @Test
    fun `on emission of successful status create a book and delete the download`() {
      every { newBookDao.insert(any()) } returns Unit
      every { newDownloadDao.delete(any()) } returns Unit
      every { uriToFileConverter.convert(any()) } returns File("test")
      val expectedStatus = DownloadStatus(
          downloadId = 10L,
          state = DownloadState.Successful
      )
      expectStatus(expectedStatus)
      val element = expectedStatus.toBookOnDisk(uriToFileConverter)
      verify {
        newBookDao.insert(listOf(element))
        newDownloadDao.delete(10L)
      }
    }

    private fun expectStatus(expectedStatus: DownloadStatus) {
      val downloadList = listOf(DownloadModel())
      every { downloader.queryStatus(downloadList) } returns listOf(expectedStatus)
      downloads.offer(downloadList)
      testScheduler.triggerActions()
      testScheduler.advanceTimeBy(1, SECONDS)
      testScheduler.triggerActions()
    }
  }

  @Nested
  inner class Books {
    @Test
    fun `emissions from DB sorted by title and observed`() {
      books.onNext(
          listOf(
              BookOnDisk().apply { book.title = "z" },
              BookOnDisk().apply { book.title = "a" }
          )
      )
      testScheduler.triggerActions()
      viewModel.bookItems.test()
          .assertValue(
              listOf(
                  BookOnDisk().apply { book.title = "a" },
                  BookOnDisk().apply { book.title = "z" }
              )
          )
          .dispose()
    }
  }

  @Test
  fun `network states observed`() {
    networkStates.offer(NOT_CONNECTED)
    viewModel.networkStates.test()
        .assertValue(NOT_CONNECTED)
        .dispose()
  }
}