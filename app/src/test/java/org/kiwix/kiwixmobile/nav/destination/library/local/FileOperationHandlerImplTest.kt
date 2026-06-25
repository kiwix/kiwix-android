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

package org.kiwix.kiwixmobile.nav.destination.library.local

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.sharedFunctions.MainDispatcherRule
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

@OptIn(ExperimentalCoroutinesApi::class)
class FileOperationHandlerImplTest {
  private val context: Context = mockk()

  @RegisterExtension
  @JvmField
  val mainDispatcherRule = MainDispatcherRule()

  private val contentResolver: ContentResolver = mockk()
  private val selectedFile: DocumentFile = mockk()
  private val parentFile: DocumentFile = mockk()
  private val sourceUri: Uri = mockk()
  private val destinationFolderUri: Uri = mockk()
  private val destinationFile: File = mockk()
  private val parentUri: Uri = mockk()
  private val originalParentUri: Uri = mockk()
  private lateinit var fileOperationHandler: FileOperationHandlerImpl

  @BeforeEach
  fun init() {
    every { context.contentResolver } returns contentResolver
    every { selectedFile.parentFile } returns parentFile
    every { parentFile.uri } returns parentUri
    every { destinationFile.parentFile } returns File("parent")
    every { destinationFile.path } returns "parent/test"
    mockkStatic(DocumentsContract::class)

    fileOperationHandler = FileOperationHandlerImpl(context, mainDispatcherRule.dispatcher)
  }

  @AfterEach
  fun reset() {
    clearAllMocks()
  }

  @Nested
  inner class Copy {
    @Test
    fun whenOpenFileDescriptorReturnsNull_throwsFileNotFoundException() = runTest {
      every { contentResolver.openFileDescriptor(sourceUri, "r") } returns null

      var thrown: Throwable? = null
      try {
        fileOperationHandler.copy(
          sourceUri = sourceUri,
          destinationFile = destinationFile,
          onProgress = {}
        )
      } catch (e: FileNotFoundException) {
        thrown = e
      }

      assertNotNull(thrown)
    }

    @Test
    fun whenFileEmpty_emitsHundredProgress() = runTest {
      val emptyFile =
        File.createTempFile("empty", ".txt")

      val parcelFileDescriptor =
        mockk<ParcelFileDescriptor>()

      every {
        contentResolver.openFileDescriptor(sourceUri, "r")
      } returns parcelFileDescriptor

      every {
        parcelFileDescriptor.fileDescriptor
      } returns FileInputStream(emptyFile).fd

      var progress = 0

      fileOperationHandler.copy(
        sourceUri = sourceUri,
        destinationFile = File.createTempFile("destination", ".txt"),
        onProgress = {
          progress = it
        }
      )

      assertEquals(100, progress)
    }

    @Test
    fun whenFileHasContent_updatesProgress() = runTest {
      val sourceFile =
        File.createTempFile("source", ".txt").apply {
          writeText("Kiwix testing content")
        }

      val destinationFile =
        File.createTempFile("destination", ".txt")

      val sourceUri: Uri = mockk()

      val parcelFileDescriptor =
        mockk<ParcelFileDescriptor>(relaxed = true)

      every {
        contentResolver.openFileDescriptor(sourceUri, "r")
      } returns parcelFileDescriptor

      every {
        parcelFileDescriptor.fileDescriptor
      } returns FileInputStream(sourceFile).fd

      var latestProgress = 0

      fileOperationHandler.copy(
        sourceUri = sourceUri,
        destinationFile = destinationFile,
        onProgress = {
          latestProgress = it
        }
      )

      assertEquals(100, latestProgress)

      assertEquals(
        sourceFile.readText(),
        destinationFile.readText()
      )
    }
  }

