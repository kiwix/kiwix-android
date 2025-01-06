/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The date when the automatic version code generation started.
 */
const val LAST_DATE = "2024-07-17"

/**
 * Base version code. This is the version code of the last release uploaded to the Play Store.
 * We use this as the starting point for generating new version codes automatically.
 */
const val BASE_VERSION_CODE = 231101

fun String.getVersionCode(): Int {
  // Get the current date. If the "KIWIX_ANDROID_RELEASE_DATE" environment
  // variable is set(in YYYY-MM-DD format).
  // It uses the specified date to generate the APK version code.
  // Otherwise, it generates the version code based on the current date.
  // See https://github.com/kiwix/kiwix-android/issues/4120 for more details.
  val currentDate = if (!System.getenv("KIWIX_ANDROID_RELEASE_DATE").isNullOrEmpty()) {
    LocalDate.parse(System.getenv("KIWIX_ANDROID_RELEASE_DATE")).also {
      println("Environment variable found. Using date: $it for version code generation.")
    }
  } else {
    LocalDate.now().also {
      println("No environment variable found. Using current date: $it for version code generation.")
    }
  }
  // Calculate the number of days between the LAST_DATE and today's date.
  // This gives us the total number of days since the last version code was set.
  val daysDifference = ChronoUnit.DAYS.between(LocalDate.parse(LAST_DATE), currentDate).toInt()

  // Generate and return the new version code.
  // The new version code is calculated by adding the number of days since LAST_DATE
  // to the base version code. This creates a unique version code for each day.
  return BASE_VERSION_CODE + daysDifference
}
