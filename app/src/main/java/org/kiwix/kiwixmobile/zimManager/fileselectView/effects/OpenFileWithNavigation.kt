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

package org.kiwix.kiwixmobile.zimManager.fileselectView.effects

import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.setNavigationResultOnCurrent
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.ZIM_FILE_URI_KEY
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.ui.KiwixDestination

data class OpenFileWithNavigation(
  private val zimReaderSource: ZimReaderSource,
  private val coroutineScope: CoroutineScope,
  private val ioDispatcher: CoroutineDispatcher
) : SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    coroutineScope.launch {
      val canOpenInLibkiwix = withContext(ioDispatcher) {
        zimReaderSource.canOpenInLibkiwix()
      }
      if (!canOpenInLibkiwix) {
        activity.toast(
          activity.getString(R.string.error_file_not_found, zimReaderSource.toDatabase())
        )
      } else {
        val navOptions = NavOptions.Builder()
          .setPopUpTo(KiwixDestination.Reader.route, inclusive = true)
          .build()
        (activity as CoreMainActivity).apply {
          navigate(KiwixDestination.Reader.route, navOptions)
          setNavigationResultOnCurrent(zimReaderSource.toDatabase(), ZIM_FILE_URI_KEY)
        }
      }
    }
  }
}
