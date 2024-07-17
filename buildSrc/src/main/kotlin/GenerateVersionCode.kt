import java.time.LocalDate
import java.time.temporal.ChronoUnit

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

object GenerateVersionCode {
  fun getVersionCode(): Int {
    // the date when the automatic version code generation started
    val lastDate = LocalDate.of(2020, 7, 17)

    // Calculate the number of days between the lastDate and today's date.
    // This gives us the total number of days since the last version code was set.
    val daysDifference = ChronoUnit.DAYS.between(lastDate, LocalDate.now()).toInt()

    // Base version code. This is the version code of the last release uploaded to the Play Store.
    // We use this as the starting point for generating new version codes automatically.
    val baseVersionCode = 231101

    // Generate and return the new version code.
    // The new version code is calculated by adding the number of days since lastDate
    // to the base version code. This creates a unique version code for each day.
    return baseVersionCode + daysDifference
  }
}
