package org.kiwix.kiwixmobile.core.page.history.viewmodel.effects

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.reactivex.processors.PublishProcessor
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.HistoryDao
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.page.historyItem
import org.kiwix.kiwixmobile.core.page.historyState
import org.kiwix.kiwixmobile.core.page.viewmodel.effects.DeletePageItems
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.DeleteAllHistory
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.DeleteSelectedHistory

internal class ShowDeleteHistoryDialogTest {
  val effects = mockk<PublishProcessor<SideEffect<*>>>(relaxed = true)
  private val historyDao = mockk<HistoryDao>()
  val activity = mockk<CoreMainActivity>()
  private val dialogShower = mockk<DialogShower>(relaxed = true)
  private val coroutineScope: CoroutineScope = mockk(relaxed = true)

  @Test
  fun `invoke with shows dialog that offers ConfirmDelete action`() {
    val showDeleteHistoryDialog =
      ShowDeleteHistoryDialog(
        effects,
        historyState(),
        historyDao,
        coroutineScope
      )
    mockkActivityInjection(showDeleteHistoryDialog)
    val lambdaSlot = slot<() -> Unit>()
    showDeleteHistoryDialog.invokeWith(activity)
    verify { dialogShower.show(any(), capture(lambdaSlot)) }
    lambdaSlot.captured.invoke()
    verify { effects.offer(DeletePageItems(historyState(), historyDao)) }
  }

  @Test
  fun `invoke with selected item shows dialog with delete selected items title`() {
    val showDeleteHistoryDialog =
      ShowDeleteHistoryDialog(
        effects,
        historyState(listOf(historyItem(isSelected = true))),
        historyDao,
        coroutineScope
      )
    mockkActivityInjection(showDeleteHistoryDialog)
    showDeleteHistoryDialog.invokeWith(activity)
    verify { dialogShower.show(DeleteSelectedHistory, any()) }
  }

  @Test
  fun `invoke with no selected items shows dialog with delete all items title`() {
    val showDeleteHistoryDialog =
      ShowDeleteHistoryDialog(
        effects,
        historyState(),
        historyDao,
        coroutineScope
      )
    mockkActivityInjection(showDeleteHistoryDialog)
    showDeleteHistoryDialog.invokeWith(activity)
    verify { dialogShower.show(DeleteAllHistory, any()) }
  }

  private fun mockkActivityInjection(showDeleteHistoryDialog: ShowDeleteHistoryDialog) {
    every { activity.cachedComponent.inject(showDeleteHistoryDialog) } answers {
      showDeleteHistoryDialog.dialogShower = dialogShower
      Unit
    }
  }
}
