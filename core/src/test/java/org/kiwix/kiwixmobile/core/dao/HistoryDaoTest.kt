package org.kiwix.kiwixmobile.core.dao

import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.dao.entities.MyObjectBox
import org.kiwix.kiwixmobile.core.history.HistoryListItem.HistoryItem
import org.kiwix.sharedFunctions.historyItem
import java.io.File

internal class HistoryDaoTest {
  private val testDirectory = File("src/test/java/org/kiwix/kiwixmobile/core/dao/test-db")
  private lateinit var boxStore: BoxStore
  private lateinit var historyDao: HistoryDao

  @BeforeEach
  fun setUp() {
    BoxStore.deleteAllFiles(testDirectory)
    boxStore = MyObjectBox.builder().directory(testDirectory).build()
    historyDao = HistoryDao(boxStore.boxFor())
  }

  @AfterEach
  fun tearDown() {
    boxStore.close()
    BoxStore.deleteAllFiles(testDirectory)
  }

  @Test
  fun `saveHistory should save single history item`() {
    var historyItem = historyItemWithTitle("1")
    historyDao.saveHistory(historyItem)

    val historyList: List<HistoryItem> = historyDao
      .getHistoryList(false, "")

    historyItem = createSameHistoryItemWithModifiedDatabaseId(historyItem, 1)
    assertEquals(historyList[0], historyItem)
  }

  @Nested
  inner class GetHistoryListDesigns {
    @Test
    fun `getHistoryList should return item with path if only showing current book history`() {
      var historyItem1 = historyItemWithTitle("1")
      historyDao.saveHistory(historyItem1)

      val historyTitleList: List<HistoryItem> = historyDao
        .getHistoryList(true, "zimPath1")

      historyItem1 = createSameHistoryItemWithModifiedDatabaseId(historyItem1, 1)
      assertEquals(historyTitleList[0], historyItem1)
    }

    @Test
    fun `getHistoryList should not return item with invalid path if only showing book history`() {
      val historyItem1 = historyItemWithTitle("1")
      historyDao.saveHistory(historyItem1)

      val historyTitleList: List<HistoryItem> = historyDao
        .getHistoryList(true, "not/path")

      assertTrue(historyTitleList.isEmpty())
    }

    @Test
    fun `getHistoryList should return multiple inserted history items`() {
      var historyItem1 = historyItemWithTitle("1")
      var historyItem2 = historyItemWithTitle("2")
      var historyItem3 = historyItemWithTitle("3")
      historyDao.saveHistory(historyItem1)
      historyDao.saveHistory(historyItem2)
      historyDao.saveHistory(historyItem3)

      val historyTitleList: List<HistoryItem> = historyDao
        .getHistoryList(false, "")

      historyItem1 = createSameHistoryItemWithModifiedDatabaseId(historyItem1, 1)
      historyItem2 = createSameHistoryItemWithModifiedDatabaseId(historyItem2, 2)
      historyItem3 = createSameHistoryItemWithModifiedDatabaseId(historyItem3, 3)

      assertTrue(historyTitleList.contains(historyItem1))
      assertTrue(historyTitleList.contains(historyItem2))
      assertTrue(historyTitleList.contains(historyItem3))
    }
  }

  @Nested
  inner class DeleteHistoryDesigns {
    @Test
    fun `deleteHistory should delete selected item`() {
      var historyItem1 = historyItemWithTitle("1")
      var historyItem2 = historyItemWithTitle("2")
      var historyItem3 = historyItemWithTitle("3")
      historyDao.saveHistory(historyItem1)
      historyDao.saveHistory(historyItem2)
      historyDao.saveHistory(historyItem3)
      historyItem1 = createSameHistoryItemWithModifiedDatabaseId(historyItem1, 1)
      historyDao.deleteHistory(listOf(historyItem1))

      val historyTitleList: List<HistoryItem> = historyDao
        .getHistoryList(false, "")

      historyItem2 = createSameHistoryItemWithModifiedDatabaseId(historyItem2, 2)
      historyItem3 = createSameHistoryItemWithModifiedDatabaseId(historyItem3, 3)
      assertFalse(historyTitleList.contains(historyItem1))
      assertTrue(historyTitleList.contains(historyItem2))
      assertTrue(historyTitleList.contains(historyItem3))
    }

    @Test
    fun `deleteHistory should delete multiple selected items`() {
      var historyItem1 = historyItemWithTitle("1")
      var historyItem2 = historyItemWithTitle("2")
      var historyItem3 = historyItemWithTitle("3")
      historyDao.saveHistory(historyItem1)
      historyDao.saveHistory(historyItem2)
      historyDao.saveHistory(historyItem3)
      historyItem1 = createSameHistoryItemWithModifiedDatabaseId(historyItem1, 1)
      historyItem2 = createSameHistoryItemWithModifiedDatabaseId(historyItem2, 2)
      historyDao.deleteHistory(listOf(historyItem1, historyItem2))

      val historyTitleList: List<HistoryItem> = historyDao
        .getHistoryList(false, "")

      historyItem3 = createSameHistoryItemWithModifiedDatabaseId(historyItem3, 3)
      assertFalse(historyTitleList.contains(historyItem1))
      assertFalse(historyTitleList.contains(historyItem2))
      assertTrue(historyTitleList.contains(historyItem3))
    }
  }

  @Test
  fun `deleteAllHistory should delete all history`() {
    var historyItem1 = historyItemWithTitle("1")
    var historyItem2 = historyItemWithTitle("2")
    var historyItem3 = historyItemWithTitle("3")
    historyDao.saveHistory(historyItem1)
    historyDao.saveHistory(historyItem2)
    historyDao.saveHistory(historyItem3)
    historyDao.deleteAllHistory()

    val historyTitleList: List<HistoryItem> = historyDao
      .getHistoryList(false, "")

    historyItem1 = createSameHistoryItemWithModifiedDatabaseId(historyItem1, 1)
    historyItem2 = createSameHistoryItemWithModifiedDatabaseId(historyItem2, 2)
    historyItem3 = createSameHistoryItemWithModifiedDatabaseId(historyItem3, 3)
    assertFalse(historyTitleList.contains(historyItem1))
    assertFalse(historyTitleList.contains(historyItem2))
    assertFalse(historyTitleList.contains(historyItem3))
  }
}

fun historyItemWithTitle(historyTitle: String): HistoryItem =
  historyItem(
    "url$historyTitle",
    "2012-01-01",
    0,
    "zimId",
    "zimName$historyTitle",
    "zimPath$historyTitle",
    "favicon",
    historyTitle,
    0
  )

private fun createSameHistoryItemWithModifiedDatabaseId(
  historyItem: HistoryItem,
  databaseId: Long
): HistoryItem =
  historyItem(
    historyItem.historyUrl,
    historyItem.dateString,
    databaseId,
    historyItem.zimId,
    historyItem.zimName,
    historyItem.zimFilePath,
    historyItem.favicon,
    historyItem.historyTitle,
    historyItem.timeStamp
  )
