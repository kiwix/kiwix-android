package org.kiwix.kiwixmobile.core.page.history.viewmodel

import com.jraska.livedata.test
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.dao.HistoryDao
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.page.history.viewmodel.Action.ExitActionModeMenu
import org.kiwix.kiwixmobile.core.page.history.viewmodel.Action.ExitHistory
import org.kiwix.kiwixmobile.core.page.history.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.page.history.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.page.history.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.page.history.viewmodel.Action.UpdateHistory
import org.kiwix.kiwixmobile.core.page.history.viewmodel.Action.UserClickedConfirmDelete
import org.kiwix.kiwixmobile.core.page.history.viewmodel.Action.UserClickedDeleteButton
import org.kiwix.kiwixmobile.core.page.history.viewmodel.Action.UserClickedDeleteSelectedHistoryItems
import org.kiwix.kiwixmobile.core.page.history.viewmodel.Action.UserClickedShowAllToggle
import org.kiwix.kiwixmobile.core.page.history.viewmodel.effects.DeleteHistoryItems
import org.kiwix.kiwixmobile.core.page.history.viewmodel.effects.ShowDeleteHistoryDialog
import org.kiwix.kiwixmobile.core.page.history.viewmodel.effects.UpdateAllHistoryPreference
import org.kiwix.kiwixmobile.core.page.history.viewmodel.effects.historyItem
import org.kiwix.kiwixmobile.core.page.history.viewmodel.effects.historyState
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

  @Test
  fun `initial state is Initialising`() {
    viewModel.state.test().assertValue(historyState())
  }

  @Test
  internal fun `ExitHistory finishes activity`() {
    viewModel.effects.test().also { viewModel.actions.offer(ExitHistory) }.assertValue(Finish)
    viewModel.state.test().assertValue(historyState())
  }

  @Test
  internal fun `ExitActionModeMenu deselects history items`() {
    viewModel.state.postValue(historyState(historyItems = listOf(historyItem(isSelected = true))))
    viewModel.actions.offer(ExitActionModeMenu)
    viewModel.state.test().assertValue(
      historyState(historyItems = listOf(historyItem(isSelected = false)))
    )
  }

  @Test
  internal fun `UserClickedConfirmDelete offers DeleteHistoryItems`() {
    viewModel.effects.test().also { viewModel.actions.offer(UserClickedConfirmDelete) }
      .assertValue(DeleteHistoryItems(historyState(), historyDao))
    viewModel.state.test().assertValue(historyState())
  }

  @Test
  internal fun `UserClickedDeleteButton offers ShowDeleteHistoryDialog`() {
    viewModel.effects.test().also { viewModel.actions.offer(UserClickedDeleteButton) }
      .assertValue(ShowDeleteHistoryDialog(viewModel.actions))
    viewModel.state.test().assertValue(historyState())
  }

  @Test
  internal fun `UserClickedDeleteSelectedHistoryItems offers ShowDeleteHistoryDialog`() {
    viewModel.effects.test().also { viewModel.actions.offer(UserClickedDeleteSelectedHistoryItems) }
      .assertValue(ShowDeleteHistoryDialog(viewModel.actions))
    viewModel.state.test().assertValue(historyState())
  }

  @Test
  internal fun `UserClickedShowAllToggle offers UpdateAllHistoryPreference`() {
    viewModel.effects.test()
      .also { viewModel.actions.offer(UserClickedShowAllToggle(false)) }
      .assertValue(UpdateAllHistoryPreference(sharedPreferenceUtil, false))
    viewModel.state.test().assertValue(historyState(showAll = false))
  }

  @Test
  internal fun `OnItemClick selects item if one is selected`() {
    val historyItem = historyItem(isSelected = true)
    viewModel.state.postValue(historyState(listOf(historyItem)))
    viewModel.actions.offer(OnItemClick(historyItem))
    viewModel.state.test().assertValue(historyState(listOf(historyItem())))
  }

  @Test
  internal fun `OnItemClick offers OpenHistoryItem if none is selected`() {
    viewModel.state.postValue(historyState(listOf(historyItem())))
    viewModel.effects.test().also { viewModel.actions.offer(OnItemClick(historyItem())) }
      .assertValue(OpenHistoryItem(historyItem(), zimReaderContainer))
    viewModel.state.test().assertValue(historyState(listOf(historyItem())))
  }

  @Test
  internal fun `OnItemLongClick selects item if none is selected`() {
    val historyItem = historyItem()
    viewModel.state.postValue(historyState(listOf(historyItem)))
    viewModel.actions.offer(OnItemLongClick(historyItem))
    viewModel.state.test().assertValue(historyState(listOf(historyItem(isSelected = true))))
  }

  @Test
  fun `Filter updates search term`() {
    val searchTerm = "searchTerm"
    viewModel.actions.offer(Filter(searchTerm))
    viewModel.state.test().assertValue(historyState(searchTerm = searchTerm))
  }

  @Test
  internal fun `UpdateHistory updates history`() {
    viewModel.actions.offer(UpdateHistory(listOf(historyItem())))
    viewModel.state.test().assertValue(historyState(listOf(historyItem())))
  }
}
