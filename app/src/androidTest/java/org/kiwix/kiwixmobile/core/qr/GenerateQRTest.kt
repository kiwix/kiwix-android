/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.qr

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.utils.TestingUtils.FAIL_FAST_RULE_ORDER
import org.kiwix.kiwixmobile.testutils.FailFastRule

@RunWith(AndroidJUnit4::class)
@SmallTest
class GenerateQRTest {
  @Rule(order = FAIL_FAST_RULE_ORDER)
  @JvmField
  val failFastRule = FailFastRule()

  @Test fun testCreateQR() {
    val qr = GenerateQR().createQR("https://kiwix.org")
    assertEquals(524288, qr.byteCount)
    assertEquals(512, qr.width)
    assertEquals(512, qr.height)
    qr.recycle()
  }

  @Test fun testCreateQRWithCustomDimen() {
    val qr = GenerateQR().createQR("https://kiwix.org", 128)
    assertEquals(32768, qr.byteCount)
    assertEquals(128, qr.width)
    assertEquals(128, qr.height)
    qr.recycle()
  }

  @Test fun testCreateQRWithEmptyContentThrows() {
    assertThrows(IllegalArgumentException::class.java) {
      GenerateQR().createQR("")
    }
  }

  @Test fun testCreateQRWithBlankContent() {
    assertQrCreated(GenerateQR().createQR("   "))
  }

  @Test fun testCreateQRWithServerAddresses() {
    listOf(
      "http://192.168.43.1:8080/",
      "http://[fe80::1c2b:3cff:fe4d:5e6f]:8080/",
      "http://10.0.2.15:8080/content/wikipedia_en_all/A/Main_Page?lang=हिन्दी&q=a b"
    ).forEach { assertQrCreated(GenerateQR().createQR(it)) }
  }

  @Test fun testCreateQRWithLongContent() {
    assertQrCreated(GenerateQR().createQR("http://192.168.43.1:8080/" + "a".repeat(1000)))
  }

  @Test fun testCreateQRWithContentExceedingCapacityThrows() {
    assertThrows(Exception::class.java) {
      GenerateQR().createQR("a".repeat(5000))
    }
  }

  private fun assertQrCreated(qr: Bitmap) {
    assertEquals(512, qr.width)
    assertEquals(512, qr.height)
    qr.recycle()
  }
}
