/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.downloader.model

import android.content.Context
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.R

class SecondsTest {
  private lateinit var mockContext: Context

  @BeforeEach
  fun setUp() {
    mockContext = mockk(relaxed = true)
    mockkObject(CoreApp.Companion)
    mockkStatic(ContextCompat::class)
    every { CoreApp.instance } returns mockk(relaxed = true)
    every { ContextCompat.getContextForLanguage(any()) } returns mockContext
    every { mockContext.getString(R.string.time_day) } returns "day"
    every { mockContext.getString(R.string.time_hour) } returns "hour"
    every { mockContext.getString(R.string.time_minute) } returns "minute"
    every { mockContext.getString(R.string.time_second) } returns "second"
    every { mockContext.getString(R.string.time_left) } returns "left"
  }

  @AfterEach
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `toHumanReadableTime formats days correctly`() {
    val seconds = Seconds(86400L)
    assertThat(seconds.toHumanReadableTime()).isEqualTo("1 day left")
  }

  @Test
  fun `toHumanReadableTime formats multiple days correctly`() {
    val seconds = Seconds(172800L)
    assertThat(seconds.toHumanReadableTime()).isEqualTo("2 day left")
  }

  @Test
  fun `toHumanReadableTime formats hours correctly`() {
    val seconds = Seconds(3600L)
    assertThat(seconds.toHumanReadableTime()).isEqualTo("1 hour left")
  }

  @Test
  fun `toHumanReadableTime formats multiple hours correctly`() {
    val seconds = Seconds(7200L)
    assertThat(seconds.toHumanReadableTime()).isEqualTo("2 hour left")
  }

  @Test
  fun `toHumanReadableTime formats minutes correctly`() {
    val seconds = Seconds(60L)
    assertThat(seconds.toHumanReadableTime()).isEqualTo("1 minute left")
  }

  @Test
  fun `toHumanReadableTime formats multiple minutes correctly`() {
    val seconds = Seconds(300L)
    assertThat(seconds.toHumanReadableTime()).isEqualTo("5 minute left")
  }

  @Test
  fun `toHumanReadableTime formats seconds correctly`() {
    val seconds = Seconds(25L)
    assertThat(seconds.toHumanReadableTime()).isEqualTo("25 second left")
  }

  @Test
  fun `toHumanReadableTime formats zero seconds`() {
    val seconds = Seconds(0L)
    assertThat(seconds.toHumanReadableTime()).isEqualTo("0 second left")
  }

  @Test
  fun `toHumanReadableTime formats 1 second`() {
    val seconds = Seconds(1L)
    assertThat(seconds.toHumanReadableTime()).isEqualTo("1 second left")
  }

  @Test
  fun `toHumanReadableTime formats 59 seconds without rounding to minutes`() {
    val seconds = Seconds(29L)
    assertThat(seconds.toHumanReadableTime()).isEqualTo("29 second left")
  }

  @Test
  fun `seconds property stores the value correctly`() {
    val seconds = Seconds(42L)
    assertThat(seconds.seconds).isEqualTo(42L)
  }

  @Test
  fun `toHumanReadableTime rounds near one day to one day`() {
    val seconds = Seconds(86399L)
    assertThat(seconds.toHumanReadableTime()).isEqualTo("1 day left")
  }

  @Test
  fun `toHumanReadableTime rounds near one hour to one hour`() {
    val seconds = Seconds(3599L)
    assertThat(seconds.toHumanReadableTime()).isEqualTo("1 hour left")
  }

  @Test
  fun `toHumanReadableTime rounds near one minute to one minute`() {
    val seconds = Seconds(59L)
    assertThat(seconds.toHumanReadableTime()).isEqualTo("1 minute left")
  }

  @Test
  fun `handles very large durations`() {
    val seconds = Seconds(86400 * 10L)
    assertThat(seconds.toHumanReadableTime()).isEqualTo("10 day left")
  }

  @Test
  fun `context strings are retrieved for formatting`() {
    val seconds = Seconds(3600)
    seconds.toHumanReadableTime()

    verify { mockContext.getString(R.string.time_hour) }
    verify { mockContext.getString(R.string.time_left) }
  }
}
