/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.main.reader.helper.intent

import android.content.Intent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.PendingIntentParser.ReaderIntentAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderIntentManager @Inject constructor(private val pendingIntentParser: PendingIntentParser) {
  private var pendingAction: ReaderIntentAction = ReaderIntentAction.None
  private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
  val events = _events.asSharedFlow()

  fun storePendingIntent(intent: Intent?) {
    pendingAction = intent?.let(pendingIntentParser::parse) ?: ReaderIntentAction.None
    _events.tryEmit(Unit)
  }

  fun openZimFileFromPath(path: String, pageUrl: String) {
    pendingAction = ReaderIntentAction.OpenZim(path, pageUrl)
    _events.tryEmit(Unit)
  }

  fun consumePendingAction(): ReaderIntentAction =
    pendingAction.also { pendingAction = ReaderIntentAction.None }
}
