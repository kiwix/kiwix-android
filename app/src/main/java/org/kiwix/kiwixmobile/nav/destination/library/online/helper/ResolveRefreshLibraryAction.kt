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

package org.kiwix.kiwixmobile.nav.destination.library.online.helper

import android.net.ConnectivityManager
import kotlinx.coroutines.flow.first
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.isNetworkAvailable
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.isWifi
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveRefreshLibraryAction.Result.Proceed
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveRefreshLibraryAction.Result.NoInternetWithContent
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveRefreshLibraryAction.Result.NoInternetWithEmptyContent
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveRefreshLibraryAction.Result.WifiOnlyBlocked
import javax.inject.Inject

class ResolveRefreshLibraryAction @Inject constructor(
  private val kiwixDataStore: KiwixDataStore,
  private val connectivityManager: ConnectivityManager
) {
  sealed class Result {
    object Proceed : Result()
    object NoInternetWithContent : Result()
    object NoInternetWithEmptyContent : Result()
    object WifiOnlyBlocked : Result()
  }

  suspend operator fun invoke(hasItems: Boolean): Result {
    return if (!connectivityManager.isNetworkAvailable()) {
      if (hasItems) {
        NoInternetWithContent
      } else {
        NoInternetWithEmptyContent
      }
    } else if (kiwixDataStore.wifiOnly.first() && !connectivityManager.isWifi()) {
      WifiOnlyBlocked
    } else {
      Proceed
    }
  }
}
