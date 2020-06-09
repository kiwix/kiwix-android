package org.kiwix.kiwixmobile.core.bookmark.viewmodel.effects

import androidx.appcompat.app.AppCompatActivity
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

data class UpdateAllBookmarksPreference(
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val isChecked: Boolean
) : SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    sharedPreferenceUtil.showBookmarksAllBooks = isChecked
  }
}
