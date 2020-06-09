package org.kiwix.kiwixmobile.core.history.viewmodel

import com.jraska.livedata.test
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.HistoryDao
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.DateItem
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ExitActionModeMenu
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ExitHistory
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.UserClickedConfirmDelete
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.UserClickedDeleteButton
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.UserClickedDeleteSelectedHistoryItems
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.UserClickedShowAllToggle
import org.kiwix.kiwixmobile.core.history.viewmodel.effects.DeleteHistoryItems
import org.kiwix.kiwixmobile.core.history.viewmodel.effects.OpenHistoryItem
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
    RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
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
      viewModel.state.test().assertValue(State(listOf(), true, "id", ""))
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
      resultsIn(State(listOf(item), true, "id", "searchTerm"))
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
      resultsIn(State(listOf(item), true, "id", ""))
    }

    @Test
    fun `empty search string with no database results is NoResults`() {
      emissionOf(
        searchTerm = "",
        databaseResults = emptyList()
      )
      resultsIn(State(emptyList(), true, "id", ""))
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
      resultsIn(State(listOf(item), true, "id", "b"))
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
      resultsIn(State(listOf(item), true, "id", "b"))
    }

    @Test
    fun `enters selection state if item is selected`() {
      val item =
        createSimpleHistoryItem(
          "b"
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
      viewModel.actions.offer(OnItemLongClick(item))
      resultsIn(State(listOf(item), true, "id", "b"))
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
      assertEquals(
        State(listOf(item3, item1, item2), true, "id", "")
          .getHistoryListItems(),
        listOf(date3, item3, date1, item1, date2, item2)
      )
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
      assertEquals(
        State(listOf(item3, item1, item2), true, "id", "")
          .getHistoryListItems(),
        listOf(date1, item1, item2, date3, item3)
      )
    }

    @Test
    fun `OnItemLongClick enters selection state`() {
      val item1 =
        createSimpleHistoryItem(
          "a", "1 Aug 2020"
        )
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item1)
      )
      viewModel.actions.offer(OnItemLongClick(item1))
      item1.isSelected = true
      resultsIn(State(listOf(item1), true, "id", ""))
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
      resultsIn(State(listOf(item1, item2), true, "id", ""))
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
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item1, item2)
      )
      viewModel.actions.offer(ExitActionModeMenu)
      item1.isSelected = false
      item2.isSelected = false
      resultsIn(State(listOf(item1, item2), true, "id", ""))
    }
  }

  @Nested
  inner class GetHistoryItemsList {

    @Test
    fun `non empty search string with no search results is empty history item list`() {
      emissionOf(
        searchTerm = "a",
        databaseResults = listOf(
          createSimpleHistoryItem(
            ""
          )
        )
      )
      assertEquals(emptyList<HistoryListItem>(), viewModel.state.value?.getHistoryListItems())
      // resultsIn(Results(emptyList(), true, "id", "a"))
    }

    @Test
    fun `showAllHistory results in all history being shown`() {
      val item1 = createSimpleHistoryItem(zimId = "notCurrentId1")
      val item2 = createSimpleHistoryItem(zimId = "notCurrentId")
      val date = DateItem(item1.dateString)
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item1, item2)
      )
      viewModel.actions.offer(UserClickedShowAllToggle(true))
      assertEquals(listOf(date, item1, item2), viewModel.state.value?.getHistoryListItems())
    }

    @Test
    fun `showCurrentBook results in current book history being shown`() {
      val item1 = createSimpleHistoryItem(zimId = "id")
      val item2 = createSimpleHistoryItem(zimId = "notCurrentId")
      val date = DateItem(item1.dateString)
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item1, item2)
      )
      viewModel.actions.offer(UserClickedShowAllToggle(false))
      assertEquals(listOf(date, item1), viewModel.state.value?.getHistoryListItems())
    }

    @Test
    fun `filter ignores case`() {
      val item1 = createSimpleHistoryItem(historyTitle = "TITLE_IN_CAPS")
      val date = DateItem(item1.dateString)
      emissionOf(
        searchTerm = "title_in_caps",
        databaseResults = listOf(item1)
      )
      assertEquals(listOf(date, item1), viewModel.state.value?.getHistoryListItems())
    }
  }

  @Nested
  inner class ActionMapping {
    @Test
    fun `ExitedSearch offers Finish`() {
      actionResultsInEffects(ExitHistory, Finish)
    }

    @Test
    fun `OnItemLongClick selects history item from state`() {
      val item1 =
        createSimpleHistoryItem(
          "a", "1 Aug 2020", isSelected = false
        )
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item1)
      )
      viewModel.actions.offer(OnItemLongClick(item1))
      assertItemIsSelected(item1)
    }

    private fun assertItemIsSelected(item: HistoryItem) {
      assertTrue(
        (viewModel.state.value?.historyItems?.find { it.id == item.id } as HistoryItem).isSelected
      )
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
        UserClickedShowAllToggle(true),
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
        UserClickedConfirmDelete,
        DeleteHistoryItems(viewModel.state.value!!, historyDao)
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
