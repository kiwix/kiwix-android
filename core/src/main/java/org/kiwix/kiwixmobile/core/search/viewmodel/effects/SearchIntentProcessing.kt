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

import android.annotation.TargetApi
import android.content.Intent
import android.os.Build.VERSION_CODES
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.processors.PublishProcessor
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.search.viewmodel.Action
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ReceivedPromptForSpeechInput
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ScreenOrigin
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchOrigin
import org.kiwix.kiwixmobile.core.utils.Constants

data class SearchIntentProcessing(
  private val intent: Intent?,
  private val actions: PublishProcessor<Action>
) : SideEffect<Unit> {
  @TargetApi(VERSION_CODES.M)
  override fun invokeWith(activity: AppCompatActivity) {
    if (intent != null) {
      if (intent.getBooleanExtra(Constants.TAG_FROM_TAB_SWITCHER, false)) {
        actions.offer(ScreenOrigin(SearchOrigin.FromTabView))
      } else {
        actions.offer(ScreenOrigin(SearchOrigin.FromWebView))
      }
      if (intent.hasExtra(Intent.EXTRA_PROCESS_TEXT)) {
        actions.offer(Filter(intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)))
      }
      if (intent.hasExtra(Constants.EXTRA_SEARCH)) {
        actions.offer(Filter(intent.getStringExtra(Constants.EXTRA_SEARCH)))
      }
      if (intent.getBooleanExtra(Constants.EXTRA_IS_WIDGET_VOICE, false)) {
        actions.offer(ReceivedPromptForSpeechInput)
      }
    }
  }
}
