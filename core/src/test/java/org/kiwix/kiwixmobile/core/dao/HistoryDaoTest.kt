package org.kiwix.kiwixmobile.core.dao

import io.mockk.every
import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import org.junit.After
import org.junit.Before
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.kiwix.kiwixmobile.core.dao.entities.MyObjectBox
import org.kiwix.kiwixmobile.core.history.HistoryListItem.HistoryItem
import java.io.File
import java.io.IOException

internal class HistoryDaoTest {
  private val testDirectory = File("src/test/java/org/kiwix/kiwixmobile/core/dao/test-db")
  private lateinit var boxStore : BoxStore
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
    val historyItem = mockkHistoryItemWithTitle("testHistoryTitle")
    historyDao.saveHistory(historyItem)

    val historyList: List<HistoryItem> = historyDao
      .getHistoryList(false, "")

    assertEquals(historyItem.historyTitle, historyList[0].historyTitle)
  }

  @Test
  fun `getHistoryList should return item with canonicalPath if only showing current book history`(){
    val historyItem1 = mockkHistoryItemWithTitle("1")
    every { historyItem1.zimFilePath } returns "path"
    historyDao.saveHistory(historyItem1)

    val historyTitleList: List<String> = historyDao
      .getHistoryList(true, "path")
      .map { historyItem -> historyItem.historyTitle }

    assertTrue(historyTitleList.contains(historyItem1.historyTitle))
  }


  @Test
  fun `getHistoryList should not return item with invalid canonicalPath if only showing current book history`(){
    val historyItem1 = mockkHistoryItemWithTitle("1")
    every { historyItem1.zimFilePath } returns "path"
    historyDao.saveHistory(historyItem1)

    val historyTitleList: List<String> = historyDao
      .getHistoryList(true, "not/path")
      .map { historyItem -> historyItem.historyTitle }

    assertFalse(historyTitleList.contains(historyItem1.historyTitle))
  }

  @Test
  fun `getHistoryList should return multiple inserted history items`() {
    val historyItem1 = mockkHistoryItemWithTitle("1")
    val historyItem2 = mockkHistoryItemWithTitle("2")
    val historyItem3 = mockkHistoryItemWithTitle("3")
    historyDao.saveHistory(historyItem1)
    historyDao.saveHistory(historyItem2)
    historyDao.saveHistory(historyItem3)

    val historyTitleList: List<String> = historyDao
      .getHistoryList(false, "")
      .map { historyItem -> historyItem.historyTitle }

    assertTrue(historyTitleList.contains(historyItem1.historyTitle))
    assertTrue(historyTitleList.contains(historyItem2.historyTitle))
    assertTrue(historyTitleList.contains(historyItem3.historyTitle))
  }

  @Test
  fun `deleteHistory should delete selected item`() {
    val historyItem1 = mockkHistoryItemWithTitle("1")
    val historyItem2 = mockkHistoryItemWithTitle("2")
    val historyItem3 = mockkHistoryItemWithTitle("3")
    historyDao.saveHistory(historyItem1)
    historyDao.saveHistory(historyItem2)
    historyDao.saveHistory(historyItem3)
    every { historyItem1.databaseId } returns 1
    historyDao.deleteHistory(listOf(historyItem1))

    val historyTitleList: List<String> = historyDao
      .getHistoryList(false, "")
      .map { historyItem -> historyItem.historyTitle }

    assertFalse(historyTitleList.contains(historyItem1.historyTitle))
    assertTrue(historyTitleList.contains(historyItem2.historyTitle))
    assertTrue(historyTitleList.contains(historyItem3.historyTitle))
  }


  @Test
  fun `deleteHistory should delete multiple selected items`() {
    val historyItem1 = mockkHistoryItemWithTitle("1")
    val historyItem2 = mockkHistoryItemWithTitle("2")
    val historyItem3 = mockkHistoryItemWithTitle("3")
    historyDao.saveHistory(historyItem1)
    historyDao.saveHistory(historyItem2)
    historyDao.saveHistory(historyItem3)
    every { historyItem1.databaseId } returns 1
    every { historyItem2.databaseId } returns 2
    historyDao.deleteHistory(listOf(historyItem1, historyItem2))

    val historyTitleList: List<String> = historyDao
      .getHistoryList(false, "")
      .map { historyItem -> historyItem.historyTitle }

    assertFalse(historyTitleList.contains(historyItem1.historyTitle))
    assertFalse(historyTitleList.contains(historyItem2.historyTitle))
    assertTrue(historyTitleList.contains(historyItem3.historyTitle))
  }

  @Test
  fun `deleteAllHistory should delete all history`() {
    val historyItem1 = mockkHistoryItemWithTitle("1")
    val historyItem2 = mockkHistoryItemWithTitle("2")
    val historyItem3 = mockkHistoryItemWithTitle("3")
    historyDao.saveHistory(historyItem1)
    historyDao.saveHistory(historyItem2)
    historyDao.saveHistory(historyItem3)
    historyDao.deleteAllHistory()

    val historyTitleList: List<String> = historyDao
      .getHistoryList(false, "")
      .map { historyItem -> historyItem.historyTitle }

    assertFalse(historyTitleList.contains(historyItem1.historyTitle))
    assertFalse(historyTitleList.contains(historyItem2.historyTitle))
    assertFalse(historyTitleList.contains(historyItem3.historyTitle))

  }
}
