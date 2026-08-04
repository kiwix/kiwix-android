package org.kiwix.kiwixmobile.core.page.history.viewmodel.effects

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.HistoryRoomDao
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.page.historyItem
import org.kiwix.kiwixmobile.core.page.historyState
import org.kiwix.kiwixmobile.core.page.viewmodel.effects.DeletePageItems
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.DeleteAllHistory
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.DeleteSelectedHistory
import org.kiwix.sharedFunctions.MainDispatcherRule

internal class ShowDeleteHistoryDialogTest {
  @RegisterExtension
  @JvmField
  val mainDispatcherRule = MainDispatcherRule()
  private val testDispatcher = mainDispatcherRule.dispatcher
  val effects = mockk<MutableSharedFlow<SideEffect<*>>>(relaxed = true)
  private val historyRoomDao = mockk<HistoryRoomDao>()
  val activity = mockk<CoreMainActivity>()
  private val dialogShower = mockk<DialogShower>(relaxed = true)

  @Test
  fun `invoke with shows dialog that offers ConfirmDelete action`() = runTest {
    val testScope = this
    val showDeleteHistoryDialog =
      ShowDeleteHistoryDialog(
        effects,
        historyState(),
        historyRoomDao,
        testScope,
        dialogShower,
        testDispatcher
      )
    val lambdaSlot = slot<() -> Unit>()
    showDeleteHistoryDialog.invokeWith(activity)
    verify { dialogShower.show(any(), capture(lambdaSlot)) }
    lambdaSlot.captured.invoke()
    verify {
      effects.tryEmit(
        DeletePageItems(
          historyState(),
          historyRoomDao,
          testScope,
          testDispatcher
        )
      )
    }
  }

  @Test
  fun `invoke with selected item shows dialog with delete selected items title`() = runTest {
    val showDeleteHistoryDialog =
      ShowDeleteHistoryDialog(
        effects,
        historyState(listOf(historyItem(isSelected = true, zimReaderSource = mockk()))),
        historyRoomDao,
        this,
        dialogShower,
        testDispatcher
      )
    showDeleteHistoryDialog.invokeWith(activity)
    verify { dialogShower.show(DeleteSelectedHistory, any()) }
  }

  @Test
  fun `invoke with no selected items shows dialog with delete all items title`() = runTest {
    val showDeleteHistoryDialog =
      ShowDeleteHistoryDialog(
        effects,
        historyState(),
        historyRoomDao,
        this,
        dialogShower,
        testDispatcher
      )
    showDeleteHistoryDialog.invokeWith(activity)
    verify { dialogShower.show(DeleteAllHistory, any()) }
  }
}
