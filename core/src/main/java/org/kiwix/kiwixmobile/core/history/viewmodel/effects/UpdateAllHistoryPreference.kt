package org.kiwix.kiwixmobile.core.history.viewmodel.effects

import androidx.appcompat.app.AppCompatActivity
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

data class UpdateAllHistoryPreference(
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val isChecked: Boolean
) : SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    sharedPreferenceUtil.showHistoryAllBooks = isChecked
  }
}
