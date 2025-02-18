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

package org.kiwix.kiwixmobile

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import io.objectbox.Box
import io.objectbox.BoxStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.dao.entities.BookmarkEntity
import org.kiwix.kiwixmobile.core.data.remote.ObjectBoxToLibkiwixMigrator
import org.kiwix.kiwixmobile.core.di.modules.DatabaseModule
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.utils.KiwixIdlingResource
import org.kiwix.libkiwix.Book
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

class ObjectBoxToLibkiwixMigratorTest : BaseActivityTest() {
  private val objectBoxToLibkiwixMigrator = ObjectBoxToLibkiwixMigrator()

  // take the existing boxStore object
  private var boxStore: BoxStore? = null
  private lateinit var zimFile: File
  private lateinit var box: Box<BookmarkEntity>
  private val expectedZimName = "Alpine_Linux"
  private val expectedZimId = "60094d1e-1c9a-a60b-2011-4fb02f8db6c3"
  private val expectedZimFilePath: String by lazy { zimFile.canonicalPath }
  private val expectedTitle = "Installing"
  private val expectedBookmarkUrl = "https://alpine_linux/InstallingPage"
  private val expectedFavicon = ""
  private val bookmarkEntity: BookmarkEntity by lazy {
    BookmarkEntity(
      0,
      expectedZimId,
      expectedZimName,
      expectedZimFilePath,
      ZimReaderSource(File(expectedZimFilePath)),
      expectedBookmarkUrl,
      expectedTitle,
      expectedFavicon
    )
  }

  @Rule
  @JvmField
  var retryRule = RetryRule()

