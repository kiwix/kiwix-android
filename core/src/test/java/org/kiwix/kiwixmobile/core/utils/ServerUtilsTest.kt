/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class ServerUtilsTest {

  @Test
  internal fun `formatIpForAndroidPie should return first ip occurrence of pie address`() {
    assertThat(
      ServerUtils.formatIpForAndroidPie(
        """
      fec0::15:b2ff:fe00:0
      fec0::15d6:ece1:61fb:5b55
      192.168.232.2
      fec0::d8d1:9ff:fe42:160c
      fec0::8d6e:2327:6d9f:ce75
      192.168.200.2
        """.trimIndent()
      )
    ).isEqualTo(
      "192.168.232.2"
    )
  }

  @Test
  internal fun `formatIpForAndroidPie should return full ip on given ip`() {
    assertThat(
      ServerUtils.formatIpForAndroidPie("192.168.232.2")
    ).isEqualTo("192.168.232.2")
  }

  @Test(expected = IllegalArgumentException::class)
  internal fun `formatIpForAndroidPie should throw invalid argument exception on invalid ip`() {
    ServerUtils.formatIpForAndroidPie("invalid ip")
  }
}
