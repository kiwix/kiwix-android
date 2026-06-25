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

package org.kiwix.kiwixmobile.core.main.note

import android.Manifest
import android.app.Activity
import androidx.compose.ui.text.input.TextFieldValue
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.main.note.helper.NoteMetadata
import org.kiwix.kiwixmobile.core.main.note.helper.NoteMetadataFactory
import org.kiwix.kiwixmobile.core.main.note.repository.NoteRepository
import org.kiwix.kiwixmobile.core.main.note.repository.NoteRepository.NoteFileContent
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.StorageUtils
import org.kiwix.kiwixmobile.core.utils.StorageUtils.isExternalStorageWritable
import org.kiwix.sharedFunctions.MainDispatcherRule
import java.io.File
import kotlin.io.path.createTempDirectory

@OptIn(ExperimentalCoroutinesApi::class)
class AddNoteViewModelTest {
  @Rule
  @JvmField
  val dispatcherRule = MainDispatcherRule()
  private lateinit var activity: Activity
  private lateinit var noteRepository: NoteRepository
  private lateinit var zimReaderContainer: ZimReaderContainer
  private lateinit var noteMetadataFactory: NoteMetadataFactory
  private lateinit var kiwixPermissionChecker: KiwixPermissionChecker
  private lateinit var viewModel: AddNoteViewModel

  private lateinit var noteMetadata: NoteMetadata

  @Before
  fun setUp() {
    mockkObject(StorageUtils)
    activity = mockk()
    noteRepository = mockk()
    zimReaderContainer = mockk(relaxed = true)
    noteMetadataFactory = mockk()
    kiwixPermissionChecker = mockk()
    noteMetadata = mockk(relaxed = true)

    viewModel = AddNoteViewModel(
      noteRepository = noteRepository,
      zimReaderContainer = zimReaderContainer,
      noteMetadataFactory = noteMetadataFactory,
      kiwixPermissionChecker = kiwixPermissionChecker,
      ioDispatcher = dispatcherRule.dispatcher
    )
  }

  @After
  fun tearDown() {
    unmockkObject(StorageUtils)
    unmockkAll()
  }

  private fun initializeViewModel(config: AddNoteDialogConfig = AddNoteDialogConfig()) {
    every { noteMetadataFactory.create(any(), zimReaderContainer) } returns noteMetadata
    every { noteMetadata.articleTitle } returns "Article Title"

    viewModel.initialize(config)
  }

  @Test
  fun `initialize updates article title`() {
    initializeViewModel()

    assertEquals(
      "Article Title",
      viewModel.uiState.value.articleTitle
    )
  }

  @Test
  fun `onTextChanged updates state and enables menu items`() {
    viewModel.onTextChanged(TextFieldValue("My Note"))
    with(viewModel.uiState.value) {
      assertEquals("My Note", noteTextFieldValue.text)
      assertTrue(noteEdited)
      assertTrue(isSaveMenuButtonEnable)
      assertTrue(isShareMenuButtonEnable)
    }
  }

  @Test
  fun `onTextChanged with same text does not mark note as edited`() {
    viewModel.onTextChanged(TextFieldValue(""))
    with(viewModel.uiState.value) {
      assertEquals("", noteTextFieldValue.text)
      assertFalse(noteEdited)
      assertFalse(isSaveMenuButtonEnable)
      assertFalse(isShareMenuButtonEnable)
    }
  }

  @Test
  fun `restoreNoteText restores note and marks edited`() {
    viewModel.restoreNoteText(TextFieldValue("Restored"))

    with(viewModel.uiState.value) {
      assertEquals("Restored", noteTextFieldValue.text)
      assertTrue(noteEdited)
    }
  }

  @Test
  fun `setInitialNoteText updates ui state when note exists`() = runTest {
    initializeViewModel()
    every { noteMetadata.isZimFileExist } returns true
    coEvery {
      noteRepository.loadNote(noteMetadata)
    } returns NoteFileContent(text = "Saved Note", fileExists = true)
    viewModel.setInitialNoteText()
    advanceUntilIdle()
    coVerify(exactly = 1) {
      noteRepository.loadNote(noteMetadata)
    }
    with(viewModel.uiState.value) {
      assertEquals("Saved Note", noteTextFieldValue.text)
      assertTrue(isDeleteMenuButtonEnable)
      assertTrue(isShareMenuButtonEnable)
      assertTrue(isSaveMenuButtonEnable)
    }
  }