  @Before
  override fun waitForIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (TestUtils.isSystemUINotRespondingDialogVisible(this)) {
        TestUtils.closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
      putBoolean(SharedPreferenceUtil.IS_PLAY_STORE_BUILD, true)
      putString(SharedPreferenceUtil.PREF_LANG, "en")
      putLong(
        SharedPreferenceUtil.PREF_LAST_DONATION_POPUP_SHOWN_IN_MILLISECONDS,
        System.currentTimeMillis()
      )
    }
    activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
      onActivity {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        it.navigate(R.id.libraryFragment)
      }
    }
    boxStore = DatabaseModule.boxStore
    CoreApp.coreComponent.inject(objectBoxToLibkiwixMigrator)
    setUpObjectBoxAndData()
  }

  private fun setUpObjectBoxAndData() {
    if (boxStore == null) {
      throw RuntimeException(
        "BoxStore is not available for testing," +
          " check is your application running"
      )
    }
    box = boxStore!!.boxFor(BookmarkEntity::class.java)

    // clear the data before running the test case
    clearBookmarks()

    // add a file in fileSystem because we need to actual file path for making object of Archive.
    val loadFileStream =
      ObjectBoxToLibkiwixMigratorTest::class.java.classLoader.getResourceAsStream("testzim.zim")
    zimFile = File(
      context.getExternalFilesDirs(null)[0],
      "testzim.zim"
    )
    if (zimFile.exists()) zimFile.delete()
    zimFile.createNewFile()
    loadFileStream.use { inputStream ->
      val outputStream: OutputStream = FileOutputStream(zimFile)
      outputStream.use { it ->
        val buffer = ByteArray(inputStream.available())
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
          it.write(buffer, 0, length)
        }
      }
    }
  }

  @Test
  fun testSingleDataMigration(): Unit = runBlocking {
    box.put(bookmarkEntity)
    // migrate data into room database
    objectBoxToLibkiwixMigrator.migrateBookMarks(box)
    // check if data successfully migrated to room
    val actualDataAfterMigration =
      objectBoxToLibkiwixMigrator.libkiwixBookmarks.bookmarks().blockingFirst()
    assertEquals(1, actualDataAfterMigration.size)
    assertEquals(actualDataAfterMigration[0].zimReaderSource?.toDatabase(), expectedZimFilePath)
    assertEquals(actualDataAfterMigration[0].zimId, expectedZimId)
    assertEquals(actualDataAfterMigration[0].title, expectedTitle)
    assertEquals(actualDataAfterMigration[0].url, expectedBookmarkUrl)
    // Clear the bookmarks list from device to not affect the other test cases.
    clearBookmarks()
  }

  @Test
  fun testMigrationWithEmptyData(): Unit = runBlocking {
    // Migrate data from empty ObjectBox database
    objectBoxToLibkiwixMigrator.migrateBookMarks(box)
    val actualDataAfterMigration =
      objectBoxToLibkiwixMigrator.libkiwixBookmarks.bookmarks().blockingFirst()
    assertTrue(actualDataAfterMigration.isEmpty())
    // Clear the bookmarks list from device to not affect the other test cases.
    clearBookmarks()
  }

  @Test
  fun testMigrationWithExistingData(): Unit = runBlocking {
    // Test if data successfully migrated to Room and existing data is preserved
    val existingTitle = "Home Page"
    val existingBookmarkUrl = "https://alpine_linux/HomePage"
    val secondBookmarkEntity = BookmarkEntity(
      0,
      expectedZimId,
      expectedZimName,
      expectedZimFilePath,
      ZimReaderSource(File(expectedZimFilePath)),
      existingBookmarkUrl,
      existingTitle,
      expectedFavicon
    )
    val libkiwixBook = Book()
    objectBoxToLibkiwixMigrator.libkiwixBookmarks.saveBookmark(
      LibkiwixBookmarkItem(
        secondBookmarkEntity,
        libkiwixBook
      )
    )
    box.put(bookmarkEntity)
    // Migrate data into Room database
    objectBoxToLibkiwixMigrator.migrateBookMarks(box)
    val actualDataAfterMigration =
      objectBoxToLibkiwixMigrator.libkiwixBookmarks.bookmarks().blockingFirst()
    assertEquals(2, actualDataAfterMigration.size)
    val existingItem =
      actualDataAfterMigration.find {
        it.url == existingBookmarkUrl && it.title == existingTitle
      }
    assertNotNull(existingItem)
    val newItem =
      actualDataAfterMigration.find {
        it.url == expectedBookmarkUrl && it.title == expectedTitle
      }
    assertNotNull(newItem)
    // Clear the bookmarks list from device to not affect the other test cases.
    clearBookmarks()
  }

  @Test
  fun testLargeDataMigration(): Unit = runBlocking {
    // Test large data migration for recent searches
    for (i in 1..1000) {
      box.put(
        BookmarkEntity(
          0,
          expectedZimId,
          expectedZimName,
          expectedZimFilePath,
          ZimReaderSource(File(expectedZimFilePath)),
          "https://alpine_linux/search_$i",
          "title_$i",
          expectedFavicon
        )
      )
    }
    // Migrate data into Room database
    objectBoxToLibkiwixMigrator.migrateBookMarks(box)
    // Check if data successfully migrated to Room
    val actualDataAfterMigration =
      objectBoxToLibkiwixMigrator.libkiwixBookmarks.bookmarks().blockingFirst()
    assertEquals(1000, actualDataAfterMigration.size)
    // Clear the bookmarks list from device to not affect the other test cases.
    clearBookmarks()
  }

  @Test
  fun testMigrationForNewCustomApps(): Unit = runBlocking {
    val expectedZimId = "60094d1e-1c9a-a60b-2011"
    val bookmarkEntity = BookmarkEntity(
      0,
      expectedZimId,
      expectedZimName,
      null,
      null,
      expectedBookmarkUrl,
      expectedTitle,
      expectedFavicon
    )
    box.put(bookmarkEntity)
    // migrate data into room database
    objectBoxToLibkiwixMigrator.migrateBookMarks(box)
    // check if data successfully migrated to room
    val actualDataAfterMigration =
      objectBoxToLibkiwixMigrator.libkiwixBookmarks.bookmarks().blockingFirst()
    assertEquals(1, actualDataAfterMigration.size)
    assertEquals(actualDataAfterMigration[0].zimReaderSource?.toDatabase(), null)
    assertEquals(actualDataAfterMigration[0].zimId, expectedZimId)
    assertEquals(actualDataAfterMigration[0].title, expectedTitle)
    assertEquals(actualDataAfterMigration[0].url, expectedBookmarkUrl)
    // Clear the bookmarks list from device to not affect the other test cases.
    clearBookmarks()
  }

  @Test
  fun testMigrationForNonExistingFiles(): Unit = runBlocking {
    val expectedZimId = "60094d1e-1c9a-a60b-2011-a60b"
    val nonExistingPath = "storage/Download/demo/demo.zim"
    val bookmarkEntity = BookmarkEntity(
      0,
      expectedZimId,
      expectedZimName,
      nonExistingPath,
      ZimReaderSource(File(nonExistingPath)),
      expectedBookmarkUrl,
      expectedTitle,
      expectedFavicon
    )
    box.put(bookmarkEntity)
    // migrate data into room database
    objectBoxToLibkiwixMigrator.migrateBookMarks(box)
    // check if data successfully migrated to room
    val actualDataAfterMigration =
      objectBoxToLibkiwixMigrator.libkiwixBookmarks.bookmarks().blockingFirst()
    assertEquals(1, actualDataAfterMigration.size)
    assertEquals(actualDataAfterMigration[0].zimReaderSource?.toDatabase(), null)
    assertEquals(actualDataAfterMigration[0].zimId, expectedZimId)
    assertEquals(actualDataAfterMigration[0].title, expectedTitle)
    assertEquals(actualDataAfterMigration[0].url, expectedBookmarkUrl)
    // Clear the bookmarks list from device to not affect the other test cases.
    clearBookmarks()
  }

  private fun clearBookmarks() {
    // delete bookmarks for testing other edge cases
    objectBoxToLibkiwixMigrator.libkiwixBookmarks.deleteBookmarks(
      objectBoxToLibkiwixMigrator.libkiwixBookmarks.bookmarks()
        .blockingFirst() as List<LibkiwixBookmarkItem>
    )
    box.removeAll()
    if (::zimFile.isInitialized) {
      zimFile.delete() // delete the temp ZIM file to free up the memory
    }
  }

  @After
  fun finish() {
    IdlingRegistry.getInstance().unregister(KiwixIdlingResource.getInstance())
    TestUtils.deleteTemporaryFilesOfTestCases(context)
  }

  companion object {

    @BeforeClass
    fun beforeClass() {
      IdlingPolicies.setMasterPolicyTimeout(180, TimeUnit.SECONDS)
      IdlingPolicies.setIdlingResourceTimeout(180, TimeUnit.SECONDS)
      IdlingRegistry.getInstance().register(KiwixIdlingResource.getInstance())
    }
  }
}
