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
package org.kiwix.kiwixmobile.core.data.local.dao

import org.kiwix.kiwixmobile.core.data.local.KiwixDatabase
import com.yahoo.squidb.sql.Query
import org.kiwix.kiwixmobile.core.data.local.entity.NetworkLanguageDatabaseEntity
import org.kiwix.kiwixmobile.core.zim_manager.Language
import java.lang.Exception
import java.util.ArrayList
import javax.inject.Inject

@Deprecated("")
class NetworkLanguageDao @Inject constructor(private val mDb: KiwixDatabase) {
  val filteredLanguages: ArrayList<Language>
    @Suppress("TooGenericExceptionCaught")
    get() {
      val result = ArrayList<Language>()
      try {
        val languageCursor = mDb.query(
          NetworkLanguageDatabaseEntity::class.java,
          Query.select()
        )
        while (languageCursor.moveToNext()) {
          val languageCode =
            languageCursor.get(NetworkLanguageDatabaseEntity.LANGUAGE_I_S_O_3) ?: ""
          val enabled = languageCursor.get(NetworkLanguageDatabaseEntity.ENABLED) ?: false
          result.add(Language(languageCode, enabled, 0))
        }
      } catch (exception: Exception) {
        exception.printStackTrace()
      }
      return result
    }
}
