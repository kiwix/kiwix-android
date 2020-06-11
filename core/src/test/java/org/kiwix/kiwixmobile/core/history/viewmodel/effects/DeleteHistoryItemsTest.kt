package org.kiwix.kiwixmobile.core.history.viewmodel.effects

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.dao.HistoryDao
import org.kiwix.kiwixmobile.core.history.viewmodel.HistoryState
import org.kiwix.kiwixmobile.core.history.viewmodel.historyItem

internal class DeleteHistoryItemsTest {

  @Test
  fun `delete with selected items only deletes the selected items`() {
    val item1 = historyItem(isSelected = true)
    val item2 = historyItem()
    val state: MutableLiveData<HistoryState> = mockk()
    every { state.value } returns HistoryState(listOf(item1, item2), true, "", "")
    val historyDao: HistoryDao = mockk()
    val activity: AppCompatActivity = mockk()
    DeleteHistoryItems(state.value!!, historyDao).invokeWith(activity)
    verify { historyDao.deleteHistory(listOf(item1)) }
  }

  @Test
  fun `delete with no selected items deletes all items`() {
    val item1 = historyItem()
    val item2 = historyItem()
    val state: MutableLiveData<HistoryState> = mockk()
    every { state.value } returns HistoryState(listOf(item1, item2), true, "", "")
    val historyDao: HistoryDao = mockk()
    val activity: AppCompatActivity = mockk()
    DeleteHistoryItems(state.value!!, historyDao).invokeWith(activity)
    verify { historyDao.deleteHistory(listOf(item1, item2)) }
  }
}
