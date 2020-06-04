package org.kiwix.kiwixmobile.core.history.viewmodel.effects

import DeleteSelectedOrAllHistoryItems
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.dao.HistoryDao
import org.kiwix.kiwixmobile.core.history.viewmodel.State
import org.kiwix.kiwixmobile.core.history.viewmodel.State.SelectionResults
import org.kiwix.kiwixmobile.core.history.viewmodel.createSimpleHistoryItem

internal class DeleteSelectedOrAllHistoryItemsTest {

  @Test
  fun `delete with selected items only deletes the selected items`() {
    val item1 = createSimpleHistoryItem(isSelected = true)
    val item2 = createSimpleHistoryItem()
    val state: MutableLiveData<State> = mockk()
    every { state.value } returns SelectionResults(listOf(item1, item2))
    val historyDao: HistoryDao = mockk()
    val activity: AppCompatActivity = mockk()
    DeleteSelectedOrAllHistoryItems(state, historyDao).invokeWith(activity)
    verify { historyDao.deleteHistory(listOf(item1)) }
  }

  @Test
  fun `delete with no selected items deletes all items`() {
    val item1 = createSimpleHistoryItem()
    val item2 = createSimpleHistoryItem()
    val state: MutableLiveData<State> = mockk()
    every { state.value } returns SelectionResults(listOf(item1, item2))
    val historyDao: HistoryDao = mockk()
    val activity: AppCompatActivity = mockk()
    DeleteSelectedOrAllHistoryItems(state, historyDao).invokeWith(activity)
    verify { historyDao.deleteHistory(listOf(item1, item2)) }
  }
}
