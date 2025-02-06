package org.kiwix.kiwixmobile.core.page.notes.viewmodel.effects

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

import androidx.appcompat.app.AppCompatActivity
import io.reactivex.processors.PublishProcessor
import kotlinx.coroutines.CoroutineScope
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.PageDao
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.cachedComponent
import org.kiwix.kiwixmobile.core.page.notes.viewmodel.NotesState
import org.kiwix.kiwixmobile.core.page.viewmodel.effects.DeletePageItems
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.DeleteAllNotes
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.DeleteSelectedNotes
import javax.inject.Inject

data class ShowDeleteNotesDialog(
  private val effects: PublishProcessor<SideEffect<*>>,
  private val state: NotesState,
  private val pageDao: PageDao,
  private val viewModelScope: CoroutineScope
) : SideEffect<Unit> {
  @Inject lateinit var dialogShower: DialogShower
  override fun invokeWith(activity: AppCompatActivity) {
    activity.cachedComponent.inject(this)
    dialogShower.show(
      if (state.isInSelectionState) DeleteSelectedNotes else DeleteAllNotes,
      {
        effects.offer(DeletePageItems(state, pageDao, viewModelScope))
      }
    )
  }
}
