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

package org.kiwix.kiwixmobile.core

import android.content.Context
import android.content.res.Configuration
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

class DarkModeConfigTest {
  private lateinit var darkModeConfig: DarkModeConfig
  private lateinit var sharedPreferenceUtil: SharedPreferenceUtil
  private lateinit var context: Context

  @BeforeEach
  fun setUp() {
    sharedPreferenceUtil = mockk()
    context = mockk()
    darkModeConfig = DarkModeConfig(sharedPreferenceUtil, context)
  }

  @Test
  fun `should return true when dark mode is ON`() {
    every { sharedPreferenceUtil.darkMode } returns DarkModeConfig.Mode.ON
    val result = darkModeConfig.isDarkModeActive()
    assertTrue(result)
  }

  @Test
  fun `should return false when dark mode is OFF`() {
    every { sharedPreferenceUtil.darkMode } returns DarkModeConfig.Mode.OFF
    val result = darkModeConfig.isDarkModeActive()
    assertFalse(result)
  }

  @Test
  fun `should return true when dark mode is SYSTEM and uiMode is ON`() {
    val configuration = Configuration().apply {
      uiMode = Configuration.UI_MODE_NIGHT_YES
    }
    every { context.resources } returns mockk(relaxed = true)
    every { context.resources.configuration } returns configuration
    every { sharedPreferenceUtil.darkMode } returns DarkModeConfig.Mode.SYSTEM
    val result = darkModeConfig.isDarkModeActive()
    assertTrue(result)
  }

  @Test
  fun `should return false when dark mode is SYSTEM and uiMode is OFF`() {
    val configuration = Configuration().apply {
      uiMode = Configuration.UI_MODE_NIGHT_NO
    }
    every { context.resources } returns mockk(relaxed = true)
    every { context.resources.configuration } returns configuration
    every { sharedPreferenceUtil.darkMode } returns DarkModeConfig.Mode.SYSTEM
    val result = darkModeConfig.isDarkModeActive()
    assertFalse(result)
  }

  @Test
  fun `should return false when dark mode is SYSTEM and uiMode is NOT_SET`() {
    val configuration = Configuration().apply {
      uiMode = Configuration.UI_MODE_NIGHT_UNDEFINED
    }
    every { context.resources } returns mockk(relaxed = true)
    every { context.resources.configuration } returns configuration
    every { sharedPreferenceUtil.darkMode } returns DarkModeConfig.Mode.SYSTEM
    val result = darkModeConfig.isDarkModeActive()
    assertFalse(result)
  }

  @Test
  fun `should call setMode during init`() {
    every { sharedPreferenceUtil.darkModes() } returns mockk(relaxed = true)
    darkModeConfig.init()
    verify { sharedPreferenceUtil.darkModes() }
  }
}
