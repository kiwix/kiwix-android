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

package org.kiwix.kiwixmobile.core.extensions

import org.threeten.bp.LocalDate
import java.util.Calendar
import java.util.GregorianCalendar

// The day of this timestamp in the device's time zone. GregorianCalendar is used because
// Calendar.getInstance() can return another calendar for some locales (e.g. a Buddhist
// calendar for Thai), whose years don't match LocalDate's.
fun Long.toLocalDate(): LocalDate {
  val calendar = GregorianCalendar().apply { timeInMillis = this@toLocalDate }
  return LocalDate.of(
    calendar[Calendar.YEAR],
    calendar[Calendar.MONTH] + 1,
    calendar[Calendar.DAY_OF_MONTH]
  )
}

// The timestamp of the start of this day in the device's time zone.
fun LocalDate.startOfDayEpochMilli(): Long =
  GregorianCalendar(year, monthValue - 1, dayOfMonth).timeInMillis
