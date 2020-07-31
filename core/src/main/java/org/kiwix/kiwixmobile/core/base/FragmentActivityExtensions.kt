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

package org.kiwix.kiwixmobile.core.base

import android.content.Intent
import android.view.ActionMode
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions.Super.ShouldCall

interface FragmentActivityExtensions {
  enum class Super {
    ShouldCall,
    ShouldNotCall
  }

  fun onActionModeStarted(actionMode: ActionMode, activity: AppCompatActivity): Super = ShouldCall
  fun onActionModeFinished(actionMode: ActionMode, activity: AppCompatActivity): Super = ShouldCall
  fun onBackPressed(activity: AppCompatActivity): Super = ShouldCall
  fun onNewIntent(intent: Intent, activity: AppCompatActivity): Super = ShouldCall
  fun onCreateOptionsMenu(menu: Menu, activity: AppCompatActivity): Super = ShouldCall
}
