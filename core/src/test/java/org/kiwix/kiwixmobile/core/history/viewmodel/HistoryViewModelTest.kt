package org.kiwix.kiwixmobile.core.history.viewmodel

import com.jraska.livedata.test
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.assertj.core.api.Assertions.assertThat
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
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.UpdateHistory
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
    every { historyDao.history() } returns itemsFromDb
    viewModel = HistoryViewModel(historyDao, zimReaderContainer, sharedPreferenceUtil)
  }

  private fun resultsIn(st: HistoryState) {
    viewModel.state.test().assertValue(st)
  }

  @Nested
  inner class StateTests {

    @Test
    fun `initial state is Initialising`() {
      viewModel.state.test().assertValue(state())
    }

    @Test
    internal fun `ExitHistory finishes activity`() {
      viewModel.effects.test().also { viewModel.actions.offer(ExitHistory) }.assertValue(Finish)
      viewModel.state.test().assertValue(state())
    }

    @Test
    internal fun `ExitActionModeMenu deselects history items`() {
      viewModel.state.postValue(state(historyItems = listOf(historyItem(isSelected = true))))
      viewModel.actions.offer(ExitActionModeMenu)
      viewModel.state.test()
        .assertValue(state(historyItems = listOf(historyItem(isSelected = false))))
    }

    @Test
    internal fun `UserClickedConfirmDelete offers DeleteHistoryItems`() {
      viewModel.effects.test().also { viewModel.actions.offer(UserClickedConfirmDelete) }
        .assertValue(DeleteHistoryItems(state(), historyDao))
      viewModel.state.test().assertValue(state())
    }

    @Test
    internal fun `UserClickedDeleteButton offers ShowDeleteHistoryDialog`() {
      viewModel.effects.test().also { viewModel.actions.offer(UserClickedDeleteButton) }
        .assertValue(ShowDeleteHistoryDialog(viewModel.actions))
      viewModel.state.test().assertValue(state())
    }

    @Test
    internal fun `UserClickedDeleteSelectedHistoryItems offers ShowDeleteHistoryDialog`() {
      viewModel.effects.test()
        .also { viewModel.actions.offer(UserClickedDeleteSelectedHistoryItems) }
        .assertValue(ShowDeleteHistoryDialog(viewModel.actions))
      viewModel.state.test().assertValue(state())
    }

    @Test
    internal fun `UserClickedShowAllToggle offers UpdateAllHistoryPreference`() {
      viewModel.effects.test()
        .also { viewModel.actions.offer(UserClickedShowAllToggle(false)) }
        .assertValue(UpdateAllHistoryPreference(sharedPreferenceUtil, false))
      viewModel.state.test().assertValue(state(showAll = false))
    }

    @Test
    internal fun `OnItemClick selects item if one is selected`() {
      val historyItem = historyItem(isSelected = true)
      viewModel.state.postValue(state(listOf(historyItem)))
      viewModel.actions.offer(OnItemClick(historyItem))
      viewModel.state.test().assertValue(state(listOf(historyItem())))
    }

    @Test
    internal fun `OnItemClick offers OpenHistoryItem if none is selected`() {
      viewModel.state.postValue(state(listOf(historyItem())))
      viewModel.effects.test()
        .also { viewModel.actions.offer(OnItemClick(historyItem())) }
        .assertValue(OpenHistoryItem(historyItem(), zimReaderContainer))
      viewModel.state.test().assertValue(state(listOf(historyItem())))
    }

    @Test
    internal fun `OnItemLongClick selects item if none is selected`() {
      val historyItem = historyItem()
      viewModel.state.postValue(state(listOf(historyItem)))
      viewModel.actions.offer(OnItemLongClick(historyItem))
      viewModel.state.test().assertValue(state(listOf(historyItem(isSelected = true))))
    }

    @Test
    fun `Filter updates search term`() {
      val searchTerm = "searchTerm"
      viewModel.actions.offer(Filter(searchTerm))
      viewModel.state.test().assertValue(state(searchTerm = searchTerm))
    }

    @Test
    internal fun `UpdateHistory updates history`() {
      viewModel.actions.offer(UpdateHistory(listOf(historyItem())))
      viewModel.state.test().assertValue(state(listOf(historyItem())))
    }

    @Test
    fun `empty search string with database results shows Results`() {
      val item =
        historyItem("")
      val date = DateItem(item.dateString)
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item)
      )
      resultsIn(HistoryState(listOf(item), true, "id", ""))
    }

    @Test
    fun `empty search string with no database results is NoResults`() {
      emissionOf(
        searchTerm = "",
        databaseResults = emptyList()
      )
      resultsIn(HistoryState(emptyList(), true, "id", ""))
    }

    @Test
    fun `duplicate search terms are ignored`() {
      val searchString1 = "a"
      val searchString2 = "b"
      val item =
        historyItem("b")
      val date = DateItem(item.dateString)
      emissionOf(
        searchTerm = searchString1,
        databaseResults = listOf(item)
      )
      viewModel.actions.offer(Filter(searchString2))
      resultsIn(HistoryState(listOf(item), true, "id", "b"))
    }

    @Test
    fun `only latest search term is used`() {
      val item =
        historyItem("b")
      val date = DateItem(item.dateString)
      emissionOf(
        searchTerm = "a",
        databaseResults = emptyList()
      )
      emissionOf(
        searchTerm = "b",
        databaseResults = listOf(item)
      )
      resultsIn(HistoryState(listOf(item), true, "id", "b"))
    }

    @Test
    fun `enters selection state if item is selected`() {
      val item =
        historyItem(
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
      resultsIn(HistoryState(listOf(item), true, "id", "b"))
    }

    @Test
    fun `OnItemLongClick enters selection state`() {
      val item1 =
        historyItem(
          "a", "1 Aug 2020"
        )
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item1)
      )
      viewModel.actions.offer(OnItemLongClick(item1))
      item1.isSelected = true
      resultsIn(HistoryState(listOf(item1), true, "id", ""))
    }

    @Test
    fun `Deselection via OnItemClick exits selection state if last item is deselected`() {
      val item1 =
        historyItem(
          "a", "1 Aug 2020"
        )
      val item2 =
        historyItem(
          "a", "1 Aug 2020"
        )
      val date = DateItem(item1.dateString)
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item1, item2)
      )
      viewModel.actions.offer(OnItemLongClick(item1))
      viewModel.actions.offer(OnItemClick(item1))
      resultsIn(HistoryState(listOf(item1, item2), true, "id", ""))
    }

    @Test
    fun `ExitActionMode deselects all items`() {
      val item1 =
        historyItem(
          "a", "1 Aug 2020", isSelected = true
        )
      val item2 =
        historyItem(
          "a", "1 Aug 2020", isSelected = true
        )
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item1, item2)
      )
      viewModel.actions.offer(ExitActionModeMenu)
      item1.isSelected = false
      item2.isSelected = false
      resultsIn(HistoryState(listOf(item1, item2), true, "id", ""))
    }
  }

  private fun emissionOf(searchTerm: String, databaseResults: List<HistoryListItem.HistoryItem>) {
  }

  @Nested
  inner class HistoryListItemsTests {

    @Test
    fun `order of date headers and items are correct`() {
      val item1 =
        historyItem(
          "a", "1 Aug 2020"
        )
      val date1 = DateItem(item1.dateString)
      val item2 =
        historyItem(
          "b", "2 Jun 1990"
        )
      val date2 = DateItem(item2.dateString)
      val item3 =
        historyItem(
          "c", "1 Aug 2021"
        )
      val date3 = DateItem(item3.dateString)
      val state = HistoryState(listOf(item3, item1, item2), true, "id", "")
      assertThat(
        state.historyListItems
      ).isEqualTo(
        listOf(date3, item3, date1, item1, date2, item2)
      )
    }

    @Test
    fun `non empty search string with no search results is empty history item list`() {
      val state = HistoryState(
        listOf(historyItem("")),
        true,
        "id",
        "a"
      )
      assertThat(emptyList<HistoryListItem>()).isEqualTo(state.historyListItems)
    }

    @Test
    fun `showAllHistory results in all history being shown`() {
      val item1 = historyItem(zimId = "notCurrentId1")
      val item2 = historyItem(zimId = "notCurrentId")
      val date = DateItem(item1.dateString)
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item1, item2)
      )
      viewModel.actions.offer(UserClickedShowAllToggle(true))
      assertThat(listOf(date, item1, item2)).isEqualTo(viewModel.state.value?.historyListItems)
    }

    @Test
    fun `showCurrentBook results in current book history being shown`() {
      val item1 = historyItem(zimId = "id")
      val item2 = historyItem(zimId = "notCurrentId")
      val date = DateItem(item1.dateString)
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item1, item2)
      )
      viewModel.actions.offer(UserClickedShowAllToggle(false))
      assertThat(listOf(date, item1)).isEqualTo(viewModel.state.value?.historyListItems)
    }

    @Test
    fun `filter ignores case`() {
      val item1 = historyItem(historyTitle = "TITLE_IN_CAPS")
      val date = DateItem(item1.dateString)
      emissionOf(
        searchTerm = "title_in_caps",
        databaseResults = listOf(item1)
      )
      assertThat(listOf(date, item1)).isEqualTo(viewModel.state.value?.historyListItems)
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
        historyItem(
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
      assertThat(
        (viewModel.state.value?.historyItems?.find { it.id == item.id } as HistoryItem).isSelected
      )
    }

    @Test
    fun `OnItemClick selects history item from state if in SelectionMode`() {
      val item1 =
        historyItem(
          "a", "1 Aug 2020", id = 2
        )
      val item2 =
        historyItem(
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
        historyItem(
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
