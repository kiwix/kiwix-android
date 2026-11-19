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

package org.kiwix.kiwixmobile.webserver.wifi_hotspot

import android.app.NotificationManager
import android.graphics.Bitmap
import android.os.Build
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.qr.GenerateQR
import org.kiwix.sharedFunctions.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU], application = TestApplication::class)
class HotspotNotificationManagerTest {
  private val notificationManager: NotificationManager = mockk(relaxed = true)
  private val generateQR: GenerateQR = mockk()
  private val mockBitmap: Bitmap = mockk(relaxed = true)
  private val context = RuntimeEnvironment.getApplication()

  private lateinit var hotspotNotificationManager: HotspotNotificationManager

  @Before
  fun setUp() {
    every { generateQR.createQR(any()) } returns mockBitmap
    hotspotNotificationManager = HotspotNotificationManager(
      notificationManager,
      context,
      generateQR
    )
  }

  @Test
  fun `when uri is non-blank then generateQR createQR is called`() {
    hotspotNotificationManager.buildForegroundNotification("http://192.168.43.1:8080")
    verify(exactly = 1) { generateQR.createQR("http://192.168.43.1:8080") }
  }

  @Test
  fun `when uri is null then generateQR createQR is not called`() {
    hotspotNotificationManager.buildForegroundNotification(null)
    verify(exactly = 0) { generateQR.createQR(any()) }
  }

  @Test
  fun `when uri is empty then generateQR createQR is not called`() {
    hotspotNotificationManager.buildForegroundNotification("")
    verify(exactly = 0) { generateQR.createQR(any()) }
  }

  @Test
  fun `when uri is whitespace only then generateQR createQR is not called`() {
    hotspotNotificationManager.buildForegroundNotification("   ")
    verify(exactly = 0) { generateQR.createQR(any()) }
  }

  @Test
  fun `when generateQR createQR throws an exception it is handled gracefully without crashing`() {
    every { generateQR.createQR(any()) } throws IllegalArgumentException("Found empty contents")
    hotspotNotificationManager.buildForegroundNotification("http://192.168.43.1:8080")
    verify(exactly = 1) { generateQR.createQR("http://192.168.43.1:8080") }
  }
}
