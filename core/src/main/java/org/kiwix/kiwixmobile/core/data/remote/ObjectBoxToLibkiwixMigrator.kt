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

package org.kiwix.kiwixmobile.core.data.remote

import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import io.objectbox.kotlin.query
import io.objectbox.query.QueryBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.dao.entities.BookmarkEntity
import org.kiwix.kiwixmobile.core.dao.entities.BookmarkEntity_
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

class ObjectBoxToLibkiwixMigrator {
  @Inject lateinit var boxStore: BoxStore
  @Inject lateinit var sharedPreferenceUtil: SharedPreferenceUtil

  fun migrateBookmarksToLibkiwix() {
    CoreApp.coreComponent.inject(this)
    migrateBookMarks(boxStore.boxFor())
    // TODO we will migrate here for other entities
  }

  fun migrateBookMarks(box: Box<BookmarkEntity>) {
    val bookMarksList = box.all
    bookMarksList.forEachIndexed { _, bookmarkEntity ->
      CoroutineScope(Dispatchers.IO).launch {
        // removing the single entity from the object box after migration.
        // box.query {
        //   equal(
        //     BookmarkEntity_.bookmarkUrl,
        //     bookmarkEntity.bookmarkUrl,
        //     QueryBuilder.StringOrder.CASE_INSENSITIVE
        //   )
        // }.remove()
      }
    }
    sharedPreferenceUtil.putPrefBookMarkMigrated(true)
  }
}
