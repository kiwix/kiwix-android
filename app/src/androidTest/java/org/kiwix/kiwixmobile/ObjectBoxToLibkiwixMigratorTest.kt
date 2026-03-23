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
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import io.objectbox.Box
import io.objectbox.BoxStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.page.bookmark.models.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.migration.data.ObjectBoxToLibkiwixMigrator
import org.kiwix.kiwixmobile.migration.di.component.DaggerMigrationComponent
import org.kiwix.kiwixmobile.migration.entities.BookOnDiskEntity
import org.kiwix.kiwixmobile.migration.entities.BookmarkEntity
import org.kiwix.kiwixmobile.migration.entities.MyObjectBox
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.kiwixmobile.utils.KiwixIdlingResource
import org.kiwix.libkiwix.Book
import org.kiwix.libzim.Archive
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

class ObjectBoxToLibkiwixMigratorTest : BaseActivityTest() {
  private val objectBoxToLibkiwixMigrator = ObjectBoxToLibkiwixMigrator()

  // take the existing boxStore object
  private var boxStore: BoxStore? = null
  private lateinit var zimFile: File
  private lateinit var bookOnDiskBox: Box<BookOnDiskEntity>
  private lateinit var bookmarkBox: Box<BookmarkEntity>
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

  private val bookOnDiskEntity: BookOnDiskEntity by lazy {
    BookOnDiskEntity(
      id = 0,
      file = zimFile,
      zimReaderSource = ZimReaderSource(zimFile),
      bookId = expectedZimId,
      title = "",
      description = "",
      language = "",
      creator = "",
      publisher = "",
      date = "",
      url = "",
      articleCount = "",
      mediaCount = "",
      size = "",
      name = expectedZimName,
      favIcon = ""
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
    KiwixDataStore(context).apply {
      lifeCycleScope.launch {
        setWifiOnly(false)
        setIntroShown()
        setPrefLanguage("en")
        setLastDonationPopupShownInMilliSeconds(System.currentTimeMillis())
        setIsScanFileSystemDialogShown(true)
        setIsFirstRun(false)
        setIsPlayStoreBuild(true)
        setPrefIsTest(true)
      }
    }
    activityScenario =
      ActivityScenario.launch(KiwixMainActivity::class.java).apply {
        moveToState(Lifecycle.State.RESUMED)
        onActivity {
          AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
          it.navigate(KiwixDestination.Library.route)
        }
      }
    val migrationComponent = DaggerMigrationComponent.builder()
      .coreComponent(CoreApp.coreComponent)
      .build()

    // Inject dependencies into migrators
    migrationComponent.inject(objectBoxToLibkiwixMigrator)
    val testDir = File(
      InstrumentationRegistry.getInstrumentation().targetContext.filesDir,
      "objectbox-test"
    )
    boxStore = MyObjectBox.builder()
      .directory(testDir)
      .androidContext(InstrumentationRegistry.getInstrumentation().targetContext)
      .build()
    setUpObjectBoxAndData()
  }

  private fun setUpObjectBoxAndData() {
    if (boxStore == null) {
      throw RuntimeException(
        "BoxStore is not available for testing," +
          " check is your application running"
      )
    }
    bookmarkBox = boxStore!!.boxFor(BookmarkEntity::class.java)

    // clear the data before running the test case
    runBlocking { clearBookmarks() }

    bookOnDiskBox = boxStore!!.boxFor(BookOnDiskEntity::class.java)
    // clear the data before running the test case
    runBlocking { clearBookOnDisk() }

    // add a file in fileSystem because we need to actual file path for making object of Archive.
    zimFile = getZimFile("testzim.zim")
  }

  private fun getZimFile(zimFileName: String): File {
    val loadFileStream =
      ObjectBoxToLibkiwixMigratorTest::class.java.classLoader.getResourceAsStream(zimFileName)
    val zimFile =
      File(
        context.getExternalFilesDirs(null)[0],
        zimFileName
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
    return zimFile
  }

  @Test
  fun testSingleDataMigration(): Unit =
    runBlocking {
      bookmarkBox.put(bookmarkEntity)
      // migrate data into room libkiwix.
      objectBoxToLibkiwixMigrator.migrateBookMarks(bookmarkBox)
      // check if data successfully migrated to libkiwix.
      val actualDataAfterMigration =
        objectBoxToLibkiwixMigrator.libkiwixBookmarks.bookmarks().first()
      assertEquals(1, actualDataAfterMigration.size)
      assertEquals(actualDataAfterMigration[0].zimReaderSource?.toDatabase(), expectedZimFilePath)
      assertEquals(actualDataAfterMigration[0].zimId, expectedZimId)
      assertEquals(actualDataAfterMigration[0].title, expectedTitle)
      assertEquals(actualDataAfterMigration[0].url, expectedBookmarkUrl)
      // Clear the bookmarks list from device to not affect the other test cases.
      clearBookmarks()
    }

  @Test
  fun migrateBookOnDisk_ShouldInsertDataInLibkiwix(): Unit =
    runBlocking {
      // test with single entity
      bookOnDiskBox.put(bookOnDiskEntity)
      // migrate data into libkiwix
      objectBoxToLibkiwixMigrator.migrateLocalBooks(bookOnDiskBox)
      // check if data successfully migrated to libkiwix.
      var actualDataAfterMigration =
        objectBoxToLibkiwixMigrator.libkiwixBookOnDisk.getBooks()
      assertEquals(1, actualDataAfterMigration.size)
      assertEquals(
        actualDataAfterMigration[0].book.zimReaderSource.toDatabase(),
        expectedZimFilePath
      )
      assertEquals(actualDataAfterMigration[0].book.id, expectedZimId)
      assertEquals(actualDataAfterMigration[0].book.title, "Test_Zim")
      // Clear the bookOnDisk list from device to not affect the other test cases.
      clearBookOnDisk()

      // test with empty data
      objectBoxToLibkiwixMigrator.migrateLocalBooks(bookOnDiskBox)
      // check if data successfully migrated to libkiwix.
      actualDataAfterMigration =
        objectBoxToLibkiwixMigrator.libkiwixBookOnDisk.getBooks()
      assertTrue(actualDataAfterMigration.isEmpty())
      // Clear the bookOnDisk list from device to not affect the other test cases.
      clearBookOnDisk()

      // Test if data successfully migrated to libkiwix and existing data is preserved
      zimFile = getZimFile("testzim.zim")
      val secondZimFile = getZimFile("small.zim")
      val archive = Archive(secondZimFile.path)
      val book = Book().apply {
        update(archive)
      }
      objectBoxToLibkiwixMigrator.libkiwixBookOnDisk.insert(listOf(book))
      val thirdZim = getZimFile("characters_encoding.zim")
      val thirdEntity = BookOnDiskEntity(
        id = 0,
        file = thirdZim,
        zimReaderSource = ZimReaderSource(thirdZim),
        bookId = expectedZimId,
        title = "",
        description = "",
        language = "",
        creator = "",
        publisher = "",
        date = "",
        url = "",
        articleCount = "",
        mediaCount = "",
        size = "",
        name = expectedZimName,
        favIcon = ""
      )
      bookOnDiskBox.put(thirdEntity)
      bookOnDiskBox.put(bookOnDiskEntity)
      // Migrate data into libkiwix
      objectBoxToLibkiwixMigrator.migrateLocalBooks(bookOnDiskBox)
      actualDataAfterMigration =
        objectBoxToLibkiwixMigrator.libkiwixBookOnDisk.getBooks()
      assertEquals(3, actualDataAfterMigration.size)
      val existingItem =
        actualDataAfterMigration.find {
          it.book.zimReaderSource.toDatabase() == secondZimFile.path
        }
      assertNotNull(existingItem)
      val newItem =
        actualDataAfterMigration.find {
          it.book.zimReaderSource.toDatabase() == expectedZimFilePath
        }
      assertNotNull(newItem)
      // Clear the bookmarks list from device to not affect the other test cases.
      clearBookmarks()
      secondZimFile.delete()
    }

  @Test
  fun testMigrationWithEmptyData(): Unit =
    runBlocking {
      // Migrate data from empty ObjectBox database
      objectBoxToLibkiwixMigrator.migrateBookMarks(bookmarkBox)
      val actualDataAfterMigration =
        objectBoxToLibkiwixMigrator.libkiwixBookmarks.bookmarks().first()
      assertTrue(actualDataAfterMigration.isEmpty())
      // Clear the bookmarks list from device to not affect the other test cases.
      clearBookmarks()
    }

  @Test
  fun testMigrationWithExistingData(): Unit =
    runBlocking {
      // Test if data successfully migrated to Room and existing data is preserved
      val existingTitle = "Home Page"
      val existingBookmarkUrl = "https://alpine_linux/HomePage"
      val secondBookmarkEntity =
        BookmarkEntity(
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
          zimId = secondBookmarkEntity.zimId,
          zimFilePath = secondBookmarkEntity.zimReaderSource?.toDatabase(),
          zimReaderSource = secondBookmarkEntity.zimReaderSource,
          zimName = secondBookmarkEntity.zimName,
          bookmarkUrl = secondBookmarkEntity.bookmarkUrl,
          title = secondBookmarkEntity.bookmarkTitle,
          favicon = secondBookmarkEntity.favicon,
          libKiwixBook = libkiwixBook
        )
      )
      bookmarkBox.put(bookmarkEntity)
      // Migrate data into libkiwix
      objectBoxToLibkiwixMigrator.migrateBookMarks(bookmarkBox)
      val actualDataAfterMigration =
        objectBoxToLibkiwixMigrator.libkiwixBookmarks.bookmarks().first()
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
  fun testLargeDataMigration(): Unit =
    runBlocking {
      // Test large data migration for recent searches
      for (i in 1..1000) {
        bookmarkBox.put(
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
      // Migrate data into libkiwix
      objectBoxToLibkiwixMigrator.migrateBookMarks(bookmarkBox)
      // Check if data successfully migrated to libkiwix
      val actualDataAfterMigration =
        objectBoxToLibkiwixMigrator.libkiwixBookmarks.bookmarks().first()
      assertEquals(1000, actualDataAfterMigration.size)
      // Clear the bookmarks list from device to not affect the other test cases.
      clearBookmarks()
    }

  @Test
  fun testMigrationForNewCustomApps(): Unit =
    runBlocking {
      val expectedZimId = "60094d1e-1c9a-a60b-2011"
      val bookmarkEntity =
        BookmarkEntity(
          0,
          expectedZimId,
          expectedZimName,
          null,
          null,
          expectedBookmarkUrl,
          expectedTitle,
          expectedFavicon
        )
      bookmarkBox.put(bookmarkEntity)
      // migrate data into libkiwix
      objectBoxToLibkiwixMigrator.migrateBookMarks(bookmarkBox)
      // check if data successfully migrated to libkiwix
      val actualDataAfterMigration =
        objectBoxToLibkiwixMigrator.libkiwixBookmarks.bookmarks().first()
      assertEquals(1, actualDataAfterMigration.size)
      assertEquals(actualDataAfterMigration[0].zimReaderSource?.toDatabase(), null)
      assertEquals(actualDataAfterMigration[0].zimId, expectedZimId)
      assertEquals(actualDataAfterMigration[0].title, expectedTitle)
      assertEquals(actualDataAfterMigration[0].url, expectedBookmarkUrl)
      // Clear the bookmarks list from device to not affect the other test cases.
      clearBookmarks()
    }

  @Test
  fun testMigrationForNonExistingFiles(): Unit =
    runBlocking {
      val expectedZimId = "60094d1e-1c9a-a60b-2011-a60b"
      val nonExistingPath = "storage/Download/demo/demo.zim"
      val bookmarkEntity =
        BookmarkEntity(
          0,
          expectedZimId,
          expectedZimName,
          nonExistingPath,
          ZimReaderSource(File(nonExistingPath)),
          expectedBookmarkUrl,
          expectedTitle,
          expectedFavicon
        )
      bookmarkBox.put(bookmarkEntity)
      // migrate data into libkiwix
      objectBoxToLibkiwixMigrator.migrateBookMarks(bookmarkBox)
      // check if data successfully migrated to libkiwix
      val actualDataAfterMigration =
        objectBoxToLibkiwixMigrator.libkiwixBookmarks.bookmarks().first()
      assertEquals(1, actualDataAfterMigration.size)
      assertEquals(actualDataAfterMigration[0].zimReaderSource?.toDatabase(), null)
      assertEquals(actualDataAfterMigration[0].zimId, expectedZimId)
      assertEquals(actualDataAfterMigration[0].title, expectedTitle)
      assertEquals(actualDataAfterMigration[0].url, expectedBookmarkUrl)
      // Clear the bookmarks list from device to not affect the other test cases.
      clearBookmarks()
    }

  private suspend fun clearBookOnDisk() {
    objectBoxToLibkiwixMigrator.libkiwixBookOnDisk.delete(
      objectBoxToLibkiwixMigrator.libkiwixBookOnDisk.getBooks()
        .map { LibkiwixBook(it.book.nativeBook) }
    )
    bookOnDiskBox.removeAll()
    if (::zimFile.isInitialized) {
      zimFile.delete() // delete the temp ZIM file to free up the memory
    }
  }

  private suspend fun clearBookmarks() {
    // delete bookmarks for testing other edge cases
    objectBoxToLibkiwixMigrator.libkiwixBookmarks.deleteBookmarks(
      objectBoxToLibkiwixMigrator.libkiwixBookmarks.bookmarks()
        .first() as List<LibkiwixBookmarkItem>
    )
    bookmarkBox.removeAll()
    if (::zimFile.isInitialized) {
      zimFile.delete() // delete the temp ZIM file to free up the memory
    }
  }

  @After
  fun finish() {
    IdlingRegistry.getInstance().unregister(KiwixIdlingResource.getInstance())
    TestUtils.deleteTemporaryFilesOfTestCases(context)
    boxStore?.close()
    boxStore?.deleteAllFiles()
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