  @Nested
  inner class Move {
    private fun mockDocumentCursor(flags: Int): Cursor {
      val cursor = mockk<Cursor>()

      every {
        DocumentsContract.isDocumentUri(context, sourceUri)
      } returns true

      every {
        contentResolver.query(
          sourceUri,
          arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
          null,
          null,
          null
        )
      } returns cursor

      every {
        cursor.moveToFirst()
      } returns true

      every {
        cursor.getInt(ZERO)
      } returns flags

      return cursor
    }

    @Test
    fun whenParentUriNull_fallsBackToCopyAndReturnsTrue() = runTest {
      val destinationFile = File("test")
      every { selectedFile.parentFile } returns null

      val parcelFileDescriptor = mockk<ParcelFileDescriptor>(relaxed = true)

      every {
        contentResolver.openFileDescriptor(sourceUri, "r")
      } returns parcelFileDescriptor

      val result =
        fileOperationHandler.move(
          selectedFile = selectedFile,
          sourceUri = sourceUri,
          destinationFolderUri = destinationFolderUri,
          destinationFile = destinationFile,
          onProgress = {}
        )

      assertEquals(true, result)

      verify(exactly = 1) {
        contentResolver.openFileDescriptor(sourceUri, "r")
      }
    }

    @Nested
    inner class TryMoveWithDocumentContract {
      @Test
      fun whenDocumentCanMoveSucceeds_returnsTrue() = runTest {
        mockDocumentCursor(DocumentsContract.Document.FLAG_SUPPORTS_MOVE)

        every {
          DocumentsContract.moveDocument(
            contentResolver,
            sourceUri,
            parentUri,
            destinationFolderUri
          )
        } returns sourceUri

        val result =
          fileOperationHandler.move(
            selectedFile = selectedFile,
            sourceUri = sourceUri,
            destinationFolderUri = destinationFolderUri,
            destinationFile = destinationFile,
            onProgress = {}
          )

        assertEquals(true, result)

        verify(exactly = 1) {
          DocumentsContract.moveDocument(
            contentResolver,
            sourceUri,
            parentUri,
            destinationFolderUri
          )
        }
      }

      @Test
      fun whenDocumentCanMoveFails_returnsFalse() = runTest {
        every { DocumentsContract.isDocumentUri(context, sourceUri) } returns false

        val result = fileOperationHandler.move(
          selectedFile = selectedFile,
          sourceUri = sourceUri,
          destinationFolderUri = destinationFolderUri,
          destinationFile = destinationFile,
          onProgress = {}
        )

        assertFalse(result)
        verify(exactly = 0) {
          DocumentsContract.moveDocument(
            contentResolver,
            sourceUri,
            parentUri,
            destinationFolderUri
          )
        }
      }

      @Test
      fun whenMoveDocumentThrows_returnsFalse() = runTest {
        mockDocumentCursor(DocumentsContract.Document.FLAG_SUPPORTS_MOVE)

        every {
          DocumentsContract.moveDocument(
            contentResolver,
            sourceUri,
            parentUri,
            destinationFolderUri
          )
        } throws RuntimeException()

        val result =
          fileOperationHandler.move(
            selectedFile = selectedFile,
            sourceUri = sourceUri,
            destinationFolderUri = destinationFolderUri,
            destinationFile = destinationFile,
            onProgress = {}
          )

        assertEquals(false, result)
      }
    }

    @Nested
    inner class DocumentCanMove {
      @Test
      fun whenUriIsNotDocumentUri_returnsFalse() = runTest {
        every {
          DocumentsContract.isDocumentUri(context, sourceUri)
        } returns false

        val result =
          fileOperationHandler.move(
            selectedFile = selectedFile,
            sourceUri = sourceUri,
            destinationFolderUri = destinationFolderUri,
            destinationFile = destinationFile,
            onProgress = {}
          )

        assertEquals(false, result)
      }

      @Test
      fun whenQueryReturnsNull_returnsFalse() = runTest {
        every {
          DocumentsContract.isDocumentUri(context, sourceUri)
        } returns true

        every {
          contentResolver.query(
            sourceUri,
            arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
            null,
            null,
            null
          )
        } returns null

        val result =
          fileOperationHandler.move(
            selectedFile = selectedFile,
            sourceUri = sourceUri,
            destinationFolderUri = destinationFolderUri,
            destinationFile = destinationFile,
            onProgress = {}
          )

        assertEquals(false, result)
      }

      @Test
      fun whenCursorCannotMoveToFirst_returnsFalse() = runTest {
        val cursor = mockk<Cursor>()

        every {
          DocumentsContract.isDocumentUri(context, sourceUri)
        } returns true

        every {
          contentResolver.query(
            sourceUri,
            arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
            null,
            null,
            null
          )
        } returns cursor

        every { cursor.moveToFirst() } returns false

        val result =
          fileOperationHandler.move(
            selectedFile = selectedFile,
            sourceUri = sourceUri,
            destinationFolderUri = destinationFolderUri,
            destinationFile = destinationFile,
            onProgress = {}
          )

        assertEquals(false, result)
      }

      @Test
      fun whenDocumentDoesNotSupportMove_returnsFalse() = runTest {
        mockDocumentCursor(0)

        val result =
          fileOperationHandler.move(
            selectedFile = selectedFile,
            sourceUri = sourceUri,
            destinationFolderUri = destinationFolderUri,
            destinationFile = destinationFile,
            onProgress = {}
          )

        assertEquals(false, result)
      }

      @Test
      fun whenDocumentSupportsMove_returnsTrue() = runTest {
        mockDocumentCursor(DocumentsContract.Document.FLAG_SUPPORTS_MOVE)

        every {
          DocumentsContract.moveDocument(
            contentResolver,
            sourceUri,
            parentUri,
            destinationFolderUri
          )
        } returns sourceUri

        val result =
          fileOperationHandler.move(
            selectedFile = selectedFile,
            sourceUri = sourceUri,
            destinationFolderUri = destinationFolderUri,
            destinationFile = destinationFile,
            onProgress = {}
          )

        assertEquals(true, result)
      }
    }
  }