  @Test
  fun `setInitialNoteText handles missing note file`() = runTest {
    initializeViewModel()
    every { noteMetadata.isZimFileExist } returns false

    coEvery {
      noteRepository.loadNote(noteMetadata)
    } returns NoteFileContent(fileExists = false, text = "")

    viewModel.setInitialNoteText()
    advanceUntilIdle()

    with(viewModel.uiState.value) {
      assertFalse(isDeleteMenuButtonEnable)
      assertFalse(isShareMenuButtonEnable)
    }
  }

  @Test
  fun `deleteNote success clears note and disables menu items`() = runTest {
    initializeViewModel()

    viewModel.onTextChanged(TextFieldValue("Note Text"))

    coEvery { noteRepository.deleteNote(noteMetadata) } returns true
    viewModel.effects.test {
      viewModel.deleteNote()

      advanceUntilIdle()

      assertEquals(
        AddNoteViewModel.AddNoteEffect.ShowUndoDeleteSnackbar("Note Text"),
        awaitItem()
      )

      with(viewModel.uiState.value) {
        assertEquals("", noteTextFieldValue.text)
        assertFalse(isDeleteMenuButtonEnable)
        assertFalse(isShareMenuButtonEnable)
        assertFalse(isSaveMenuButtonEnable)
      }
    }
  }

  @Test
  fun `deleteNote emits unsuccessful toast when deletion fails`() = runTest {
    initializeViewModel()

    coEvery { noteRepository.deleteNote(noteMetadata) } returns false
    viewModel.effects.test {
      viewModel.deleteNote()
      advanceUntilIdle()

      assertEquals(
        AddNoteViewModel.AddNoteEffect.ShowToast(R.string.note_delete_unsuccessful),
        awaitItem()
      )
    }
  }

  @Test
  fun `closeDialog emits dismiss effect when note not edited`() = runTest {
    viewModel.effects.test {
      viewModel.closeDialog()

      assertEquals(
        AddNoteViewModel.AddNoteEffect.DismissDialog,
        awaitItem()
      )
    }
  }

  @Test
  fun `closeDialog emits discard confirmation when note edited`() = runTest {
    viewModel.onTextChanged(TextFieldValue("Edited"))

    viewModel.effects.test {
      viewModel.closeDialog()

      assertEquals(
        AddNoteViewModel.AddNoteEffect.ShowDiscardConfirmationDialog,
        awaitItem()
      )
    }
  }

  @Test
  fun `saveNote emits storage not writable toast`() = runTest {
    initializeViewModel()
    every { isExternalStorageWritable() } returns false

    viewModel.effects.test {
      viewModel.saveNote()
      advanceUntilIdle()

      assertEquals(
        AddNoteViewModel.AddNoteEffect.ShowToast(
          R.string.note_save_error_storage_not_writable
        ),
        awaitItem()
      )
    }
  }

  @Test
  fun `saveNote requests storage permission when permission missing`() = runTest {
    initializeViewModel()
    every { isExternalStorageWritable() } returns true
    coEvery { kiwixPermissionChecker.hasWriteExternalStoragePermission() } returns false

    viewModel.effects.test {
      viewModel.saveNote()
      advanceUntilIdle()
      assertEquals(
        AddNoteViewModel.AddNoteEffect.RequestStoragePermission,
        awaitItem()
      )
    }
  }

  @Test
  fun `saveNote updates state and emits success toast`() = runTest {
    initializeViewModel()

    viewModel.onTextChanged(TextFieldValue("My Note"))
    every { isExternalStorageWritable() } returns true
    coEvery { kiwixPermissionChecker.hasWriteExternalStoragePermission() } returns true

    coEvery {
      noteRepository.saveNote(noteMetadata, "My Note")
    } returns true

    viewModel.effects.test {
      viewModel.saveNote()

      advanceUntilIdle()

      assertEquals(
        AddNoteViewModel.AddNoteEffect.ShowToast(
          R.string.note_save_successful
        ),
        awaitItem()
      )
    }

    with(viewModel.uiState.value) {
      assertFalse(noteEdited)
      assertTrue(isDeleteMenuButtonEnable)
      assertFalse(isSaveMenuButtonEnable)
    }
  }

