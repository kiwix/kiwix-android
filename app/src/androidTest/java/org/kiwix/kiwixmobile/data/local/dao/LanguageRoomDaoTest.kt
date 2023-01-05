/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.objectbox.Box
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.dao.LanguageRoomDao
import org.kiwix.kiwixmobile.core.dao.NewLanguagesDao
import org.kiwix.kiwixmobile.core.dao.entities.LanguageEntity
import org.kiwix.kiwixmobile.core.dao.entities.LanguageRoomEntity
import org.kiwix.kiwixmobile.core.data.local.KiwixRoomDatabase
import org.kiwix.kiwixmobile.core.zim_manager.Language
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class LanguageRoomDaoTest {

  private lateinit var languageRoomDao: LanguageRoomDao
  private lateinit var db: KiwixRoomDatabase
  private val languageBox: Box<LanguageEntity> = mockk(relaxed = true)
  private val languageDao = NewLanguagesDao(languageBox)

  @Test
  @Throws(IOException::class)
  fun test_inserting_a_language() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(
      context, KiwixRoomDatabase::class.java
    ).build()
    languageRoomDao = db.languageRoomDao()
    val language: Language = mockk()
    languageRoomDao.insert(LanguageRoomEntity(language))
    Assertions.assertEquals(1, languageRoomDao.languages().count())
  }

  @Test
  @Throws(IOException::class)
  fun test_inserting_multiple_language() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(
      context, KiwixRoomDatabase::class.java
    ).build()
    languageRoomDao = db.languageRoomDao()
    val language: Language = mockk()
    val language2: Language = mockk()
    val language3: Language = mockk()
    val language4: Language = mockk()
    val language5: Language = mockk()

    val languageLists = listOf(
      language, language2, language3, language4, language5
    )
    languageRoomDao.insert(languageLists)
    Assertions.assertEquals(5, languageRoomDao.languages().count())
  }

  @Test
  @Throws(IOException::class)
  fun test_deleting_multiple_language() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(
      context, KiwixRoomDatabase::class.java
    ).build()
    languageRoomDao = db.languageRoomDao()
    val language: Language = mockk()
    val language2: Language = mockk()
    val language3: Language = mockk()
    val language4: Language = mockk()
    val language5: Language = mockk()

    val languageLists = listOf(
      language, language2, language3, language4, language5
    )
    languageRoomDao.insert(languageLists)
    languageRoomDao.deleteLanguages()
    Assertions.assertEquals(0, languageRoomDao.languages().count())
  }

  @Test
  @Throws(IOException::class)
  fun migrationTest() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()

    val language: Language = mockk(relaxed = true)
    val language2: Language = mockk(relaxed = true)
    languageDao.insert(listOf(language, language2))
    db = Room.inMemoryDatabaseBuilder(
      context, KiwixRoomDatabase::class.java
    ).build()
    languageRoomDao = db.languageRoomDao()
    languageRoomDao.languages().subscribe {
      Assertions.assertEquals(2, it.size)
    }
  }
}
