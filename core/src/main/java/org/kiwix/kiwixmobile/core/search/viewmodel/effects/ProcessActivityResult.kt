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
import kotlinx.coroutines.channels.Channel
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.search.viewmodel.Action
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.Filter

data class ProcessActivityResult(
  private val requestCode: Int,
  private val resultCode: Int,
  private val data: Intent?,
  private val actions: Channel<Action>
) : SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    if (requestCode == StartSpeechInput.REQ_CODE_SPEECH_INPUT &&
      resultCode == Activity.RESULT_OK &&
      data != null
    ) {
      data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.apply {
        first()?.let {
          actions.offer(Filter(it))
        }
      }
    }
  }
}
