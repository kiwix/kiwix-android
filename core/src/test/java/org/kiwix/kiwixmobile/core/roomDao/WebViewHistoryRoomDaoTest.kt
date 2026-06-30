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

package org.kiwix.kiwixmobile.core.roomDao

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.dao.WebViewHistoryRoomDao
import org.kiwix.kiwixmobile.core.dao.entities.WebViewHistoryEntity
import org.kiwix.kiwixmobile.core.data.KiwixRoomDatabase
import org.kiwix.kiwixmobile.core.data.RoomDowngradeBackupHelper
import org.kiwix.sharedFunctions.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], application = TestApplication::class)
class WebViewHistoryRoomDaoTest {
  private lateinit var kiwixRoomDatabase: KiwixRoomDatabase
  private lateinit var webViewHistoryRoomDao: WebViewHistoryRoomDao
  private lateinit var databaseFile: File

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    databaseFile = context.getDatabasePath(RoomDowngradeBackupHelper.DB_NAME)

    kiwixRoomDatabase = Room.inMemoryDatabaseBuilder(context, KiwixRoomDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    webViewHistoryRoomDao = kiwixRoomDatabase.webViewHistoryRoomDao()
  }

  @After
  fun tearDown() {
    kiwixRoomDatabase.close()
    KiwixRoomDatabase.destroyInstance()
    cleanupDatabase()
  }

  private fun cleanupDatabase() {
    if (databaseFile.exists()) {
      databaseFile.deleteRecursively()
    }
  }

  @Test
  fun testInsertWebViewPageHistoryItems() = runTest {
    val entities = listOf(
      createEntity(zimId = "zim1", webViewIndex = 1),
      createEntity(zimId = "zim2", webViewIndex = 0)
    )

    webViewHistoryRoomDao.getAllWebViewPagesHistory().test {
      assertThat(awaitItem()).isEmpty()

      webViewHistoryRoomDao.insertWebViewPageHistoryItems(entities)

      val history = awaitItem()
      assertThat(history).hasSize(2)
      assertThat(history[0].zimId).isEqualTo("zim2")
      assertThat(history[1].zimId).isEqualTo("zim1")

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun testGetAllWebViewPagesHistoryWhenEmpty() = runTest {
    webViewHistoryRoomDao.getAllWebViewPagesHistory().test {
      assertThat(awaitItem()).isEmpty()

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun testClearWebViewPagesHistory() = runTest {
    webViewHistoryRoomDao.getAllWebViewPagesHistory().test {
      assertThat(awaitItem()).isEmpty()

      webViewHistoryRoomDao.insertWebViewPageHistoryItem(createEntity())
      assertThat(awaitItem()).hasSize(1)

      webViewHistoryRoomDao.clearWebViewPagesHistory()
      assertThat(awaitItem()).isEmpty()

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun testClearPageHistoryWithPrimaryKey() = runTest {
    webViewHistoryRoomDao.getAllWebViewPagesHistory().test {
      assertThat(awaitItem()).isEmpty()

      webViewHistoryRoomDao.insertWebViewPageHistoryItem(createEntity())
      assertThat(awaitItem()).hasSize(1)

      webViewHistoryRoomDao.clearPageHistoryWithPrimaryKey()
      assertThat(awaitItem()).isEmpty()

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun testResetPrimaryKey() = runTest {
    val entity = createEntity()

    webViewHistoryRoomDao.getAllWebViewPagesHistory().test {
      assertThat(awaitItem()).isEmpty()

      // First insertion ID should be 1
      webViewHistoryRoomDao.insertWebViewPageHistoryItem(entity)
      val history1 = awaitItem()
      assertThat(history1).hasSize(1)
      assertThat(history1[0].id).isEqualTo(1L)

      // Clear and insert again ID should be 2 because primary key auto-increments
      webViewHistoryRoomDao.clearWebViewPagesHistory()
      assertThat(awaitItem()).isEmpty()

      webViewHistoryRoomDao.insertWebViewPageHistoryItem(entity)
      val history2 = awaitItem()
      assertThat(history2).hasSize(1)
      assertThat(history2[0].id).isEqualTo(2L)

      // Clear and Reset Primary Key
      webViewHistoryRoomDao.clearWebViewPagesHistory()
      assertThat(awaitItem()).isEmpty()
      webViewHistoryRoomDao.resetPrimaryKey()

      // Insert again ID should be 1 again
      webViewHistoryRoomDao.insertWebViewPageHistoryItem(entity)
      val history3 = awaitItem()
      assertThat(history3).hasSize(1)
      assertThat(history3[0].id).isEqualTo(1L)

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun testBundleTypeConverter() = runTest {
    val bundle = Bundle().apply {
      putString("key", "value")
    }
    val entity = createEntity(bundle = bundle)

    webViewHistoryRoomDao.getAllWebViewPagesHistory().test {
      assertThat(awaitItem()).isEmpty()

      webViewHistoryRoomDao.insertWebViewPageHistoryItem(entity)

      val history = awaitItem()
      assertThat(history).hasSize(1)
      val retrievedBundle = history[0].webViewBackForwardListBundle

      assertThat(retrievedBundle).isNotNull
      assertThat(retrievedBundle?.getString("key")).isEqualTo("value")

      cancelAndIgnoreRemainingEvents()
    }
  }

  private fun createEntity(
    zimId: String = "kiwix-zim",
    webViewIndex: Int = 0,
    position: Int = 0,
    bundle: Bundle? = null
  ) = WebViewHistoryEntity(
    zimId = zimId,
    webViewIndex = webViewIndex,
    webViewCurrentPosition = position,
    webViewBackForwardListBundle = bundle
  )
}
