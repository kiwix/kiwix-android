package eu.mhutti1.utils.storage

/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class BytesTest {
  @Test
  fun `byte formatting range checks`() {
    assertBytesGetFormatted(1, "1 Bytes")
    assertBytesGetFormatted(Kb - 1, "1023 Bytes")

    assertBytesGetFormatted(Kb, "1 KB")
    assertBytesGetFormatted(Mb - 1, "1024 KB")

    assertBytesGetFormatted(Mb, "1 MB")
    assertBytesGetFormatted(Gb - 1, "1024 MB")

    assertBytesGetFormatted(Gb, "1 GB")
    assertBytesGetFormatted(Tb - 1, "1024 GB")

    assertBytesGetFormatted(Tb, "1 TB")
    assertBytesGetFormatted(Pb - 1, "1024 TB")

    assertBytesGetFormatted(Pb, "1 PB")
    assertBytesGetFormatted(Eb - 1, "1024 PB")

    assertBytesGetFormatted(Eb, "1 EB")
    assertBytesGetFormatted(Long.MAX_VALUE, "8 EB")
  }

  private fun assertBytesGetFormatted(size: Long, expected: String) {
    assertThat(Bytes(size).humanReadable).isEqualTo(expected)
  }
}
