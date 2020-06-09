package org.kiwix.kiwixmobile.core.history.viewmodel.effects

import androidx.appcompat.app.AppCompatActivity
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

internal class ToggleShowAllHistorySwitchAndSaveItsStateToPrefsTest {
  @Test
  fun `toggle switch should be toggled`() {
    val sharedPreferenceUtil: SharedPreferenceUtil = mockk()
    val activity: AppCompatActivity = mockk()
    UpdateAllHistoryPreference(
      sharedPreferenceUtil,
      true
    ).invokeWith(activity)
    verify {
      sharedPreferenceUtil.showHistoryAllBooks = true
    }
  }
}
