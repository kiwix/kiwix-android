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
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.channels.Channel
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.search.NAV_ARG_SEARCH_STRING
import org.kiwix.kiwixmobile.core.search.viewmodel.Action
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ReceivedPromptForSpeechInput
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ScreenWasStartedFrom
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchOrigin.FromTabView
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchOrigin.FromWebView
import org.kiwix.kiwixmobile.core.utils.EXTRA_IS_WIDGET_VOICE
import org.kiwix.kiwixmobile.core.utils.TAG_FROM_TAB_SWITCHER

data class SearchArgumentProcessing(
  private val bundle: Bundle?,
  private val actions: Channel<Action>
) : SideEffect<Unit> {
  @TargetApi(VERSION_CODES.M)
  override fun invokeWith(activity: AppCompatActivity) {
    bundle?.let {
      actions.offer(
        ScreenWasStartedFrom(
          if (it.getBoolean(TAG_FROM_TAB_SWITCHER, false)) FromTabView
          else FromWebView
        )
      )
      actions.offer(Filter(it.getString(NAV_ARG_SEARCH_STRING, "")))
      if (it.getBoolean(EXTRA_IS_WIDGET_VOICE, false)) {
        actions.offer(ReceivedPromptForSpeechInput)
      }
    }
  }
}
