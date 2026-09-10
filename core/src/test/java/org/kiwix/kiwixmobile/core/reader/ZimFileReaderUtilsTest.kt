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

package org.kiwix.kiwixmobile.core.reader

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * `truncateMimeType` previously used `String.replace(String, String)`, which does a
 * literal substring replacement rather than applying the regex pattern - it never
 * actually truncated anything. See #5070.
 */
internal class ZimFileReaderUtilsTest {
  @Test
  fun `truncateMimeType drops everything after the first space`() {
    assertThat("text/html and some junk".truncateMimeType).isEqualTo("text/html")
  }

  @Test
  fun `truncateMimeType drops the charset parameter after a semicolon`() {
    assertThat("text/html; charset=utf-8".truncateMimeType).isEqualTo("text/html")
  }

  @Test
  fun `truncateMimeType leaves a plain mimetype untouched`() {
    assertThat("application/octet-stream".truncateMimeType).isEqualTo("application/octet-stream")
  }
}
