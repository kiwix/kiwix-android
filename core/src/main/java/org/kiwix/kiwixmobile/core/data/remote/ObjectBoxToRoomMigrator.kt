/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.data.remote

import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.dao.entities.RecentSearchEntity
import org.kiwix.kiwixmobile.core.data.KiwixRoomDatabase
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

class ObjectBoxToRoomMigrator {
  @Inject lateinit var kiwixRoomDatabase: KiwixRoomDatabase
  @Inject lateinit var boxStore: BoxStore
  @Inject lateinit var sharedPreferenceUtil: SharedPreferenceUtil

  suspend fun migrateObjectBoxDataToRoom() {
    CoreApp.coreComponent.inject(this)
    migrateRecentSearch(boxStore.boxFor())
    // TODO we will migrate here for other entities
  }

  suspend fun migrateRecentSearch(box: Box<RecentSearchEntity>) {
    val searchRoomEntityList = box.all
    searchRoomEntityList.forEachIndexed { _, recentSearchEntity ->
      kiwixRoomDatabase.recentSearchRoomDao()
        .saveSearch(
          recentSearchEntity.searchTerm,
          recentSearchEntity.zimId,
          recentSearchEntity.url
        )
      // removing the single entity from the object box after migration.
      box.remove(recentSearchEntity.id)
    }
    sharedPreferenceUtil.putPrefRecentSearchMigrated(true)
  }
}
