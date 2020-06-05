package org.kiwix.kiwixmobile.core.bookmark.viewmodel.effects

import androidx.appcompat.app.AppCompatActivity
import io.reactivex.processors.BehaviorProcessor
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

data class ToggleShowAllBookmarksSwitchAndSaveItsStateToPrefs(
  private val showAllSwitchToggle: BehaviorProcessor<Boolean>,
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val isChecked: Boolean
) : SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    sharedPreferenceUtil.setShowBookmarksAllBooks(isChecked)
    showAllSwitchToggle.offer(sharedPreferenceUtil.showBookmarksAllBooks)
  }
}
