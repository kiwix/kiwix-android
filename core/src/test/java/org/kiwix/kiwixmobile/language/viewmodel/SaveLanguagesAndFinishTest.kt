package org.kiwix.kiwixmobile.language.viewmodel

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

import android.app.Activity
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.schedulers.Schedulers
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.database.newdb.dao.NewLanguagesDao
import org.kiwix.kiwixmobile.resetSchedulers
import org.kiwix.kiwixmobile.setScheduler
import org.kiwix.kiwixmobile.zim_manager.Language

class SaveLanguagesAndFinishTest {

  @Test
  fun `invoke saves and finishes`() {
    setScheduler(Schedulers.trampoline())
    val languageDao = mockk<NewLanguagesDao>()
    val activity = mockk<Activity>()
    val languages = listOf<Language>()
    SaveLanguagesAndFinish(languages, languageDao).invokeWith(activity)
    verify {
      languageDao.insert(languages)
      activity.finish()
    }
    resetSchedulers()
  }
}
