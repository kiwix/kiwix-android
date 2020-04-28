package org.kiwix.kiwixmobile.core.history.viewmodel.effects

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import org.kiwix.kiwixmobile.core.Intents
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.EXTRA_CHOSE_X_FILE
import org.kiwix.kiwixmobile.core.utils.EXTRA_CHOSE_X_URL

data class ShowAllSwitchToggled(
  private val historyItem: HistoryItem,
  private val zimReaderContainer: ZimReaderContainer
) :
  SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    val intent = Intents.internal(CoreMainActivity::class.java)
    intent.putExtra(EXTRA_CHOSE_X_URL, historyItem.historyUrl)
    if (historyItem.zimFilePath != zimReaderContainer.zimCanonicalPath) {
      intent.putExtra(EXTRA_CHOSE_X_FILE, historyItem.zimFilePath)
    }
    activity.setResult(Activity.RESULT_OK, intent)
    activity.finish()
  }
}
