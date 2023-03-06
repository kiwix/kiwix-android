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

/*
 * Provides [PackageInfoFlagsCompat] to Android versions before SDK 33,
 * keeping a near consistent API for [Compat.getPackageInfo],
 * constraining the flags to the correct values (or types when available)
 *
 * Allows for either long flags to be provided (new API), or int flags (old API).
 * The old API is currently better as it means .toLong() isn't needed on constants provided
 * to the API.
 * For future: Can Kotlin can accept either the int or long in a `@LongDef`
 */

/**
 * Flags class that wraps around the bitmask flags used in methods that retrieve package or
 * application info.
 */
open class Flags protected constructor(val value: Long)
