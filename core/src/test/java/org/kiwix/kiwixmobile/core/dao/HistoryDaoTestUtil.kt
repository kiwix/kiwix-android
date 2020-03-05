package org.kiwix.kiwixmobile.core.dao

import io.mockk.every
import io.mockk.mockk
import org.kiwix.kiwixmobile.core.history.HistoryListItem


fun mockkHistoryItemWithTitle(historyTitle: String): HistoryListItem.HistoryItem {
  val historyItem = mockk<HistoryListItem.HistoryItem>()
  every { historyItem.historyUrl } returns "url$historyTitle"
  every { historyItem.dateString } returns "2012-01-01"
  every { historyItem.databaseId } returns 0
  every { historyItem.zimId } returns "zimId"
  every { historyItem.zimName } returns "zimName$historyTitle"
  every { historyItem.zimFilePath } returns "zimPath$historyTitle"
  every { historyItem.favicon } returns "favicon"
  every { historyItem.historyTitle } returns historyTitle
  every { historyItem.timeStamp } returns 0
  return historyItem
}
