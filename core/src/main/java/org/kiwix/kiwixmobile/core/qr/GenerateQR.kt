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
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import javax.inject.Inject

/**
 * Utility class to generate QR codes.
 */
class GenerateQR @Inject constructor() {
  /**
   * Create a QR code for the given [code].
   *
   * @param code The code to encode in the QR code.
   * @param size The size of the QR code.
   * @param foregroundColor The color of the QR code.
   * @param backgroundColor The background color of the QR code.
   */
  fun createQR(
    code: String,
    size: Int = 512,
    foregroundColor: Int = Color.BLACK,
    backgroundColor: Int = Color.WHITE
  ): Bitmap {
    val hints =
      hashMapOf<EncodeHintType, Int>().also {
        it[EncodeHintType.MARGIN] = 1
      }
    val bits = QRCodeWriter().encode(code, BarcodeFormat.QR_CODE, size, size, hints)
    return Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).also {
      for (x in 0 until size) {
        for (y in 0 until size) {
          it.setPixel(x, y, if (bits[x, y]) foregroundColor else backgroundColor)
        }
      }
    }
  }
}
