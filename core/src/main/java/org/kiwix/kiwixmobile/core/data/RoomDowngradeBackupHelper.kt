/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase

object RoomDowngradeBackupHelper {
  private const val NOTES_TABLE = "NotesRoomEntity"
  private const val HISTORY_TABLE = "HistoryRoomEntity"
  private const val RECENT_SEARCH_TABLE = "RecentSearchRoomEntity"

  data class RowSnapshot(val values: Map<String, Any?>)

  data class DatabaseSnapshot(
    val notes: List<RowSnapshot>,
    val history: List<RowSnapshot>,
    val recentSearches: List<RowSnapshot>
  )

  const val DB_NAME = "KiwixRoom.db"
  const val CURRENT_ROOM_DB_VERSION = 10
  private const val ROOM_DOWNGRADE_HELPER = "RoomDowngradeHelper"

  fun isDowngrade(context: Context, targetVersion: Int): Boolean {
    val existingVersion = getExistingDbVersion(context) ?: return false
    return existingVersion > targetVersion
  }

  fun createSnapshot(context: Context): DatabaseSnapshot {
    val dbFile = context.getDatabasePath(DB_NAME)
    if (!dbFile.exists()) {
      return DatabaseSnapshot(emptyList(), emptyList(), emptyList())
    }

    val sqliteDb = SQLiteDatabase.openDatabase(
      dbFile.path,
      null,
      SQLiteDatabase.OPEN_READONLY
    )

    val snapshot = DatabaseSnapshot(
      notes = backupTable(sqliteDb, NOTES_TABLE),
      history = backupTable(sqliteDb, HISTORY_TABLE),
      recentSearches = backupTable(sqliteDb, RECENT_SEARCH_TABLE)
    )

    sqliteDb.close()
    return snapshot
  }

  fun restoreSnapshot(
    db: SupportSQLiteDatabase,
    snapshot: DatabaseSnapshot
  ) {
    restoreTableRows(db, NOTES_TABLE, snapshot.notes)
    restoreTableRows(db, HISTORY_TABLE, snapshot.history)
    restoreTableRows(db, RECENT_SEARCH_TABLE, snapshot.recentSearches)
  }

  private fun getExistingDbVersion(context: Context): Int? {
    val dbFile = context.getDatabasePath(DB_NAME)
    if (!dbFile.exists()) return null

    val db = SQLiteDatabase.openDatabase(
      dbFile.path,
      null,
      SQLiteDatabase.OPEN_READONLY
    )

    val version = db.version
    db.close()
    return version
  }

  private fun backupTable(
    db: SQLiteDatabase,
    tableName: String
  ): List<RowSnapshot> {
    return try {
      db.rawQuery("SELECT * FROM $tableName", null).use { cursor ->
        readAllRows(cursor)
      }
    } catch (ignore: Exception) {
      Log.e(ROOM_DOWNGRADE_HELPER, "Can not make backup of database. original exception = $ignore")
      emptyList()
    }
  }

  private fun readAllRows(cursor: Cursor): List<RowSnapshot> {
    val rows = mutableListOf<RowSnapshot>()

    val columnNames = cursor.columnNames
    val columnIndexMap = mapColumnIndices(cursor, columnNames)

    while (cursor.moveToNext()) {
      rows.add(RowSnapshot(readRow(cursor, columnIndexMap)))
    }

    return rows
  }

  private fun mapColumnIndices(
    cursor: Cursor,
    columnNames: Array<String>
  ): Map<String, Int> {
    val indexMap = HashMap<String, Int>(columnNames.size)

    for (column in columnNames) {
      val index = cursor.getColumnIndex(column)
      if (index != -1) {
        indexMap[column] = index
      }
    }

    return indexMap
  }

  private fun readRow(
    cursor: Cursor,
    columnIndexMap: Map<String, Int>
  ): Map<String, Any?> {
    val row = HashMap<String, Any?>(columnIndexMap.size)

    for ((column, index) in columnIndexMap) {
      row[column] = readCursorValue(cursor, index)
    }

    return row
  }

  private fun readCursorValue(cursor: Cursor, index: Int): Any? {
    return when (cursor.getType(index)) {
      Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index)
      Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index)
      Cursor.FIELD_TYPE_STRING -> cursor.getString(index)
      Cursor.FIELD_TYPE_BLOB -> cursor.getBlob(index)
      Cursor.FIELD_TYPE_NULL -> null
      else -> null
    }
  }

  private fun getExistingColumns(
    db: SupportSQLiteDatabase,
    tableName: String
  ): Set<String> {
    val columns = mutableSetOf<String>()

    try {
      db.query("PRAGMA table_info($tableName)").use { cursor ->
        while (cursor.moveToNext()) {
          columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
      }
    } catch (ignore: Exception) {
      Log.e(ROOM_DOWNGRADE_HELPER, "Can not get existing column. original exception = $ignore")
    }

    return columns
  }

  private fun restoreTableRows(
    db: SupportSQLiteDatabase,
    tableName: String,
    rows: List<RowSnapshot>
  ) {
    if (rows.isEmpty()) return

    val existingColumns = getExistingColumns(db, tableName)
    if (existingColumns.isEmpty()) return

    val insertableColumns = existingColumns - "id"

    db.beginTransaction()
    try {
      var cachedSql: String? = null
      var cachedColumnOrder: List<String>? = null

      for (row in rows) {
        val filteredRow = row.values.filterKeys { it in insertableColumns }
        if (filteredRow.isEmpty()) continue

        val columnOrder = filteredRow.keys.sorted()

        if (cachedSql == null || cachedColumnOrder != columnOrder) {
          val columnList = columnOrder.joinToString(", ")
          val placeholders = columnOrder.joinToString(", ") { "?" }

          cachedSql =
            "INSERT OR REPLACE INTO $tableName ($columnList) VALUES ($placeholders)"

          cachedColumnOrder = columnOrder
        }

        val args = Array(columnOrder.size) { i ->
          filteredRow[columnOrder[i]]
        }

        db.execSQL(cachedSql, args)
      }

      db.setTransactionSuccessful()
    } finally {
      db.endTransaction()
    }
  }
}
