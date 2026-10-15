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

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.threeten.bp.LocalDate
import java.util.Locale
import java.util.TimeZone

class DateExtensionsTest {
  private val defaultLocale = Locale.getDefault()
  private val defaultTimeZone = TimeZone.getDefault()

  @AfterEach
  fun tearDown() {
    Locale.setDefault(defaultLocale)
    TimeZone.setDefault(defaultTimeZone)
  }

  @Test
  fun toLocalDateUsesGregorianYearsInEveryLocale() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    // 2026-10-15T12:00:00Z
    val timeStamp = 1_792_065_600_000L
    listOf(
      Locale.US,
      Locale.forLanguageTag("th-TH"),
      Locale.forLanguageTag("ja-JP-u-ca-japanese"),
      Locale.forLanguageTag("fa-IR")
    ).forEach { locale ->
      Locale.setDefault(locale)
      assertEquals(LocalDate.of(2026, 10, 15), timeStamp.toLocalDate(), "date in $locale")
    }
  }

  @Test
  fun startOfDayEpochMilliIsMidnightInTheDeviceTimeZone() {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
    Locale.setDefault(Locale.forLanguageTag("th-TH"))
    val day = LocalDate.of(2026, 10, 15)

    // 2026-10-15T00:00:00+05:30
    assertEquals(1_792_002_600_000L, day.startOfDayEpochMilli())
    assertEquals(day, day.startOfDayEpochMilli().toLocalDate())
    assertEquals(day.minusDays(1), (day.startOfDayEpochMilli() - 1).toLocalDate())
  }
}
