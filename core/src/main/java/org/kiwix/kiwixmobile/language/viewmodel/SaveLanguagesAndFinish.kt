/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.language.viewmodel

import android.app.Activity
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.database.newdb.dao.NewLanguagesDao
import org.kiwix.kiwixmobile.zim_manager.Language
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.effects.SideEffect

data class SaveLanguagesAndFinish(
  val languages: List<Language>,
  val languageDao: NewLanguagesDao
) : SideEffect<Unit> {

  override fun invokeWith(activity: Activity) {
    Flowable.fromCallable { languageDao.insert(languages) }
      .subscribeOn(Schedulers.io())
      .subscribe({
        activity.finish()
      }, Throwable::printStackTrace)
  }
}
