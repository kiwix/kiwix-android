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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.sharedFunctions.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeConfigTest {
  private lateinit var themeConfig: ThemeConfig
  private lateinit var kiwixDataStore: KiwixDataStore
  private lateinit var context: Context

  @RegisterExtension
  val dispatcherRule = MainDispatcherRule()

  @OptIn(ExperimentalCoroutinesApi::class)
  @BeforeEach
  fun setUp() {
    kiwixDataStore = mockk()
    context = mockk()
    themeConfig = ThemeConfig(kiwixDataStore, context)
  }

  @Test
  fun isDarkTheme_whenDarkMode_returnsTrue() = runTest {
    every { kiwixDataStore.appTheme } returns flowOf(ThemeConfig.Theme.DARK)
    val result = themeConfig.isDarkTheme()
    assertTrue(result)
  }

  @Test
  fun isDarkTheme_whenLightMode_returnsFalse() = runTest {
    every { kiwixDataStore.appTheme } returns flowOf(ThemeConfig.Theme.LIGHT)
    val result = themeConfig.isDarkTheme()
    assertFalse(result)
  }

  @Test
  fun isDarkTheme_whenSystemModeAndUiModeNight_returnsTrue() = runTest {
    val configuration = Configuration().apply {
      uiMode = Configuration.UI_MODE_NIGHT_YES
    }
    every { context.resources } returns mockk(relaxed = true)
    every { context.resources.configuration } returns configuration
    every { kiwixDataStore.appTheme } returns flowOf(ThemeConfig.Theme.SYSTEM)
    val result = themeConfig.isDarkTheme()
    assertTrue(result)
  }

  @Test
  fun isDarkTheme_whenSystemModeAndUiModeDay_returnsFalse() = runTest {
    val configuration = Configuration().apply {
      uiMode = Configuration.UI_MODE_NIGHT_NO
    }
    every { context.resources } returns mockk(relaxed = true)
    every { context.resources.configuration } returns configuration
    every { kiwixDataStore.appTheme } returns flowOf(ThemeConfig.Theme.SYSTEM)
    val result = themeConfig.isDarkTheme()
    assertFalse(result)
  }

  @Test
  fun isDarkTheme_whenSystemModeAndUiModeNotSet_returnsFalse() = runTest {
    val configuration = Configuration().apply {
      uiMode = Configuration.UI_MODE_NIGHT_UNDEFINED
    }
    every { context.resources } returns mockk(relaxed = true)
    every { context.resources.configuration } returns configuration
    every { kiwixDataStore.appTheme } returns flowOf(ThemeConfig.Theme.SYSTEM)
    val result = themeConfig.isDarkTheme()
    assertFalse(result)
  }

  @Test
  fun init_whenCalled_setsIsThemeLoadedTrueAfterSyncRead() = runTest {
    every { kiwixDataStore.appTheme } returns flowOf(ThemeConfig.Theme.DARK)
    assertFalse(themeConfig.isThemeLoaded.value)
    themeConfig.init()
    advanceUntilIdle()
    assertTrue(themeConfig.isThemeLoaded.value)
  }

  @Test
  fun init_whenCalled_accessesAppThemeFromDataStore() = runTest {
    every { kiwixDataStore.appTheme } returns flowOf(ThemeConfig.Theme.DARK)
    val spy = spyk(themeConfig)
    assertFalse(spy.isThemeLoaded.value)
    spy.init()
    advanceUntilIdle()
    assertTrue(spy.isThemeLoaded.value)
  }

  @Test
  fun init_whenCalledWithLightTheme_setsIsThemeLoadedTrue() = runTest {
    every { kiwixDataStore.appTheme } returns flowOf(ThemeConfig.Theme.LIGHT)
    themeConfig.init()
    advanceUntilIdle()
    assertTrue(themeConfig.isThemeLoaded.value)
  }
}
