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
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

class ThemeConfigTest {
  private lateinit var themeConfig: ThemeConfig
  private lateinit var sharedPreferenceUtil: SharedPreferenceUtil
  private lateinit var context: Context

  @OptIn(ExperimentalCoroutinesApi::class)
  private val testDispatcher = UnconfinedTestDispatcher()

  @OptIn(ExperimentalCoroutinesApi::class)
  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    sharedPreferenceUtil = mockk()
    context = mockk()
    themeConfig = ThemeConfig(sharedPreferenceUtil, context)
  }

  @Test
  fun `should return true when dark mode is ON`() {
    every { sharedPreferenceUtil.appTheme } returns ThemeConfig.Theme.DARK
    val result = themeConfig.isDarkTheme()
    assertTrue(result)
  }

  @Test
  fun `should return false when dark mode is OFF`() {
    every { sharedPreferenceUtil.appTheme } returns ThemeConfig.Theme.LIGHT
    val result = themeConfig.isDarkTheme()
    assertFalse(result)
  }

  @Test
  fun `should return true when dark mode is SYSTEM and uiMode is ON`() {
    val configuration =
      Configuration().apply {
        uiMode = Configuration.UI_MODE_NIGHT_YES
      }
    every { context.resources } returns mockk(relaxed = true)
    every { context.resources.configuration } returns configuration
    every { sharedPreferenceUtil.appTheme } returns ThemeConfig.Theme.SYSTEM
    val result = themeConfig.isDarkTheme()
    assertTrue(result)
  }

  @Test
  fun `should return false when dark mode is SYSTEM and uiMode is OFF`() {
    val configuration =
      Configuration().apply {
        uiMode = Configuration.UI_MODE_NIGHT_NO
      }
    every { context.resources } returns mockk(relaxed = true)
    every { context.resources.configuration } returns configuration
    every { sharedPreferenceUtil.appTheme } returns ThemeConfig.Theme.SYSTEM
    val result = themeConfig.isDarkTheme()
    assertFalse(result)
  }

  @Test
  fun `should return false when dark mode is SYSTEM and uiMode is NOT_SET`() {
    val configuration =
      Configuration().apply {
        uiMode = Configuration.UI_MODE_NIGHT_UNDEFINED
      }
    every { context.resources } returns mockk(relaxed = true)
    every { context.resources.configuration } returns configuration
    every { sharedPreferenceUtil.appTheme } returns ThemeConfig.Theme.SYSTEM
    val result = themeConfig.isDarkTheme()
    assertFalse(result)
  }

  @Test
  fun `should call setMode during init`() {
    every { sharedPreferenceUtil.appThemes() } returns mockk(relaxed = true)
    val spy = spyk(themeConfig)
    spy.init()
    verify { sharedPreferenceUtil.appThemes() }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }
}
