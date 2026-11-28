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

package org.kiwix.kiwixmobile.core.utils.files

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import eu.mhutti1.utils.storage.StorageDeviceUtils
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.dao.HistoryRoomDao
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.dao.NotesRoomDao
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.sharedFunctions.MainDispatcherRule
import org.kiwix.sharedFunctions.TestApplication
import org.kiwix.sharedFunctions.libkiwixBook
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], application = TestApplication::class)
class LegacyBooksMoverTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val kiwixDataStore: KiwixDataStore = mockk(relaxed = true)
  private val libkiwixBookOnDisk: LibkiwixBookOnDisk = mockk(relaxed = true)
  private val libkiwixBookmarks: LibkiwixBookmarks = mockk(relaxed = true)
  private val historyRoomDao: HistoryRoomDao = mockk(relaxed = true)
  private val notesRoomDao: NotesRoomDao = mockk(relaxed = true)
  private lateinit var documentsKiwix: File
  private lateinit var mover: LegacyBooksMover

  @Before
  fun setUp() {
    val documents = StorageDeviceUtils.publicDocumentsDirectory(context)!!
    documentsKiwix = File(documents, "Kiwix").apply { deleteRecursively() }
    coEvery { kiwixDataStore.legacyBooksMoveDeclined } returns flowOf(false)
    coEvery { kiwixDataStore.currentZimFile } returns flowOf(null)
    coEvery { kiwixDataStore.selectedStorage } returns
      flowOf(context.getExternalFilesDir(null)!!.path)
    mover = LegacyBooksMover(
      context,
      kiwixDataStore,
      libkiwixBookOnDisk,
      libkiwixBookmarks,
      historyRoomDao,
      notesRoomDao,
      mainDispatcherRule.dispatcher
    )
  }

  private fun appSpecificBook(name: String, content: String = "zim-$name"): BookOnDisk {
    val file = File(context.getExternalFilesDir(null), "Kiwix/$name").apply {
      parentFile?.mkdirs()
      writeText(content)
    }
    return BookOnDisk(book = libkiwixBook(id = name), zimReaderSource = ZimReaderSource(file))
  }

  @Test
  fun offersOnlyBooksInAppSpecificStorage() = runTest(mainDispatcherRule.dispatcher) {
    val legacy = appSpecificBook("wiki.zim", "12345")
    val elsewhere = File(documentsKiwix, "other.zim").apply {
      parentFile?.mkdirs()
      writeText("x")
    }
    coEvery { libkiwixBookOnDisk.getBooks() } returns listOf(
      legacy,
      BookOnDisk(book = libkiwixBook(id = "other"), zimReaderSource = ZimReaderSource(elsewhere))
    )

    val offer = mover.offer()!!
    assertEquals(listOf(legacy), offer.books)
    assertEquals(5L, offer.totalBytes)
  }

  @Test
  fun noOfferOnceDeclinedOrWithoutBooks() = runTest(mainDispatcherRule.dispatcher) {
    coEvery { libkiwixBookOnDisk.getBooks() } returns emptyList()
    assertNull(mover.offer())

    coEvery { libkiwixBookOnDisk.getBooks() } returns listOf(appSpecificBook("wiki.zim"))
    coEvery { kiwixDataStore.legacyBooksMoveDeclined } returns flowOf(true)
    assertNull(mover.offer())
  }

  @Test
  fun switchesEmptyLegacyStorageToDocuments() = runTest(mainDispatcherRule.dispatcher) {
    coEvery { libkiwixBookOnDisk.getBooks() } returns emptyList()

    mover.useDocumentsIfNothingToMove()

    coVerify { kiwixDataStore.setSelectedStorage(documentsKiwix.parent!!) }
  }

  @Test
  fun keepsLegacyStorageWhileItHoldsBooks() = runTest(mainDispatcherRule.dispatcher) {
    coEvery { libkiwixBookOnDisk.getBooks() } returns listOf(appSpecificBook("wiki.zim"))

    mover.useDocumentsIfNothingToMove()

    coVerify(exactly = 0) { kiwixDataStore.setSelectedStorage(any()) }
  }

  @Test
  fun movesAndRelocatesReferences() = runTest(mainDispatcherRule.dispatcher) {
    val book = appSpecificBook("wiki.zim", "zim content")
    val oldPath = book.zimReaderSource.file!!.canonicalPath
    val newPath = File(documentsKiwix, "wiki.zim").canonicalPath
    val progress = mutableListOf<Pair<Int, Int>>()

    val result = mover.move(LegacyBooksMover.Offer(listOf(book), 11)) { done, all ->
      progress += done to all
    }

    assertEquals(LegacyBooksMover.Result(moved = 1, failed = 0), result)
    assertEquals("zim content", File(newPath).readText())
    assertFalse(File(oldPath).exists())
    assertEquals(listOf(1 to 1), progress)
    coVerify { kiwixDataStore.setSelectedStorage(documentsKiwix.parent!!) }
    coVerify(ordering = Ordering.ORDERED) {
      libkiwixBookOnDisk.delete("wiki.zim")
      libkiwixBookOnDisk.insertFile(newPath)
    }
    coVerify { libkiwixBookmarks.relocateBook("wiki.zim", File(newPath)) }
    verify { historyRoomDao.relocateBook(oldPath, newPath) }
    verify { notesRoomDao.relocateBook(oldPath, newPath) }
  }

  @Test
  fun splitBooksMoveAllParts() = runTest(mainDispatcherRule.dispatcher) {
    val first = appSpecificBook("split.zimaa", "a")
    val second = File(first.zimReaderSource.file!!.parentFile, "split.zimab").apply { writeText("b") }

    mover.move(LegacyBooksMover.Offer(listOf(first), 2))

    assertEquals("a", File(documentsKiwix, "split.zimaa").readText())
    assertEquals("b", File(documentsKiwix, "split.zimab").readText())
    assertFalse(second.exists())
  }

  @Test
  fun existingTargetKeepsTheOriginal() = runTest(mainDispatcherRule.dispatcher) {
    val book = appSpecificBook("wiki.zim")
    File(documentsKiwix, "wiki.zim").apply {
      parentFile?.mkdirs()
      writeText("other")
    }

    val result = mover.move(LegacyBooksMover.Offer(listOf(book), 1))

    assertEquals(LegacyBooksMover.Result(moved = 0, failed = 1), result)
    assertTrue(book.zimReaderSource.file!!.exists())
    coVerify(exactly = 0) { libkiwixBookOnDisk.delete(any<String>()) }
  }
}
