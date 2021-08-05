/*
 * Kiwix Android
 * Copyright (c) 2021 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.dao

import org.junit.jupiter.api.Test
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.objectbox.Box
import org.kiwix.kiwixmobile.core.dao.entities.LanguageEntity
import org.kiwix.kiwixmobile.core.zim_manager.Language
import java.util.Locale
import java.util.concurrent.Callable

internal class NewLanguagesDaoTest {

  private val box: Box<LanguageEntity> = mockk(relaxed = true)
  private val newLanguagesDao = NewLanguagesDao(box)

  @Test
  fun insert() {
    val id = 0L
    val active = false
    val occurrencesOfLanguage = 0
    val language: Language = mockk()
    val slot: CapturingSlot<Callable<Unit>> = slot()
    every { box.store.callInTx(capture(slot)) } returns Unit
    every { language.id } returns id
    every { language.active } returns active
    every { language.languageCode } returns Locale.ENGLISH.toString()
    every { language.occurencesOfLanguage } returns occurrencesOfLanguage
    newLanguagesDao.insert(listOf(language))
    slot.captured.call()
    verify { box.removeAll() }
    verify {
      box.put(
        listOf(
          LanguageEntity(
            id = 0L,
            locale = Locale.ENGLISH,
            active = false,
            occurencesOfLanguage = 0
          )
        )
      )
    }
  }
}
