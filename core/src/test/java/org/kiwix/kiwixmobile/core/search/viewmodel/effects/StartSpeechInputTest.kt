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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.appcompat.app.AppCompatActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.search.viewmodel.Action
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.StartSpeechInputFailed
import java.util.Locale

internal class StartSpeechInputTest {

  private val actions = mockk<Channel<Action>>(relaxed = true)

  @Suppress("DEPRECATION")
  @Test
  fun `when invoke with throws exception offer StartSpeechInputFailed action`() {
    val activity = mockk<AppCompatActivity>(relaxed = true)
    every { activity.startActivityForResult(any(), any()) } throws ActivityNotFoundException()
    StartSpeechInput(actions).invokeWith(activity)
    verify { actions.trySend(StartSpeechInputFailed).isSuccess }
  }

  @Suppress("DEPRECATION")
  @Test
  fun `invoke with starts an activity for speech recognition`() {
    val activity = mockk<AppCompatActivity>()
    every { activity.getString(R.string.app_name) } returns "app"
    every { activity.getString(R.string.speech_prompt_text, "app") } returns "the app"
    every { activity.startActivityForResult(any(), any()) } returns Unit
    mockkConstructor(Intent::class)
    StartSpeechInput(actions).invokeWith(activity)
    verify {
      constructedWith<Intent>().putExtra(
        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
      )
      constructedWith<Intent>().putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
      constructedWith<Intent>().putExtra(RecognizerIntent.EXTRA_PROMPT, "the app")
      activity.startActivityForResult(any(), StartSpeechInput.REQ_CODE_SPEECH_INPUT)
    }
  }
}
