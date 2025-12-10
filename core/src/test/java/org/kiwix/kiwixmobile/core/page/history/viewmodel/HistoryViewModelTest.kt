package org.kiwix.kiwixmobile.core.page.history.viewmodel

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.dao.HistoryRoomDao
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.history.viewmodel.effects.ShowDeleteHistoryDialog
import org.kiwix.kiwixmobile.core.page.history.viewmodel.effects.UpdateAllHistoryPreference
import org.kiwix.kiwixmobile.core.page.historyItem
import org.kiwix.kiwixmobile.core.page.historyState
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UpdatePages
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UserClickedShowAllToggle
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.files.testFlow
import org.kiwix.sharedFunctions.InstantExecutorExtension

@ExtendWith(InstantExecutorExtension::class)
internal class HistoryViewModelTest {
  private val historyRoomDao: HistoryRoomDao = mockk()
  private val zimReaderContainer: ZimReaderContainer = mockk()
  private val kiwixDataStore: KiwixDataStore = mockk()
  private val dialogShower = mockk<AlertDialogShower>(relaxed = true)
  private val viewModelScope = CoroutineScope(Dispatchers.IO)

  private lateinit var viewModel: HistoryViewModel
  private val zimReaderSource: ZimReaderSource = mockk()

  private val itemsFromDb: MutableSharedFlow<List<Page>> =
    MutableSharedFlow<List<Page>>(0)

  @BeforeEach
  fun init() {
    clearAllMocks()
    every { zimReaderContainer.id } returns "id"
    every { zimReaderContainer.name } returns "zimName"
    coEvery { kiwixDataStore.showHistoryOfAllBooks } returns flowOf(true)
    every { historyRoomDao.history() } returns itemsFromDb
    every { historyRoomDao.pages() } returns historyRoomDao.history()
    viewModel = HistoryViewModel(historyRoomDao, zimReaderContainer, kiwixDataStore).apply {
      alertDialogShower = dialogShower
      lifeCycleScope = viewModelScope
    }
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
    assertThat(
      viewModel.updatePages(
        historyState(),
        UpdatePages(listOf(historyItem(zimReaderSource = zimReaderSource)))
      )
    ).isEqualTo(
      historyState(listOf(historyItem(zimReaderSource = zimReaderSource)))
    )
  }

  @Test
  fun `offerUpdateToShowAllToggle offers UpdateAllHistoryPreference`() = runTest {
    testFlow(
      flow = viewModel.effects,
      triggerAction = {
        viewModel.offerUpdateToShowAllToggle(
          UserClickedShowAllToggle(false),
          historyState()
        )
      },
      assert = {
        assertThat(awaitItem()).isEqualTo(
          UpdateAllHistoryPreference(kiwixDataStore, false, viewModelScope)
        )
      }
    )
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
    assertThat(
      viewModel.deselectAllPages(
        historyState(
          listOf(
            historyItem(
              isSelected = true,
              zimReaderSource = zimReaderSource
            )
          )
        )
      )
    )
      .isEqualTo(
        historyState(
          listOf(
            historyItem(
              isSelected = false,
              zimReaderSource = zimReaderSource
            )
          )
        )
      )
  }

  @Test
  fun `createDeletePageDialogEffect returns ShowDeleteHistoryDialog`() =
    runTest {
      assertThat(viewModel.createDeletePageDialogEffect(historyState(), viewModelScope)).isEqualTo(
        ShowDeleteHistoryDialog(
          viewModel.effects,
          historyState(),
          historyRoomDao,
          viewModelScope,
          dialogShower
        )
      )
    }

  @Test
  fun `copyWithNewItems returns state with new items`() {
    assertThat(
      viewModel.copyWithNewItems(
        historyState(),
        listOf(historyItem(isSelected = true, zimReaderSource = zimReaderSource))
      )
    )
      .isEqualTo(
        historyState(
          listOf(
            historyItem(
              isSelected = true,
              zimReaderSource = zimReaderSource
            )
          )
        )
      )
  }
}
