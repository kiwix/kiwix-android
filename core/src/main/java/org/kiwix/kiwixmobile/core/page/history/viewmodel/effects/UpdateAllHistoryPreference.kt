package org.kiwix.kiwixmobile.core.page.history.viewmodel.effects

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore

data class UpdateAllHistoryPreference(
  private val kiwixDataStore: KiwixDataStore,
  private val isChecked: Boolean,
  private val lifeCycleScope: CoroutineScope
) : SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    lifeCycleScope.launch {
      kiwixDataStore.setShowHistoryOfAllBooks(isChecked)
    }
  }
}
