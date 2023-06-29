package org.kiwix.kiwixmobile.core.page.history.viewmodel

import androidx.lifecycle.viewModelScope
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.dao.HistoryDao
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.history.viewmodel.effects.ShowDeleteHistoryDialog
import org.kiwix.kiwixmobile.core.page.history.viewmodel.effects.UpdateAllHistoryPreference
import org.kiwix.kiwixmobile.core.page.historyItem
import org.kiwix.kiwixmobile.core.page.historyState
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UpdatePages
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UserClickedShowAllToggle
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.sharedFunctions.InstantExecutorExtension
import org.kiwix.sharedFunctions.setScheduler

@ExtendWith(InstantExecutorExtension::class)
internal class HistoryViewModelTest {
  private val historyDao: HistoryDao = mockk()
  private val zimReaderContainer: ZimReaderContainer = mockk()
  private val sharedPreferenceUtil: SharedPreferenceUtil = mockk()

  private lateinit var viewModel: HistoryViewModel
  private val testScheduler = TestScheduler()

  init {
    setScheduler(testScheduler)
    RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
  }

  private val itemsFromDb: PublishProcessor<List<Page>> =
    PublishProcessor.create()

  @BeforeEach
  fun init() {
    clearAllMocks()
    every { zimReaderContainer.id } returns "id"
    every { zimReaderContainer.name } returns "zimName"
    every { sharedPreferenceUtil.showHistoryAllBooks } returns true
    every { historyDao.history() } returns itemsFromDb
    every { historyDao.pages() } returns historyDao.history()
    viewModel = HistoryViewModel(historyDao, zimReaderContainer, sharedPreferenceUtil)
  }

  @Test
  fun `Initial state returns initial state`() {
    assertThat(viewModel.initialState()).isEqualTo(historyState())
  }

  @Test
  fun `updatePagesBasedOnFilter returns state with searchTerm`() {
    assertThat(viewModel.updatePagesBasedOnFilter(historyState(), Filter("searchTerm")))
      .isEqualTo(
        historyState(searchTerm = "searchTerm")
      )
  }

  @Test
  fun `updatePages return state with history items`() {
    assertThat(viewModel.updatePages(historyState(), UpdatePages(listOf(historyItem())))).isEqualTo(
      historyState(listOf(historyItem()))
    )
  }

  @Test
  fun `offerUpdateToShowAllToggle offers UpdateAllHistoryPreference`() {
    viewModel.effects.test().also {
      viewModel.offerUpdateToShowAllToggle(
        UserClickedShowAllToggle(false), historyState()
      )
    }.assertValues(UpdateAllHistoryPreference(sharedPreferenceUtil, false))
  }

  @Test
  fun `offerUpdateToShowAllToggle returns state with showAll set to input value`() {
    assertThat(
      viewModel.offerUpdateToShowAllToggle(
        UserClickedShowAllToggle(false),
        historyState()
      )
    ).isEqualTo(historyState(showAll = false))
  }

  @Test
  fun `deselectAllPages returns state with all pages deselected`() {
    assertThat(viewModel.deselectAllPages(historyState(listOf(historyItem(isSelected = true)))))
      .isEqualTo(historyState(listOf(historyItem(isSelected = false))))
  }

  @Test
  fun `createDeletePageDialogEffect returns ShowDeleteHistoryDialog`() {
    assertThat(viewModel.createDeletePageDialogEffect(historyState())).isEqualTo(
      ShowDeleteHistoryDialog(
        viewModel.effects,
        historyState(),
        historyDao,
        viewModel.viewModelScope
      )
    )
  }

  @Test
  fun `copyWithNewItems returns state with new items`() {
    assertThat(viewModel.copyWithNewItems(historyState(), listOf(historyItem(isSelected = true))))
      .isEqualTo(historyState(listOf(historyItem(isSelected = true))))
  }
}
