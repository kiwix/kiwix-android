/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.language.viewmodel

import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.NewLanguagesDao
import org.kiwix.kiwixmobile.core.zim_manager.Language

data class SaveLanguagesAndFinish(
  val languages: List<Language>,
  val languageDao: NewLanguagesDao
) : SideEffect<Unit> {

  override fun invokeWith(activity: AppCompatActivity) {
    Flowable.fromCallable { languageDao.insert(languages) }
      .subscribeOn(Schedulers.io())
      .subscribe({
        activity.onBackPressed()
      }, Throwable::printStackTrace)
  }
}