  @Nested
  inner class RollBackMove {
    @Test
    fun whenMoveBackSucceeds_returnsTrue() {
      val cursor = mockk<Cursor>()

      every {
        DocumentsContract.isDocumentUri(any(), any())
      } returns true

      every {
        contentResolver.query(
          any(),
          arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
          null,
          null,
          null
        )
      } returns cursor

      every {
        cursor.moveToFirst()
      } returns true

      every {
        cursor.getInt(ZERO)
      } returns DocumentsContract.Document.FLAG_SUPPORTS_MOVE

      every {
        DocumentsContract.moveDocument(
          any(),
          any(),
          any(),
          any()
        )
      } returns mockk()

      val result =
        fileOperationHandler.rollbackMove(
          destinationFile = destinationFile,
          originalParentUri = originalParentUri
        )

      assertTrue(result)

      verify(exactly = 1) {
        DocumentsContract.moveDocument(
          any(),
          any(),
          any(),
          any()
        )
      }
    }

    @Test
    fun whenMoveBackFails_returnsFalse() {
      every {
        DocumentsContract.isDocumentUri(any(), any())
      } returns false

      val result =
        fileOperationHandler.rollbackMove(
          destinationFile = destinationFile,
          originalParentUri = originalParentUri
        )

      assertFalse(result)

      verify(exactly = 0) {
        DocumentsContract.moveDocument(
          any(),
          any(),
          any(),
          any()
        )
      }
    }

    @Test
    fun whenMoveDocumentThrows_returnsFalse() {
      val cursor = mockk<Cursor>()

      every {
        DocumentsContract.isDocumentUri(any(), any())
      } returns true

      every {
        contentResolver.query(
          any(),
          arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
          null,
          null,
          null
        )
      } returns cursor

      every {
        cursor.moveToFirst()
      } returns true

      every {
        cursor.getInt(ZERO)
      } returns DocumentsContract.Document.FLAG_SUPPORTS_MOVE

      every {
        DocumentsContract.moveDocument(
          any(),
          any(),
          any(),
          any()
        )
      } throws RuntimeException()

      val result =
        fileOperationHandler.rollbackMove(
          destinationFile = destinationFile,
          originalParentUri = originalParentUri
        )

      assertFalse(result)
    }
  }

  @Nested
  inner class Delete {
    @Test
    fun whenDocumentDeleteSucceeds_returnsTrue() = runTest {
      mockkStatic(DocumentsContract::class)
      val uri: Uri = mockk()

      every {
        DocumentsContract.deleteDocument(any(), any())
      } returns true

      val result = fileOperationHandler.delete(uri, selectedFile)

      assertTrue(result)

      verify(exactly = 0) { selectedFile.delete() }
    }

    @Test
    fun whenDocumentDeleteFails_deletesSelectedFileAndReturnsFalse() = runTest {
      mockkStatic(DocumentsContract::class)
      val uri: Uri = mockk()

      every {
        DocumentsContract.deleteDocument(contentResolver, uri)
      } throws RuntimeException()

      every {
        selectedFile.delete()
      } returns true

      val result = fileOperationHandler.delete(uri, selectedFile)

      assertFalse(result)

      verify(exactly = 1) {
        selectedFile.delete()
      }
    }
  }
}
