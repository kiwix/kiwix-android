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

package org.kiwix.kiwixmobile

import org.kiwix.kiwixmobile.core.page.history.models.HistoryListItem
import org.kiwix.kiwixmobile.core.page.notes.models.NoteListItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import java.io.File

/**
 * Shared test-data factory referenced by both instrumented tests
 * (e.g. [ObjectBoxToRoomMigratorTest]) and unit tests.
 *
 * This object lives in `androidTest` so that instrumented tests can
 * still resolve these helpers after the main `KiwixRoomDatabaseTest`
 * was migrated to the `test` source set.
 */
class KiwixRoomDatabaseTest {
  companion object {
    fun getHistoryItem(
      title: String = "Installation",
      historyUrl: String = "https://kiwix.app/A/Installation",
      dateString: String = "30 May 2024",
      databaseId: Long = 0L,
      zimId: String = "1f88ab6f-c265-b-3ff-8f49-b7f4429503800",
      zimName: String = "alpinelinux_en_all",
      zimReaderSource: ZimReaderSource =
        ZimReaderSource(
          File("/storage/emulated/0/Download/alpinelinux_en_all_maxi_2023-01.zim")
        ),
      timeStamp: Long = System.currentTimeMillis()
    ): HistoryListItem.HistoryItem =
      HistoryListItem.HistoryItem(
        databaseId = databaseId,
        zimId = zimId,
        zimName = zimName,
        historyUrl = historyUrl,
        title = title,
        zimReaderSource = zimReaderSource,
        favicon = null,
        dateString = dateString,
        timeStamp = timeStamp
      )

    fun getNoteListItem(
      databaseId: Long = 0L,
      zimId: String = "1f88ab6f-c265-b-3ff-8f49-b7f4429503800",
      title: String = "Alpine Wiki",
      zimReaderSource: ZimReaderSource =
        ZimReaderSource(
          File("/storage/emulated/0/Download/alpinelinux_en_all_maxi_2023-01.zim")
        ),
      zimUrl: String,
      noteFilePath: String =
        "/storage/emulated/0/Download/Notes/Alpine linux/AlpineNote.txt"
    ): NoteListItem =
      NoteListItem(
        databaseId = databaseId,
        zimId = zimId,
        title = title,
        zimReaderSource = zimReaderSource,
        zimUrl = zimUrl,
        noteFilePath = noteFilePath,
        null,
        false
      )
  }
}
