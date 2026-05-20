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

package org.kiwix.kiwixmobile.dao

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.kiwix.kiwixmobile.core.dao.HistoryRoomDaoCoverts
import org.kiwix.kiwixmobile.core.dao.entities.HistoryRoomEntity
import org.kiwix.kiwixmobile.core.page.history.models.HistoryListItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import java.io.File

/**
 * Tests for the [HistoryRoomDaoCoverts] TypeConverter class, verifying
 * correct bidirectional mapping between [HistoryRoomEntity] and
 * [HistoryListItem.HistoryItem].
 */
class HistoryRoomDaoConvertsTest {
  private lateinit var converter: HistoryRoomDaoCoverts

  @Before
  fun setUp() {
    converter = HistoryRoomDaoCoverts()
  }

  @Test
  fun fromHistoryRoomEntity_mapsAllFieldsCorrectly() {
    val zimReaderSource = ZimReaderSource(File("/path/to/file.zim"))
    val entity = HistoryRoomEntity(
      id = 42L,
      zimId = "zim-id-123",
      zimName = "wikipedia_en",
      zimFilePath = "/path/to/file.zim",
      zimReaderSource = zimReaderSource,
      favicon = "favicon-base64",
      historyUrl = "https://kiwix.app/A/Main_Page",
      historyTitle = "Main Page",
      dateString = "15 Jun 2024",
      timeStamp = 1718400000L
    )

    val result = converter.fromHistoryRoomEntity(entity)
    val historyItem = result as HistoryListItem.HistoryItem

    assertEquals(42L, historyItem.databaseId)
    assertEquals("zim-id-123", historyItem.zimId)
    assertEquals("wikipedia_en", historyItem.zimName)
    assertEquals(zimReaderSource, historyItem.zimReaderSource)
    assertEquals("favicon-base64", historyItem.favicon)
    assertEquals("https://kiwix.app/A/Main_Page", historyItem.historyUrl)
    assertEquals("Main Page", historyItem.title)
    assertEquals("15 Jun 2024", historyItem.dateString)
    assertEquals(1718400000L, historyItem.timeStamp)
  }

  @Test
  fun fromHistoryRoomEntity_handlesNullFavicon() {
    val entity = HistoryRoomEntity(
      id = 1L,
      zimId = "zim-1",
      zimName = "test",
      zimFilePath = null,
      zimReaderSource = null,
      favicon = null,
      historyUrl = "https://kiwix.app/A/Test",
      historyTitle = "Test",
      dateString = "01 Jan 2024",
      timeStamp = 100L
    )

    val result = converter.fromHistoryRoomEntity(entity) as HistoryListItem.HistoryItem
    assertNull(result.favicon)
  }

  @Test
  fun fromHistoryRoomEntity_handlesNullZimReaderSource() {
    val entity = HistoryRoomEntity(
      id = 1L,
      zimId = "zim-1",
      zimName = "test",
      zimFilePath = null,
      zimReaderSource = null,
      favicon = null,
      historyUrl = "https://kiwix.app/A/Test",
      historyTitle = "Test",
      dateString = "01 Jan 2024",
      timeStamp = 100L
    )

    val result = converter.fromHistoryRoomEntity(entity) as HistoryListItem.HistoryItem
    assertNull(result.zimReaderSource)
  }

  @Test
  fun fromHistoryRoomEntity_handlesEmptyStrings() {
    val entity = HistoryRoomEntity(
      id = 0L,
      zimId = "",
      zimName = "",
      zimFilePath = "",
      zimReaderSource = null,
      favicon = "",
      historyUrl = "",
      historyTitle = "",
      dateString = "",
      timeStamp = 0L
    )

    val result = converter.fromHistoryRoomEntity(entity) as HistoryListItem.HistoryItem
    assertEquals("", result.zimId)
    assertEquals("", result.zimName)
    assertEquals("", result.historyUrl)
    assertEquals("", result.title)
    assertEquals("", result.dateString)
  }

  @Test
  fun historyItemToHistoryListItem_mapsAllFieldsCorrectly() {
    val zimReaderSource = ZimReaderSource(File("/path/to/file.zim"))
    val historyItem = HistoryListItem.HistoryItem(
      databaseId = 99L,
      zimId = "zim-id-456",
      zimName = "alpinelinux_en",
      zimReaderSource = zimReaderSource,
      favicon = "favicon-data",
      historyUrl = "https://kiwix.app/A/Installation",
      title = "Installation",
      dateString = "20 Jul 2024",
      timeStamp = 1721500000L
    )

    val entity = converter.historyItemToHistoryListItem(historyItem)

    assertEquals(99L, entity.id)
    assertEquals("zim-id-456", entity.zimId)
    assertEquals("alpinelinux_en", entity.zimName)
    assertEquals(zimReaderSource, entity.zimReaderSource)
    assertEquals("favicon-data", entity.favicon)
    assertEquals("https://kiwix.app/A/Installation", entity.historyUrl)
    assertEquals("Installation", entity.historyTitle)
    assertEquals("20 Jul 2024", entity.dateString)
    assertEquals(1721500000L, entity.timeStamp)
  }

  @Test
  fun historyItemToHistoryListItem_handlesNullFavicon() {
    val historyItem = HistoryListItem.HistoryItem(
      databaseId = 1L,
      zimId = "zim-1",
      zimName = "test",
      zimReaderSource = null,
      favicon = null,
      historyUrl = "https://kiwix.app/A/Test",
      title = "Test",
      dateString = "01 Jan 2024",
      timeStamp = 100L
    )

    val entity = converter.historyItemToHistoryListItem(historyItem)
    assertNull(entity.favicon)
  }

  @Test
  fun historyItemToHistoryListItem_handlesNullZimReaderSource() {
    val historyItem = HistoryListItem.HistoryItem(
      databaseId = 1L,
      zimId = "zim-1",
      zimName = "test",
      zimReaderSource = null,
      favicon = null,
      historyUrl = "https://kiwix.app/A/Test",
      title = "Test",
      dateString = "01 Jan 2024",
      timeStamp = 100L
    )

    val entity = converter.historyItemToHistoryListItem(historyItem)
    assertNull(entity.zimReaderSource)
  }

  @Test
  fun roundTrip_entityToItemToEntity_preservesData() {
    val zimReaderSource = ZimReaderSource(File("/path/to/file.zim"))
    val original = HistoryRoomEntity(
      id = 7L,
      zimId = "roundtrip-zim",
      zimName = "roundtrip_name",
      zimFilePath = null,
      zimReaderSource = zimReaderSource,
      favicon = "round-favicon",
      historyUrl = "https://kiwix.app/A/RoundTrip",
      historyTitle = "Round Trip",
      dateString = "10 Oct 2024",
      timeStamp = 1728500000L
    )

    val historyItem = converter.fromHistoryRoomEntity(original) as HistoryListItem.HistoryItem
    val roundTripped = converter.historyItemToHistoryListItem(historyItem)

    assertEquals(original.id, roundTripped.id)
    assertEquals(original.zimId, roundTripped.zimId)
    assertEquals(original.zimName, roundTripped.zimName)
    assertEquals(original.zimReaderSource, roundTripped.zimReaderSource)
    assertEquals(original.favicon, roundTripped.favicon)
    assertEquals(original.historyUrl, roundTripped.historyUrl)
    assertEquals(original.historyTitle, roundTripped.historyTitle)
    assertEquals(original.dateString, roundTripped.dateString)
    assertEquals(original.timeStamp, roundTripped.timeStamp)
  }
}
