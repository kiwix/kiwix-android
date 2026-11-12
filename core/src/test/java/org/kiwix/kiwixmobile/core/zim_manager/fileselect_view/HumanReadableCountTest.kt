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
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.R
import org.kiwix.sharedFunctions.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], application = TestApplication::class)
class HumanReadableCountTest {
  private val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun `displays one decimal when value is less than 10 with non-zero decimal`() {
    assertThat(
      HumanReadableCount("1490000000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("1.5B pages")
    assertThat(
      HumanReadableCount("1400000000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("1.4B pages")
    assertThat(
      HumanReadableCount("8200000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("8.2M pages")
    assertThat(
      HumanReadableCount("9900").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("9.9K pages")
    assertThat(
      HumanReadableCount("1490").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("1.5K pages")
  }

  @Test
  fun `omits trailing zero when decimal is zero for values less than 10`() {
    assertThat(
      HumanReadableCount("1000000000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("1B pages")
    assertThat(
      HumanReadableCount("1000000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("1M pages")
    assertThat(
      HumanReadableCount("1000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("1K pages")
    assertThat(
      HumanReadableCount("5").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("5 pages")
    assertThat(
      HumanReadableCount("1").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("1 pages")
    assertThat(
      HumanReadableCount("9").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("9 pages")
  }

  @Test
  fun `displays no decimals when value is 10 or greater`() {
    assertThat(
      HumanReadableCount("10").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("10 pages")
    assertThat(
      HumanReadableCount("999").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("999 pages")
    assertThat(
      HumanReadableCount("10000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("10K pages")
    assertThat(
      HumanReadableCount("10412").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("10K pages")
    assertThat(
      HumanReadableCount("216400").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("216K pages")
    assertThat(
      HumanReadableCount("216600").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("217K pages")
    assertThat(
      HumanReadableCount("10400000000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("10B pages")
    assertThat(
      HumanReadableCount("14800000000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("15B pages")
    assertThat(
      HumanReadableCount("456100000000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("456B pages")
    assertThat(
      HumanReadableCount("84400000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("84M pages")
    assertThat(
      HumanReadableCount("84600000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("85M pages")
    assertThat(
      HumanReadableCount("100000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("100K pages")
  }

  @Test
  fun `moves to the next unit when the value rounds up to 1000`() {
    assertThat(
      HumanReadableCount("999400").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("999K pages")
    assertThat(
      HumanReadableCount("999499").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("999K pages")
    assertThat(
      HumanReadableCount("999500").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("1M pages")
    assertThat(
      HumanReadableCount("999999").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("1M pages")
    assertThat(
      HumanReadableCount("999500000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("1B pages")
    assertThat(
      HumanReadableCount("999700000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("1B pages")
    assertThat(
      HumanReadableCount("999500000000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("1T pages")
  }

  @Test
  fun `formats article and media counts matching rounding rules`() {
    assertThat(
      HumanReadableCount("0").toHumanReadable(context, R.string.pagesCount)
    ).isEmpty()
    assertThat(
      HumanReadableCount("5").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("5 pages")
    assertThat(
      HumanReadableCount("1490").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("1.5K pages")
    assertThat(
      HumanReadableCount("9940").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("9.9K pages")
    assertThat(
      HumanReadableCount("9950").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("10K pages")
    assertThat(
      HumanReadableCount("10412").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("10K pages")
    assertThat(
      HumanReadableCount("216600").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("217K pages")
    assertThat(
      HumanReadableCount("8200000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("8.2M pages")
    assertThat(
      HumanReadableCount("9940000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("9.9M pages")
    assertThat(
      HumanReadableCount("9950000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("10M pages")
    assertThat(
      HumanReadableCount("10400000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("10M pages")
    assertThat(
      HumanReadableCount("999499").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("999K pages")
    assertThat(
      HumanReadableCount("999500").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("1M pages")
    assertThat(
      HumanReadableCount("999700000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("1B pages")
    assertThat(
      HumanReadableCount("10400000000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("10B pages")
    assertThat(
      HumanReadableCount("456100000000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("456B pages")
    assertThat(
      HumanReadableCount("1000000000000").toHumanReadable(context, R.string.pagesCount)
    ).isEqualTo("1T pages")
  }

  @Test
  fun `returns empty string when input is null`() {
    assertThat(HumanReadableCount(null).toHumanReadable(context, R.string.pagesCount))
      .isEmpty()
  }

  @Test
  fun `returns empty string when input is not a number`() {
    assertThat(HumanReadableCount("invalid").toHumanReadable(context, R.string.pagesCount))
      .isEmpty()
    assertThat(HumanReadableCount("   ").toHumanReadable(context, R.string.pagesCount))
      .isEmpty()
    assertThat(HumanReadableCount("12.3").toHumanReadable(context, R.string.pagesCount))
      .isEmpty()
    assertThat(HumanReadableCount("1,000").toHumanReadable(context, R.string.pagesCount))
      .isEmpty()
  }

  @Test
  fun `returns empty string when input is zero`() {
    assertThat(HumanReadableCount("0").toHumanReadable(context, R.string.pagesCount))
      .isEmpty()
  }

  @Test
  fun `returns empty string when input is negative`() {
    assertThat(HumanReadableCount("-1").toHumanReadable(context, R.string.pagesCount))
      .isEmpty()
    assertThat(HumanReadableCount("-10").toHumanReadable(context, R.string.pagesCount))
      .isEmpty()
  }

  @Test
  fun `returns empty string when input exceeds Long MAX_VALUE`() {
    assertThat(
      HumanReadableCount("9999999999999999999999999")
        .toHumanReadable(context, R.string.pagesCount)
    ).isEmpty()
  }

  @Test
  fun `formats properly when input equals Long MAX_VALUE`() {
    assertThat(
      HumanReadableCount(Long.MAX_VALUE.toString())
        .toHumanReadable(context, R.string.pagesCount)
    ).isNotEmpty()
  }

  @Test
  fun `formats pages count using the real pagesCount string resource`() {
    val result = HumanReadableCount("216600").toHumanReadable(context, R.string.pagesCount)
    assertThat(result).isEqualTo("217K pages")
  }

  @Test
  fun `formats media count using the real mediaCount string resource`() {
    val result = HumanReadableCount("216600").toHumanReadable(context, R.string.mediaCount)
    assertThat(result).isEqualTo("217K media")
  }
}
