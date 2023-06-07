/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.compat

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.ConnectivityManager

/**
 * This interface defines a set of functions that are not available on all platforms.
 *
 *
 * A set of implementations for the supported platforms are available.
 *
 *
 * Each implementation ends with a `V<n>` suffix, identifying the minimum API version on which this implementation
 * can be used. For example, see [CompatV21].
 *
 *
 * Each implementation `CompatVn` should extend the implementation `CompatVm` for the greatest m<n such that `CompatVm`
 * exists. E.g. as of July 2021 `CompatV23` extends `CompatV21` because there is no `CompatV22`.
 * If `CompatV22` were to be created one day, it will extends `CompatV22` and be extended by `CompatV23`.
 *
 *
 * Each method `method` must be implemented in the lowest Compat implementation (right now `CompatV21`, but
 * it will change when min sdk change). It must also be implemented in `CompatVn` if, in version `n` and higher,
 * a different implementation must be used. This can be done either because some method used in the API `n` got
 * deprecated, changed its behavior, or because the implementation of `method` can be more efficient.
 *
 *
 * When you call method `method` from some device with API `n`, it will uses the implementation in `CompatVm`,
 * for `m < n` as great as possible. The value of `m` being at least the current min SDK. The method may be empty,
 * for example `setupNotificationChannel`'s implementation in `CompatV21` is empty since
 * notification channels were introduced in API 26.
 *
 *
 * Example: `CompatV26` extends `CompatV23` which extends `CompatV21`. The method `vibrate` is
 * defined in `CompatV21` where only the number of seconds of vibration is taken into consideration, and is
 * redefined in `CompatV26` - using `@Override` - where the style of vibration is also taken into
 * consideration. It means that  on devices using APIs 21 to 25 included, the implementation of `CompatV21` is
 * used, and on devices using API 26 and higher, the implementation of `CompatV26` is used.
 * On the other hand a method like `setTime` that got defined in `CompatV21` and redefined in
 * `CompatV23` due to a change of API, need not be implemented again in CompatV26.
 */
interface Compat {
  fun queryIntentActivities(
    packageManager: PackageManager,
    intent: Intent,
    flags: ResolveInfoFlagsCompat
  ): List<ResolveInfo>

  fun getPackageInformation(
    packageName: String,
    packageManager: PackageManager,
    flag: Int
  ): PackageInfo

  fun isNetworkAvailable(connectivity: ConnectivityManager): Boolean

  fun isWifi(connectivity: ConnectivityManager): Boolean
}
