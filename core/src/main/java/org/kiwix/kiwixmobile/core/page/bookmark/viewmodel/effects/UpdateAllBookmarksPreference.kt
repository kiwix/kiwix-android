package org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.effects

import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore

data class UpdateAllBookmarksPreference(
  private val kiwixDataStore: KiwixDataStore,
  private val isChecked: Boolean,
  private val lifeCycleScope: CoroutineScope
) : SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    lifeCycleScope.launch {
      kiwixDataStore.setShowBookmarksOfAllBooks(isChecked)
    }
  }
}