  @Test
  fun `saveNote emits unsuccessful toast when save fails`() = runTest {
    initializeViewModel()

    viewModel.onTextChanged(TextFieldValue("My Note"))
    every { isExternalStorageWritable() } returns true
    coEvery { kiwixPermissionChecker.hasWriteExternalStoragePermission() } returns true

    coEvery {
      noteRepository.saveNote(noteMetadata, "My Note")
    } returns false

    viewModel.effects.test {
      viewModel.saveNote()

      advanceUntilIdle()

      assertEquals(
        AddNoteViewModel.AddNoteEffect.ShowToast(
          R.string.note_save_unsuccessful
        ),
        awaitItem()
      )
    }
  }

  @Test
  fun `shareNote emits ShareNote when file exists`() = runTest {
    initializeViewModel()
    val tempDir = createTempDirectory().toFile()
    val tempFile = File(tempDir, "article.txt")
    tempFile.writeText("note")

    every { noteMetadata.zimNotesDirectory } returns "${tempDir.absolutePath}/"
    every { noteMetadata.articleNoteFileName } returns "article"

    viewModel.effects.test {
      viewModel.shareNote()

      val effect = awaitItem()

      assertTrue(effect is AddNoteViewModel.AddNoteEffect.ShareNote)
      assertEquals(
        tempFile.absolutePath,
        (effect as AddNoteViewModel.AddNoteEffect.ShareNote).noteFile.absolutePath
      )
    }
  }

  @Test
  fun `shareNote emits file missing toast when file does not exist`() = runTest {
    initializeViewModel()

    every { noteMetadata.zimNotesDirectory } returns "/tmp/"
    every { noteMetadata.articleNoteFileName } returns "missing"

    viewModel.effects.test {
      viewModel.shareNote()

      assertEquals(
        AddNoteViewModel.AddNoteEffect.ShowToast(R.string.note_share_error_file_missing),
        awaitItem()
      )
    }
  }

  @Test
  fun `shareNote saves edited note before sharing`() = runTest {
    initializeViewModel()

    viewModel.onTextChanged(TextFieldValue("Edited"))

    every { noteMetadata.isZimFileExist } returns true
    every { isExternalStorageWritable() } returns true
    coEvery { kiwixPermissionChecker.hasWriteExternalStoragePermission() } returns true

    coEvery {
      noteRepository.saveNote(any(), any())
    } returns true

    viewModel.shareNote()

    advanceUntilIdle()

    coVerify {
      noteRepository.saveNote(noteMetadata, "Edited")
    }
  }

  @Test
  fun `onStoragePermissionResult emits rationale toast when permission denied and rationale should be shown`() =
    runTest {
      every {
        kiwixPermissionChecker.shouldShowRationale(
          activity,
          android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
      } returns true

      viewModel.effects.test {
        viewModel.onStoragePermissionResult(isGranted = false, activity = activity)

        assertEquals(
          AddNoteViewModel.AddNoteEffect.ShowToast(R.string.ext_storage_permission_rationale_add_note),
          awaitItem()
        )
      }
    }

  @Test
  fun `onStoragePermissionResult emits read permission dialog when permission denied permanently`() =
    runTest {
      every {
        kiwixPermissionChecker.shouldShowRationale(
          activity,
          Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
      } returns false

      viewModel.effects.test {
        viewModel.onStoragePermissionResult(isGranted = false, activity = activity)

        assertEquals(
          AddNoteViewModel.AddNoteEffect.ReadPermissionRequiredDialog,
          awaitItem()
        )
      }
    }

  @Test
  fun `onStoragePermissionResult saves note when permission granted`() = runTest {
    initializeViewModel()

    viewModel.onTextChanged(TextFieldValue("My Note"))

    every { isExternalStorageWritable() } returns true
    coEvery { kiwixPermissionChecker.hasWriteExternalStoragePermission() } returns true
    coEvery { noteRepository.saveNote(noteMetadata, "My Note") } returns true

    viewModel.onStoragePermissionResult(isGranted = true, activity = activity)
    advanceUntilIdle()

    coVerify(exactly = 1) {
      noteRepository.saveNote(noteMetadata, "My Note")
    }
  }
}
