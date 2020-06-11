package org.kiwix.kiwixmobile.core.history.viewmodel

import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.HistoryItem

// dateFormat = d MMM yyyy
//             5 Jul 2020
fun historyItem(
  historyTitle: String = "historyTitle",
  dateString: String = "5 Jul 2020",
  isSelected: Boolean = false,
  id: Long = 2,
  zimId: String = "zimId"
): HistoryItem {
  return HistoryItem(
    2,
    zimId,
    "zimName",
    "zimFilePath",
    "favicon",
    "historyUrl",
    historyTitle,
    dateString,
    100,
    isSelected,
    id
  )
}

fun state(
  historyItems: List<HistoryItem> = listOf(),
  showAll: Boolean = true,
  zimId: String = "id",
  searchTerm: String = ""
): HistoryState = HistoryState(historyItems, showAll, zimId, searchTerm)
