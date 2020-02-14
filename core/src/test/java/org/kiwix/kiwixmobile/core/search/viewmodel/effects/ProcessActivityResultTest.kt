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

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.appcompat.app.AppCompatActivity
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.processors.PublishProcessor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.search.viewmodel.Action
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.Filter

internal class ProcessActivityResultTest {

  private val activity: AppCompatActivity = mockk()
  private val data = mockk<Intent>()
  private val actions = mockk<PublishProcessor<Action>>(relaxed = true)
  private val successfulResult = ProcessActivityResult(
    StartSpeechInput.REQ_CODE_SPEECH_INPUT,
    Activity.RESULT_OK,
    data,
    actions
  )

  @BeforeEach
  fun init() {
    clearAllMocks()
  }

  @Test
  fun `invoke with does nothing with invalid requestCode`() {
    successfulResult.copy(requestCode = 0).invokeWith(activity)
    verify { actions wasNot Called }
  }

  @Test
  fun `invoke with does nothing with invalid resultCode`() {
    successfulResult.copy(resultCode = 0).invokeWith(activity)
    verify { actions wasNot Called }
  }

  @Test
  fun `invoke with does nothing with invalid data`() {
    successfulResult.copy(data = null).invokeWith(activity)
    verify { actions wasNot Called }
  }

  @Test
  fun `invoke with sends filter action with data`() {
    every { data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)[0] } returns ""
    successfulResult.invokeWith(activity)
    verify { actions.offer(Filter("")) }
  }
}
