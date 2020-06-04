package org.kiwix.kiwixmobile.core.history.viewmodel.effects

import androidx.appcompat.app.AppCompatActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.processors.BehaviorProcessor
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

internal class ToggleShowAllHistorySwitchAndSaveItsStateToPrefsTest {
  @Test
  fun `toggle switch should be toggled`() {
    val toggleSwitch: BehaviorProcessor<Boolean> = mockk()
    every { toggleSwitch.offer(true) } returns true
    val sharedPreferenceUtil: SharedPreferenceUtil = mockk()
    val activity: AppCompatActivity = mockk()
    ToggleShowAllHistorySwitchAndSaveItsStateToPrefs(
      toggleSwitch,
      sharedPreferenceUtil,
      true
    ).invokeWith(activity)
    verify {
      toggleSwitch.offer(true)
      sharedPreferenceUtil.showHistoryCurrentBook = false
    }
  }
}
