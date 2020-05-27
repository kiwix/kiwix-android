package org.kiwix.kiwixmobile.core.history.viewmodel.effects

import android.app.Activity
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.processors.BehaviorProcessor
import org.kiwix.kiwixmobile.core.Intents.internal
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.EXTRA_CHOSE_X_FILE
import org.kiwix.kiwixmobile.core.utils.EXTRA_CHOSE_X_URL
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

data class ToggleShowAllHistorySwitchAndSaveItsStateToPrefs(
  private val showAllSwitchToggle: BehaviorProcessor<Boolean>,
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val isChecked: Boolean
) : SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    showAllSwitchToggle.offer(isChecked)
    sharedPreferenceUtil.setShowHistoryCurrentBook(!isChecked)
  }
}
