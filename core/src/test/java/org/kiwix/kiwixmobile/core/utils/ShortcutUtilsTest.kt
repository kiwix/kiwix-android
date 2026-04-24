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

package org.kiwix.kiwixmobile.core.utils

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], shadows = [ShadowBuild::class])
class ShortcutUtilsTest {
  @Before
  fun setUp() {
    mockkStatic(Process::class)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `isXiaomiDevice returns true for known Xiaomi brands`() {
    listOf("Xiaomi", "Redmi", "POCO", "Blackshark").forEach { brand ->
      assertThat(ShortcutUtils.isXiaomiDevice(brand))
        .withFailMessage("Failed to detect brand as Xiaomi-family: $brand")
        .isTrue
    }
  }

  @Test
  fun `isXiaomiDevice returns false for other brands`() {
    listOf("Samsung", "Google", "OnePlus", "Motorola", "Sony").forEach { brand ->
      assertThat(ShortcutUtils.isXiaomiDevice(brand))
        .withFailMessage("Should not detect $brand as Xiaomi-family")
        .isFalse
    }
  }

  @Test
  fun `isShortcutPermissionGranted returns true for non-Xiaomi devices`() {
    ShadowBuild.setManufacturer("Google")
    val context = mockk<Context>()
    assertThat(ShortcutUtils.isShortcutPermissionGranted(context)).isTrue
  }

  @Test
  fun `isShortcutPermissionGranted returns false on Xiaomi when permission check cannot be verified`() {
    ShadowBuild.setManufacturer("Xiaomi")
    val context = mockk<Context>()
    val appOpsManager = mockk<AppOpsManager>()

    every { context.getSystemService(Context.APP_OPS_SERVICE) } returns appOpsManager
    every { context.packageName } returns "org.kiwix.kiwixmobile"
    every { Process.myUid() } returns 1000

    // Since reflection in a test environment doesn't have the real MIUI classes,
    // it will trigger the 'catch' block. We verify it returns 'false' (safe default)
    // rather than crashing.
    assertThat(ShortcutUtils.isShortcutPermissionGranted(context)).isFalse
  }

  @Test
  fun `addBookShortcut returns ReaderNull if zimFileReader is null`() {
    val context = mockk<Context>()
    assertThat(ShortcutUtils.addBookShortcut(context, null, "test_url"))
      .isEqualTo(ShortcutResult.ReaderNull)
  }

  @Test
  fun `openMiuiPermissionEditor does not crash when intent fails`() {
    val context = mockk<Context>(relaxed = true)
    every { context.startActivity(any()) } throws RuntimeException("Activity not found")

    // Should not crash, just log errors and try fallback
    ShortcutUtils.openMiuiPermissionEditor(context)
  }
}
