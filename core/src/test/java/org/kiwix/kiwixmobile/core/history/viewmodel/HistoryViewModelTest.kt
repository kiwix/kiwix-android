package org.kiwix.kiwixmobile.core.history.viewmodel

import OpenHistoryItem
import com.jraska.livedata.test
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.TestScheduler
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.HistoryDao
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.DateItem
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ExitActionModeMenu
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ExitHistory
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ToggleShowHistoryFromAllBooks
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.UserClickedDelete
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.UserClickedDeleteButton
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.UserClickedDeleteSelectedHistoryItems
import org.kiwix.kiwixmobile.core.history.viewmodel.State.Results
import org.kiwix.kiwixmobile.core.history.viewmodel.State.SelectionResults
import org.kiwix.kiwixmobile.core.history.viewmodel.effects.DeleteHistoryItems
import org.kiwix.kiwixmobile.core.history.viewmodel.effects.ShowDeleteHistoryDialog
import org.kiwix.kiwixmobile.core.history.viewmodel.effects.UpdateAllHistoryPreference
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.Finish
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.sharedFunctions.InstantExecutorExtension
import org.kiwix.sharedFunctions.setScheduler
import java.util.concurrent.TimeUnit.MILLISECONDS

@ExtendWith(InstantExecutorExtension::class)
internal class HistoryViewModelTest {
  private val historyDao: HistoryDao = mockk()
  private val zimReaderContainer: ZimReaderContainer = mockk()
  private val sharedPreferenceUtil: SharedPreferenceUtil = mockk()

  lateinit var viewModel: HistoryViewModel
  private val testScheduler = TestScheduler()

  init {
    setScheduler(testScheduler)
  }

  private val itemsFromDb: PublishProcessor<List<HistoryItem>> =
    PublishProcessor.create()

  @BeforeEach
  fun init() {
    clearAllMocks()
    every { zimReaderContainer.id } returns "id"
    every { zimReaderContainer.name } returns "zimName"
    every { sharedPreferenceUtil.showHistoryAllBooks } returns true
    every { historyDao.history() } returns itemsFromDb.distinctUntilChanged()
    viewModel = HistoryViewModel(historyDao, zimReaderContainer, sharedPreferenceUtil)
  }

  private fun resultsIn(st: State) {
    viewModel.state.test()
      .also { testScheduler.advanceTimeBy(100, MILLISECONDS) }
      .assertValue(st)
  }

  private fun emissionOf(
    searchTerm: String,
    databaseResults: List<HistoryItem>
  ) {
    itemsFromDb.offer(databaseResults)
    viewModel.actions.offer(Filter(searchTerm))
  }

