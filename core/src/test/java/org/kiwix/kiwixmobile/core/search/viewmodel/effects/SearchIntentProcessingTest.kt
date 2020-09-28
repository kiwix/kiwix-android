/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile.core.search.viewmodel.effects

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.channels.Channel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.search.viewmodel.Action
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ReceivedPromptForSpeechInput
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ScreenWasStartedFrom
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchOrigin.FromTabView
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchOrigin.FromWebView
import org.kiwix.kiwixmobile.core.utils.EXTRA_IS_WIDGET_VOICE
import org.kiwix.kiwixmobile.core.utils.EXTRA_SEARCH
import org.kiwix.kiwixmobile.core.utils.TAG_FROM_TAB_SWITCHER

internal class SearchIntentProcessingTest {

  private val actions: Channel<Action> = mockk(relaxed = true)

  private val activity: AppCompatActivity = mockk()

  private val intent: Intent = mockk(relaxed = true)

  @BeforeEach
  fun init() {
    clearAllMocks()
  }

  @Test
  fun `invoke with does nothing with null data`() {
    SearchIntentProcessing(null, actions).invokeWith(activity)
    verify { actions wasNot Called }
  }

  @Test
  fun `invoke with offers action when EXTRA_PROCESS_TEXT present`() {
    val extra = ""
    every { intent.hasExtra(Intent.EXTRA_PROCESS_TEXT) } returns true
    every { intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT) } returns extra
    SearchIntentProcessing(intent, actions).invokeWith(activity)
    verifySequence {
      actions.offer(any<ScreenWasStartedFrom>())
      actions.offer(Filter(extra))
    }
  }

  @Test
  fun `invoke with offers action when EXTRA_SEARCH present`() {
    val extra = ""
    every { intent.hasExtra(EXTRA_SEARCH) } returns true
    every { intent.getStringExtra(EXTRA_SEARCH) } returns extra
    SearchIntentProcessing(intent, actions).invokeWith(activity)
    verifySequence {
      actions.offer(any<ScreenWasStartedFrom>())
      actions.offer(Filter(extra))
    }
  }

  @Test
  fun `invoke with offers action when EXTRA_IS_WIDGET_VOICE present`() {
    every { intent.getBooleanExtra(EXTRA_IS_WIDGET_VOICE, false) } returns true
    SearchIntentProcessing(intent, actions).invokeWith(activity)
    verifySequence {
      actions.offer(any<ScreenWasStartedFrom>())
      actions.offer(ReceivedPromptForSpeechInput)
    }
  }

  @Test
  fun `invoke with offers action when TAG_FROM_TAB_SWITCHER present`() {
    every { intent.getBooleanExtra(TAG_FROM_TAB_SWITCHER, false) } returns true
    SearchIntentProcessing(intent, actions).invokeWith(activity)
    verifySequence {
      actions.offer(ScreenWasStartedFrom(FromTabView))
    }
  }

  @Test
  fun `invoke with offers action when TAG_FROM_TAB_SWITCHER not present`() {
    every { intent.getBooleanExtra(TAG_FROM_TAB_SWITCHER, false) } returns false
    SearchIntentProcessing(intent, actions).invokeWith(activity)
    verifySequence {
      actions.offer(ScreenWasStartedFrom(FromWebView))
    }
  }
}
