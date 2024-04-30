/*
 * Kiwix Android
 * Copyright (c) 2022 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.note

import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import leakcanary.LeakAssertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.LocalLibraryFragmentDirections
import org.kiwix.kiwixmobile.nav.destination.library.library
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import org.kiwix.kiwixmobile.utils.StandardActions
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class NoteFragmentTest : BaseActivityTest() {

  @Rule
  @JvmField
  var retryRule = RetryRule()

  private lateinit var kiwixMainActivity: KiwixMainActivity

  @Before
  override fun waitForIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (isSystemUINotRespondingDialogVisible(this)) {
        closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
      putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
      putBoolean(SharedPreferenceUtil.PREF_PLAY_STORE_RESTRICTION, false)
    }
    activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
    }
  }

  @Test
  fun verifyNoteFragment() {
    activityScenario.onActivity {
      it.navigate(R.id.notesFragment)
    }
    note {
      assertToolbarExist()
      assertNoteRecyclerViewExist()
      assertSwitchWidgetExist()
    }
    LeakAssertions.assertNoLeaks()
  }

  @Test
  fun testUserCanSeeNotesForDeletedFiles() {
    // delete the notes if any saved to properly run the test scenario
    note {
      openNoteFragment()
      assertToolbarExist()
      assertNoteRecyclerViewExist()
      clickOnTrashIcon()
      assertDeleteNoteDialogDisplayed()
      clickOnDeleteButton()
      assertNoNotesTextDisplayed()
      pressBack()
    }
    loadZimFileInReader("testzim.zim")
    StandardActions.closeDrawer() // close the drawer if open before running the test cases.
    note {
      clickOnNoteMenuItem(context)
      assertNoteDialogDisplayed()
      writeDemoNote()
      saveNote()
      pressBack()
      openNoteFragment()
      assertToolbarExist()
      assertNoteRecyclerViewExist()
      clickOnSavedNote()
      clickOnOpenNote()
      assertNoteSaved()
      // to close the note dialog.
      pressBack()
      // to close the notes fragment.
      pressBack()
    }

    // goto local library fragment to delete the ZIM file
    UiThreadStatement.runOnUiThread {
      kiwixMainActivity.navigate(R.id.libraryFragment)
    }

    note(NoteRobot::refreshList)

    library {
      waitUntilZimFilesRefreshing()
      deleteZimIfExists()
    }

    note {
      openNoteFragment()
      assertToolbarExist()
      assertNoteRecyclerViewExist()
      clickOnSavedNote()
      clickOnOpenNote()
      assertNoteSaved()
      pressBack()
    }
    LeakAssertions.assertNoLeaks()
  }

  @Test
  fun testZimFileOpenedAfterOpeningNoteOnNotesScreen() {
    // delete the notes if any saved to properly run the test scenario
    note {
      openNoteFragment()
      assertToolbarExist()
      assertNoteRecyclerViewExist()
      clickOnTrashIcon()
      assertDeleteNoteDialogDisplayed()
      clickOnDeleteButton()
      assertNoNotesTextDisplayed()
      pressBack()
    }
    loadZimFileInReader("testzim.zim")
    note {
      clickOnNoteMenuItem(context)
      assertNoteDialogDisplayed()
      writeDemoNote()
      saveNote()
      pressBack()
    }

    // switch the zim file so that we can test this scenario where opening
    // the zim file properly open in the reader screen.
    loadZimFileInReader("small.zim")
    note {
      openNoteFragment()
      assertToolbarExist()
      assertNoteRecyclerViewExist()
      clickOnSavedNote()
      clickOnOpenNote()
      assertNoteSaved()
      // to close the note dialog.
      pressBack()
      // to close the notes fragment.
      pressBack()

      // now test the testzim.zim file is successfully loaded in the ZimFileReader.
      assertHomePageIsLoadedOfTestZimFile()
    }
  }

  private fun loadZimFileInReader(zimFileName: String) {
    activityScenario.onActivity {
      kiwixMainActivity = it
      kiwixMainActivity.navigate(R.id.libraryFragment)
    }

    val loadFileStream =
      NoteFragmentTest::class.java.classLoader.getResourceAsStream(zimFileName)
    val zimFile = File(ContextCompat.getExternalFilesDirs(context, null)[0], zimFileName)
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
    UiThreadStatement.runOnUiThread {
      kiwixMainActivity.navigate(
        LocalLibraryFragmentDirections.actionNavigationLibraryToNavigationReader()
          .apply { zimFileUri = zimFile.toUri().toString() }
      )
    }
  }
}
