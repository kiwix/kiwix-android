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
import kotlinx.coroutines.channels.Channel
import org.kiwix.kiwixmobile.core.R
import com.tonyodev.fetch2.R.string
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.search.viewmodel.Action
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.StartSpeechInputFailed
import java.util.Locale

data class StartSpeechInput(private val actions: Channel<Action>) : SideEffect<Unit> {

  @Suppress("DEPRECATION")
  override fun invokeWith(activity: AppCompatActivity) {
    try {
      activity.startActivityForResult(
        Intent().apply {
          action = RecognizerIntent.ACTION_RECOGNIZE_SPEECH
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
          putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            activity.getString(R.string.speech_prompt_text, activity.getString(string.app_name))
          )
        },
        REQ_CODE_SPEECH_INPUT
      )
    } catch (a: ActivityNotFoundException) {
      actions.trySend(StartSpeechInputFailed).isSuccess
    }
  }

  companion object {
    const val REQ_CODE_SPEECH_INPUT = 100
  }
}
