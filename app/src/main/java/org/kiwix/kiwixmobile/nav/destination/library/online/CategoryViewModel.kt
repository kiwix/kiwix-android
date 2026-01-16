/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.nav.destination.library.online

import android.app.Application
import androidx.lifecycle.ViewModel
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import javax.inject.Inject

@Suppress("UnusedPrivateProperty")
class CategoryViewModel @Inject constructor(
  private val context: Application,
  private val kiwixDataStore: KiwixDataStore,
  private var kiwixService: KiwixService,
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver
) : ViewModel() {
  // Prepare the category based data here.
}
