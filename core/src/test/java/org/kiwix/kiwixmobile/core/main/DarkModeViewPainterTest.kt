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

package org.kiwix.kiwixmobile.core.main

import android.view.View
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.DarkModeConfig

class DarkModeViewPainterTest {
  private lateinit var darkModeConfig: DarkModeConfig
  private lateinit var darkModeViewPainter: DarkModeViewPainter
  private lateinit var view: View

  @BeforeEach
  fun setUp() {
    darkModeConfig = mockk()
    view = mockk(relaxed = true)
    darkModeViewPainter = DarkModeViewPainter(darkModeConfig)
  }

  @Test
  fun `should activate dark mode when dark mode is active and criteria is true`() {
    every { darkModeConfig.isDarkModeActive() } returns true
    val shouldActivateCriteria: (View) -> Boolean = { true }
    darkModeViewPainter.update(view, shouldActivateCriteria)
    verify { view.setLayerType(View.LAYER_TYPE_HARDWARE, any()) }
  }

  @Test
  fun `should not activate dark mode when dark mode is active but criteria is false`() {
    every { darkModeConfig.isDarkModeActive() } returns true
    val shouldActivateCriteria: (View) -> Boolean = { false }
    darkModeViewPainter.update(view, shouldActivateCriteria)
    verify(exactly = 0) { view.setLayerType(View.LAYER_TYPE_HARDWARE, any()) }
  }

  @Test
  fun `should deactivate dark mode when dark mode is inactive`() {
    every { darkModeConfig.isDarkModeActive() } returns false
    darkModeViewPainter.update(view)
    verify { view.setLayerType(View.LAYER_TYPE_NONE, null) }
  }

  @Test
  fun `should handle null views without crashing`() {
    every { darkModeConfig.isDarkModeActive() } returns true
    darkModeViewPainter.update(null)
    assertTrue(true)
  }

  @Test
  fun `should activate dark mode for multiple additional views when dark mode is active`() {
    every { darkModeConfig.isDarkModeActive() } returns true
    val additionalView1 = mockk<View>(relaxed = true)
    val additionalView2 = mockk<View>(relaxed = true)
    darkModeViewPainter.update(view, { true }, additionalView1, additionalView2)
    verify { view.setLayerType(View.LAYER_TYPE_HARDWARE, any()) }
    verify { additionalView1.setLayerType(View.LAYER_TYPE_HARDWARE, any()) }
    verify { additionalView2.setLayerType(View.LAYER_TYPE_HARDWARE, any()) }
  }

  @Test
  fun `should deactivate dark mode for multiple additional views when dark mode is inactive`() {
    every { darkModeConfig.isDarkModeActive() } returns false
    val additionalView1 = mockk<View>(relaxed = true)
    val additionalView2 = mockk<View>(relaxed = true)
    darkModeViewPainter.update(view, { true }, additionalView1, additionalView2)
    verify { view.setLayerType(View.LAYER_TYPE_NONE, null) }
    verify { additionalView1.setLayerType(View.LAYER_TYPE_NONE, null) }
    verify { additionalView2.setLayerType(View.LAYER_TYPE_NONE, null) }
  }

  @Test
  fun `should handle null additional views without crashing when dark mode is active`() {
    every { darkModeConfig.isDarkModeActive() } returns true
    darkModeViewPainter.update(view, { true }, null, null)
    verify { view.setLayerType(View.LAYER_TYPE_HARDWARE, any()) }
  }

  @Test
  fun `should handle null additional views without crashing when dark mode is inactive`() {
    every { darkModeConfig.isDarkModeActive() } returns false
    darkModeViewPainter.update(view, { true }, null, null)
    verify { view.setLayerType(View.LAYER_TYPE_NONE, null) }
  }

  @Test
  fun `should only update main view when no additional views are passed and dark mode is active`() {
    every { darkModeConfig.isDarkModeActive() } returns true
    darkModeViewPainter.update(view)
    verify { view.setLayerType(View.LAYER_TYPE_HARDWARE, any()) }
  }

  @Test
  fun shouldOnlyUpdateMainViewWhenNoAdditionalViewsArePassedAndDarkModeIsInactive() {
    every { darkModeConfig.isDarkModeActive() } returns false
    darkModeViewPainter.update(view)
    verify { view.setLayerType(View.LAYER_TYPE_NONE, null) }
  }

  @Test
  fun `should handle empty additional views array without crashing`() {
    every { darkModeConfig.isDarkModeActive() } returns true
    darkModeViewPainter.update(view, { true }, *arrayOf())
    verify { view.setLayerType(View.LAYER_TYPE_HARDWARE, any()) }
  }
}
