/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.page.viewmodel.effects

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.main.AddNoteDialog
import org.kiwix.kiwixmobile.core.main.AddNoteDialog.Companion.NOTE_FILE_PATH
import org.kiwix.kiwixmobile.core.main.AddNoteDialog.Companion.ARTICLE_URL
import org.kiwix.kiwixmobile.core.main.AddNoteDialog.Companion.NOTES_TITLE
import org.kiwix.kiwixmobile.core.main.CoreMainActivity

class OpenNote(
  private val noteFilePath: String,
  private val zimFileUrl: String,
  private val title: String,
) : SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    activity as CoreMainActivity
    showAddNoteDialog(activity)
  }

  private fun showAddNoteDialog(activity: AppCompatActivity) {
    val act: CoreMainActivity = activity as CoreMainActivity
    val fragmentTransaction: FragmentTransaction =
      act.supportFragmentManager.beginTransaction()
    val previousInstance: Fragment? =
      act.supportFragmentManager.findFragmentByTag(AddNoteDialog.TAG)

    if (previousInstance == null) {
      val dialogFragment = AddNoteDialog()
      val bundle = Bundle()
      bundle.putString(NOTE_FILE_PATH, noteFilePath)
      bundle.putString(ARTICLE_URL, zimFileUrl)
      bundle.putString(NOTES_TITLE, title)
      dialogFragment.arguments = bundle
      dialogFragment.show(fragmentTransaction, AddNoteDialog.TAG)
    }
  }
}
