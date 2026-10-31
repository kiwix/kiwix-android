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

package org.kiwix.kiwixmobile.core.zim_manager

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ByteTest {
  @Nested
  inner class SmallUnitValues {
    @Test
    fun `displays one decimal when value is less than 10 with non-zero decimal`() {
      assertThat(Byte("1490000000").humanReadable).isEqualTo("1.5 GB")
      assertThat(Byte("1400000000").humanReadable).isEqualTo("1.4 GB")
      assertThat(Byte("8200000").humanReadable).isEqualTo("8.2 MB")
      assertThat(Byte("9900").humanReadable).isEqualTo("9.9 KB")
    }

    @Test
    fun `omits trailing zero when decimal is zero for values less than 10`() {
      assertThat(Byte("1000000000").humanReadable).isEqualTo("1 GB")
      assertThat(Byte("1000000").humanReadable).isEqualTo("1 MB")
      assertThat(Byte("1000").humanReadable).isEqualTo("1 KB")
      assertThat(Byte("5").humanReadable).isEqualTo("5 B")
    }
  }

  @Nested
  inner class LargeUnitValues {
    @Test
    fun `displays no decimals when value is 10 or greater`() {
      assertThat(Byte("10400000000").humanReadable).isEqualTo("10 GB")
      assertThat(Byte("14800000000").humanReadable).isEqualTo("15 GB")
      assertThat(Byte("456100000000").humanReadable).isEqualTo("456 GB")
      assertThat(Byte("84400000").humanReadable).isEqualTo("84 MB")
      assertThat(Byte("84600000").humanReadable).isEqualTo("85 MB")
      assertThat(Byte("100000").humanReadable).isEqualTo("100 KB")
    }
  }

  @Nested
  inner class UnitBoundaryValues {
    @Test
    fun `moves to the next unit when the value rounds up to 1000`() {
      assertThat(Byte("999700000").humanReadable).isEqualTo("1 GB")
      assertThat(Byte("999999").humanReadable).isEqualTo("1 MB")
      assertThat(Byte("999500").humanReadable).isEqualTo("1 MB")
      assertThat(Byte("999400").humanReadable).isEqualTo("999 KB")
    }

    @Test
    fun `displays zero bytes instead of crashing`() {
      assertThat(Byte("0").humanReadable).isEqualTo("0 B")
    }
  }

  @Nested
  inner class InvalidOrNullValues {
    @Test
    fun `returns empty string when input is null`() {
      assertThat(Byte(null).humanReadable).isEmpty()
    }

    @Test
    fun `returns empty string when input is not a number`() {
      assertThat(Byte("invalid").humanReadable).isEmpty()
    }
  }
}
