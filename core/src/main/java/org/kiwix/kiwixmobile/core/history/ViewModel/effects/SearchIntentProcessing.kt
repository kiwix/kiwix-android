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


import android.annotation.TargetApi
import android.content.Intent
import android.os.Build.VERSION_CODES
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.processors.PublishProcessor
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.history.ViewModel.Action
import org.kiwix.kiwixmobile.core.history.ViewModel.Action.Filter
import org.kiwix.kiwixmobile.core.history.ViewModel.Action.ReceivedPromptForSpeechInput
import org.kiwix.kiwixmobile.core.utils.EXTRA_SEARCH
import org.kiwix.kiwixmobile.core.utils.EXTRA_IS_WIDGET_VOICE

data class SearchIntentProcessing(
  private val intent: Intent?,
  private val actions: PublishProcessor<Action>
) : SideEffect<Unit> {
  @TargetApi(VERSION_CODES.M)
  override fun invokeWith(activity: AppCompatActivity) {
    intent?.let {
      if (it.hasExtra(Intent.EXTRA_PROCESS_TEXT)) {
        actions.offer(Filter(it.getStringExtra(Intent.EXTRA_PROCESS_TEXT)))
      }
      if (intent.hasExtra(EXTRA_SEARCH)) {
        actions.offer(Filter(intent.getStringExtra(EXTRA_SEARCH)))
      }
      if (intent.getBooleanExtra(EXTRA_IS_WIDGET_VOICE, false)) {
        actions.offer(ReceivedPromptForSpeechInput)
      }
    }
  }
}