  @Nested
  inner class StateTests {

    @Test
    fun `initial state is Initialising`() {
      viewModel.state.test().assertValue(Results(listOf(), true))
    }

    @Test
    fun `non empty search term with search results shows Results`() {
      val searchTerm = "searchTerm"
      val item =
        createSimpleHistoryItem(
          historyTitle = searchTerm
        )
      val date = DateItem(item.dateString)
      emissionOf(
        searchTerm = searchTerm,
        databaseResults = listOf(item)
      )
      resultsIn(Results(listOf(item), true))
    }

    @Test
    fun `non empty search string with no search results is NoResults`() {
      emissionOf(
        searchTerm = "a",
        databaseResults = listOf(
          createSimpleHistoryItem(
            ""
          )
        )
      )
      resultsIn(Results(emptyList(), true))
    }

    @Test
    fun `empty search string with database results shows Results`() {
      val item =
        createSimpleHistoryItem("")
      val date = DateItem(item.dateString)
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item)
      )
      resultsIn(Results(listOf(item), true))
    }

    @Test
    fun `empty search string with no database results is NoResults`() {
      emissionOf(
        searchTerm = "",
        databaseResults = emptyList()
      )
      resultsIn(Results(emptyList(), true))
    }

    @Test
    fun `duplicate search terms are ignored`() {
      val searchString1 = "a"
      val searchString2 = "b"
      val item =
        createSimpleHistoryItem("b")
      val date = DateItem(item.dateString)
      emissionOf(
        searchTerm = searchString1,
        databaseResults = listOf(item)
      )
      viewModel.actions.offer(Filter(searchString2))
      resultsIn(Results(listOf(item), true))
    }

    @Test
    fun `only latest search term is used`() {
      val item =
        createSimpleHistoryItem("b")
      val date = DateItem(item.dateString)
      emissionOf(
        searchTerm = "a",
        databaseResults = emptyList()
      )
      emissionOf(
        searchTerm = "b",
        databaseResults = listOf(item)
      )
      resultsIn(Results(listOf(item), true))
    }

    @Test
    fun `enters selection state if item is selected`() {
      val item =
        createSimpleHistoryItem(
          "b", isSelected = true
        )
      val date = DateItem(item.dateString)
      emissionOf(
        searchTerm = "a",
        databaseResults = emptyList()
      )
      emissionOf(
        searchTerm = "b",
        databaseResults = listOf(item)
      )
      resultsIn(SelectionResults(listOf(item), true))
    }

    @Test
    fun `order of date headers and items are correct`() {
      val item1 =
        createSimpleHistoryItem(
          "a", "1 Aug 2020"
        )
      val date1 = DateItem(item1.dateString)
      val item2 =
        createSimpleHistoryItem(
          "b", "2 Jun 1990"
        )
      val date2 = DateItem(item2.dateString)
      val item3 =
        createSimpleHistoryItem(
          "c", "1 Aug 2021"
        )
      val date3 = DateItem(item3.dateString)
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item2, item3, item1)
      )
      resultsIn(Results(listOf(item3, item1, item2), true))
    }

    @Test
    fun `date headers are merged if on same day`() {
      val item1 =
        createSimpleHistoryItem(
          "a", "1 Aug 2020"
        )
      val date1 = DateItem(item1.dateString)
      val item2 =
        createSimpleHistoryItem(
          "b", "1 Aug 2020"
        )
      val item3 =
        createSimpleHistoryItem(
          "c", "1 Aug 2019"
        )
      val date3 = DateItem(item3.dateString)
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item2, item3, item1)
      )
      resultsIn(Results(listOf(item2, item1, item3), true))
    }

    @Test
    fun `OnItemLongClick enters selection state`() {
      val item1 =
        createSimpleHistoryItem(
          "a", "1 Aug 2020"
        )
      val date = DateItem(item1.dateString)
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item1)
      )
      viewModel.actions.offer(OnItemLongClick(item1))
      item1.isSelected = true
      resultsIn(SelectionResults(listOf(item1), true))
    }

    @Test
    fun `Deselection via OnItemClick exits selection state if last item is deselected`() {
      val item1 =
        createSimpleHistoryItem(
          "a", "1 Aug 2020"
        )
      val item2 =
        createSimpleHistoryItem(
          "a", "1 Aug 2020"
        )
      val date = DateItem(item1.dateString)
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item1, item2)
      )
      viewModel.actions.offer(OnItemLongClick(item1))
      viewModel.actions.offer(OnItemClick(item1))
      resultsIn(Results(listOf(item1, item2), true))
    }

    @Test
    fun `Deselection via OnItemLongClick exits selection state if last item is deselected`() {
      val item1 =
        createSimpleHistoryItem(
          "a", "1 Aug 2020"
        )
      val item2 =
        createSimpleHistoryItem(
          "a", "1 Aug 2020"
        )
      val date = DateItem(item1.dateString)
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item1, item2)
      )
      viewModel.actions.offer(OnItemLongClick(item1))
      viewModel.actions.offer(OnItemLongClick(item1))
      resultsIn(Results(listOf(item1, item2), true))
    }

    @Test
    fun `ExitActionMode deselects all items`() {
      val item1 =
        createSimpleHistoryItem(
          "a", "1 Aug 2020", isSelected = true
        )
      val item2 =
        createSimpleHistoryItem(
          "a", "1 Aug 2020", isSelected = true
        )
      val date = DateItem(item1.dateString)
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item1, item2)
      )
      viewModel.actions.offer(ExitActionModeMenu)
      item1.isSelected = false
      item2.isSelected = false
      resultsIn(Results(listOf(item1, item2), true))
    }
  }

  @Nested
  inner class ActionMapping {
    @Test
    fun `ExitedSearch offers Finish`() {
      actionResultsInEffects(ExitHistory, Finish)
    }

    @Test
    fun `ExitActionModeMenu deselects all history items from state`() {
      val item1 =
        createSimpleHistoryItem(
          "a", "1 Aug 2020", isSelected = true
        )
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item1)
      )
      viewModel.actions.offer(ExitActionModeMenu)
      assertItemIsDeselected(item1)
    }

    @Test
    fun `OnItemLongClick selects history item from state`() {
      val item1 =
        createSimpleHistoryItem(
          "a", "1 Aug 2020"
        )
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item1)
      )
      viewModel.actions.offer(OnItemLongClick(item1))
      assertItemIsSelected(item1)
    }

    @Test
    fun `OnItemLongClick selects history item from state if in SelectionMode`() {
      val item1 =
        createSimpleHistoryItem(
          "a", "1 Aug 2020", id = 2
        )
      val item2 =
        createSimpleHistoryItem(
          "b", "1 Aug 2020", id = 3
        )
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item1, item2)
      )
      viewModel.actions.offer(OnItemLongClick(item1))
      viewModel.actions.offer(OnItemLongClick(item2))
      assertItemIsSelected(item1)
      assertItemIsSelected(item2)
    }

    private fun assertItemIsSelected(item: HistoryItem) {
      assertTrue(
        (viewModel.state.value?.historyListItems?.find {
          it.id == item.id
        } as HistoryItem).isSelected
      )
    }

    private fun assertItemIsDeselected(item: HistoryItem) {
      assertFalse(
        (viewModel.state.value?.historyListItems?.find {
          it.id == item.id
        } as HistoryItem).isSelected
      )
    }

    @Test
    fun `OnItemLongClick deselects history item from state if in SelectionMode`() {
      val item1 =
        createSimpleHistoryItem(
          "a", "1 Aug 2020", id = 2
        )
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item1)
      )
      viewModel.actions.offer(OnItemLongClick(item1))
      viewModel.actions.offer(OnItemLongClick(item1))
      assertItemIsDeselected(item1)
    }

    @Test
    fun `OnItemClick selects history item from state if in SelectionMode`() {
      val item1 =
        createSimpleHistoryItem(
          "a", "1 Aug 2020", id = 2
        )
      val item2 =
        createSimpleHistoryItem(
          "b", "1 Aug 2020", id = 3
        )
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item1, item2)
      )
      viewModel.actions.offer(OnItemLongClick(item1))
      viewModel.actions.offer(OnItemClick(item2))
      assertItemIsSelected(item1)
      assertItemIsSelected(item2)
    }

    @Test
    fun `OnItemClick offers OpenHistoryItem if not in selection mode `() {
      val item1 =
        createSimpleHistoryItem(
          "a", "1 Aug 2020", id = 2
        )
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item1)
      )
      actionResultsInEffects(OnItemClick(item1), OpenHistoryItem(item1, zimReaderContainer))
    }

    @Test
    fun `ToggleShowHistoryFromAllBooks switches show all books toggle`() {
      actionResultsInEffects(
        ToggleShowHistoryFromAllBooks(true),
        UpdateAllHistoryPreference(
          sharedPreferenceUtil,
          true
        )
      )
    }

    @Test
    fun `RequestDeleteAllHistoryItems opens dialog to request deletion`() {
      actionResultsInEffects(
        UserClickedDeleteButton,
        ShowDeleteHistoryDialog(viewModel.actions)
      )
    }

    @Test
    fun `RequestDeleteSelectedHistoryItems opens dialog to request deletion`() {
      actionResultsInEffects(
        UserClickedDeleteSelectedHistoryItems,
        ShowDeleteHistoryDialog(viewModel.actions)
      )
    }

    @Test
    fun `DeleteHistoryItems calls DeleteSelectedOrAllHistoryItems side effect`() {
      actionResultsInEffects(
        UserClickedDelete,
        DeleteHistoryItems(viewModel.state.value!!.historyItems, historyDao)
      )
    }

    private fun actionResultsInEffects(
      action: Action,
      vararg effects: SideEffect<*>
    ) {
      viewModel.effects
        .test()
        .also { viewModel.actions.offer(action) }
        .assertValues(*effects)
    }
  }
}
