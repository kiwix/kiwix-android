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

package org.kiwix.kiwixmobile.core.zim_manager.fileselect_view

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class HumanReadableCountTest {
  @Nested
  inner class SmallUnitValues {
    @Test
    fun `displays one decimal when value is less than 10 with non-zero decimal`() {
      assertThat(HumanReadableCount("1490000000").humanReadable).isEqualTo("1.5B")
      assertThat(HumanReadableCount("1400000000").humanReadable).isEqualTo("1.4B")
      assertThat(HumanReadableCount("8200000").humanReadable).isEqualTo("8.2M")
      assertThat(HumanReadableCount("9900").humanReadable).isEqualTo("9.9K")
      assertThat(HumanReadableCount("1490").humanReadable).isEqualTo("1.5K")
    }

    @Test
    fun `omits trailing zero when decimal is zero for values less than 10`() {
      assertThat(HumanReadableCount("1000000000").humanReadable).isEqualTo("1B")
      assertThat(HumanReadableCount("1000000").humanReadable).isEqualTo("1M")
      assertThat(HumanReadableCount("1000").humanReadable).isEqualTo("1K")
      assertThat(HumanReadableCount("5").humanReadable).isEqualTo("5")
      assertThat(HumanReadableCount("1").humanReadable).isEqualTo("1")
      assertThat(HumanReadableCount("9").humanReadable).isEqualTo("9")
    }
  }

  @Nested
  inner class LargeUnitValues {
    @Test
    fun `displays no decimals when value is 10 or greater`() {
      assertThat(HumanReadableCount("10").humanReadable).isEqualTo("10")
      assertThat(HumanReadableCount("999").humanReadable).isEqualTo("999")
      assertThat(HumanReadableCount("10000").humanReadable).isEqualTo("10K")
      assertThat(HumanReadableCount("10412").humanReadable).isEqualTo("10K")
      assertThat(HumanReadableCount("216400").humanReadable).isEqualTo("216K")
      assertThat(HumanReadableCount("216600").humanReadable).isEqualTo("217K")
      assertThat(HumanReadableCount("10400000000").humanReadable).isEqualTo("10B")
      assertThat(HumanReadableCount("14800000000").humanReadable).isEqualTo("15B")
      assertThat(HumanReadableCount("456100000000").humanReadable).isEqualTo("456B")
      assertThat(HumanReadableCount("84400000").humanReadable).isEqualTo("84M")
      assertThat(HumanReadableCount("84600000").humanReadable).isEqualTo("85M")
      assertThat(HumanReadableCount("100000").humanReadable).isEqualTo("100K")
    }
  }

  @Nested
  inner class UnitBoundaryValues {
    @Test
    fun `moves to the next unit when the value rounds up to 1000`() {
      assertThat(HumanReadableCount("999400").humanReadable).isEqualTo("999K")
      assertThat(HumanReadableCount("999499").humanReadable).isEqualTo("999K")
      assertThat(HumanReadableCount("999500").humanReadable).isEqualTo("1M")
      assertThat(HumanReadableCount("999999").humanReadable).isEqualTo("1M")
      assertThat(HumanReadableCount("999500000").humanReadable).isEqualTo("1B")
      assertThat(HumanReadableCount("999700000").humanReadable).isEqualTo("1B")
      assertThat(HumanReadableCount("999500000000").humanReadable).isEqualTo("1T")
    }
  }

  @Nested
  inner class ExpectedValuesRoundingTable {
    @Test
    fun `formats article and media counts matching rounding rules`() {
      assertThat(HumanReadableCount("0").humanReadable).isEmpty()
      assertThat(HumanReadableCount("5").humanReadable).isEqualTo("5")
      assertThat(HumanReadableCount("1490").humanReadable).isEqualTo("1.5K")
      assertThat(HumanReadableCount("9940").humanReadable).isEqualTo("9.9K")
      assertThat(HumanReadableCount("9950").humanReadable).isEqualTo("10K")
      assertThat(HumanReadableCount("10412").humanReadable).isEqualTo("10K")
      assertThat(HumanReadableCount("216600").humanReadable).isEqualTo("217K")
      assertThat(HumanReadableCount("8200000").humanReadable).isEqualTo("8.2M")
      assertThat(HumanReadableCount("9940000").humanReadable).isEqualTo("9.9M")
      assertThat(HumanReadableCount("9950000").humanReadable).isEqualTo("10M")
      assertThat(HumanReadableCount("10400000").humanReadable).isEqualTo("10M")
      assertThat(HumanReadableCount("999499").humanReadable).isEqualTo("999K")
      assertThat(HumanReadableCount("999500").humanReadable).isEqualTo("1M")
      assertThat(HumanReadableCount("999700000").humanReadable).isEqualTo("1B")
      assertThat(HumanReadableCount("10400000000").humanReadable).isEqualTo("10B")
      assertThat(HumanReadableCount("456100000000").humanReadable).isEqualTo("456B")
      assertThat(HumanReadableCount("1000000000000").humanReadable).isEqualTo("1T")
    }
  }

  @Nested
  inner class InvalidOrNullOrZeroValues {
    @Test
    fun `returns empty string when input is null`() {
      assertThat(HumanReadableCount(null).humanReadable).isEmpty()
    }

    @Test
    fun `returns empty string when input is not a number`() {
      assertThat(HumanReadableCount("invalid").humanReadable).isEmpty()
      assertThat(HumanReadableCount("   ").humanReadable).isEmpty()
      assertThat(HumanReadableCount("12.3").humanReadable).isEmpty()
      assertThat(HumanReadableCount("1,000").humanReadable).isEmpty()
    }

    @Test
    fun `returns empty string when input is zero`() {
      assertThat(HumanReadableCount("0").humanReadable).isEmpty()
    }

    @Test
    fun `returns empty string when input is negative`() {
      assertThat(HumanReadableCount("-1").humanReadable).isEmpty()
      assertThat(HumanReadableCount("-10").humanReadable).isEmpty()
    }

    @Test
    fun `returns empty string when input exceeds Long MAX_VALUE`() {
      assertThat(HumanReadableCount("9999999999999999999999999").humanReadable).isEmpty()
    }

    @Test
    fun `formats properly when input equals Long MAX_VALUE`() {
      assertThat(HumanReadableCount(Long.MAX_VALUE.toString()).humanReadable).isNotEmpty()
    }
  }

  @Nested
  inner class ToHumanReadableWithContext {
    private lateinit var mockContext: Context
    private val stringResId = 123

    @BeforeEach
    fun setUp() {
      mockContext = mockk()
    }

    @AfterEach
    fun tearDown() {
      unmockkAll()
    }

    @Test
    fun `formats string resource correctly using context`() {
      every { mockContext.getString(stringResId, "217K") } returns "217K articles"
      val result = HumanReadableCount("216600").toHumanReadable(mockContext, stringResId)
      assertThat(result).isEqualTo("217K articles")
    }

    @Test
    fun `returns empty string when count is invalid`() {
      val result = HumanReadableCount("invalid").toHumanReadable(mockContext, stringResId)
      assertThat(result).isEmpty()
    }

    @Test
    fun `returns empty string when count is zero`() {
      val result = HumanReadableCount("0").toHumanReadable(mockContext, stringResId)
      assertThat(result).isEmpty()
    }

    @Test
    fun `returns empty string when count is null`() {
      val result = HumanReadableCount(null).toHumanReadable(mockContext, stringResId)
      assertThat(result).isEmpty()
    }
  }
}
