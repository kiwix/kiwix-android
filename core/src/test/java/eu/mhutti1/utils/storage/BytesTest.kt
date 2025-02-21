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

package eu.mhutti1.utils.storage

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class BytesTest {
  @Test
  fun `byte formatting range checks`() {
    assertBytesGetFormatted(1, "1 Bytes")
    assertBytesGetFormatted(KB - 1, "1023 Bytes")

    assertBytesGetFormatted(KB, "1 KB")
    assertBytesGetFormatted(MB - 1, "1024 KB")

    assertBytesGetFormatted(MB, "1 MB")
    assertBytesGetFormatted(GB - 1, "1024 MB")

    assertBytesGetFormatted(GB, "1 GB")
    assertBytesGetFormatted(TB - 1, "1024 GB")

    assertBytesGetFormatted(TB, "1 TB")
    assertBytesGetFormatted(PB - 1, "1024 TB")

    assertBytesGetFormatted(PB, "1 PB")
    assertBytesGetFormatted(EB - 1, "1024 PB")

    assertBytesGetFormatted(EB, "1 EB")
    assertBytesGetFormatted(Long.MAX_VALUE, "8 EB")
  }

  private fun assertBytesGetFormatted(size: Long, expected: String) {
    assertThat(Bytes(size).humanReadable).isEqualTo(expected)
  }
}
