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

package org.kiwix.kiwixmobile.migration.data

import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import kotlinx.coroutines.flow.first
import org.kiwix.kiwixmobile.core.dao.HistoryRoomDao
import org.kiwix.kiwixmobile.core.dao.NotesRoomDao
import org.kiwix.kiwixmobile.core.dao.RecentSearchRoomDao
import org.kiwix.kiwixmobile.core.page.history.models.HistoryListItem
import org.kiwix.kiwixmobile.core.page.notes.models.NoteListItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.migration.entities.HistoryEntity
import org.kiwix.kiwixmobile.migration.entities.NotesEntity
import org.kiwix.kiwixmobile.migration.entities.RecentSearchEntity
import javax.inject.Inject

class ObjectBoxToRoomMigrator {
  @Inject lateinit var recentSearchRoomDao: RecentSearchRoomDao

  @Inject lateinit var historyRoomDao: HistoryRoomDao

  @Inject lateinit var notesRoomDao: NotesRoomDao

  @Inject lateinit var boxStore: BoxStore

  @Inject lateinit var kiwixDataStore: KiwixDataStore

  suspend fun migrateObjectBoxDataToRoom() {
    if (!kiwixDataStore.isRecentSearchMigrated.first()) {
      migrateRecentSearch(boxStore.boxFor())
    }
    if (!kiwixDataStore.isHistoryMigrated.first()) {
      migrateHistory(boxStore.boxFor())
    }
    if (!kiwixDataStore.isNotesMigrated.first()) {
      migrateNotes(boxStore.boxFor())
    }
    // TODO we will migrate here for other entities
  }

  suspend fun migrateRecentSearch(box: Box<RecentSearchEntity>) {
    val searchRoomEntityList = box.all
    searchRoomEntityList.forEachIndexed { _, recentSearchEntity ->
      recentSearchRoomDao
        .saveSearch(
          recentSearchEntity.searchTerm,
          recentSearchEntity.zimId,
          recentSearchEntity.url
        )
      // removing the single entity from the object box after migration.
      box.remove(recentSearchEntity.id)
    }
    kiwixDataStore.setRecentSearchMigrated(true)
  }

  suspend fun migrateHistory(box: Box<HistoryEntity>) {
    val historyEntityList = box.all
    historyEntityList.forEachIndexed { _, historyEntity ->
      historyEntity.zimFilePath?.let { filePath ->
        // set zimReaderSource for previously saved history items
        ZimReaderSource.fromDatabaseValue(filePath)?.let { zimReaderSource ->
          historyEntity.zimReaderSource = zimReaderSource
        }
      }
      historyRoomDao
        .saveHistory(
          HistoryListItem.HistoryItem(
            historyEntity.id,
            historyEntity.zimId,
            historyEntity.zimName,
            historyEntity.zimReaderSource,
            historyEntity.favicon,
            historyEntity.historyUrl,
            historyEntity.historyTitle,
            historyEntity.dateString,
            historyEntity.timeStamp,
            false
          )
        )
      // removing the single entity from the object box after migration.
      box.remove(historyEntity.id)
    }
    kiwixDataStore.setHistoryMigrated(true)
  }

  suspend fun migrateNotes(box: Box<NotesEntity>) {
    val notesEntityList = box.all
    notesEntityList.forEachIndexed { _, notesEntity ->
      notesEntity.zimFilePath?.let { filePath ->
        // set zimReaderSource for previously saved notes
        ZimReaderSource.fromDatabaseValue(filePath)?.let { zimReaderSource ->
          notesEntity.zimReaderSource = zimReaderSource
        }
      }
      notesRoomDao.saveNote(
        NoteListItem(
          notesEntity.id,
          notesEntity.zimId,
          notesEntity.noteTitle,
          notesEntity.zimReaderSource,
          notesEntity.zimUrl,
          notesEntity.noteFilePath,
          notesEntity.favicon
        )
      )
      // removing the single entity from the object box after migration.
      box.remove(notesEntity.id)
    }
    kiwixDataStore.setNotesMigrated(true)
  }
}
