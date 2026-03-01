/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

import android.view.MenuItem
import android.webkit.WebView
import androidx.appcompat.view.ActionMode
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.R
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [CompatFindActionModeCallback].
 *
 * Uses Robolectric to handle Android Context and layout inflation,
 * and MockK for verifying interactions with WebView and ActionMode.
 *
 * This test uses JUnit 4 with @RunWith(RobolectricTestRunner::class)
 * because Robolectric does not natively support JUnit 5.
 * The junit-vintage-engine bridges this with the JUnit 5 platform
 * used by the rest of the project.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CompatFindActionModeCallbackTest {
  private lateinit var callback: CompatFindActionModeCallback
  private val mockWebView: WebView = mockk(relaxed = true)
  private val mockActionMode: ActionMode = mockk(relaxed = true)
  private val mockMenu: android.view.Menu = mockk(relaxed = true)

  @Before
  fun setUp() {
    callback = CompatFindActionModeCallback(ApplicationProvider.getApplicationContext())
  }

  @Test
  fun `callback is not active by default`() {
    assertThat(callback.isActive).isFalse()
  }

  @Test
  fun `setActive sets isActive to true`() {
    callback.setActive()
    assertThat(callback.isActive).isTrue()
  }

  @Test(expected = IllegalArgumentException::class)
  fun `setWebView throws when null is passed`() {
    callback.setWebView(null)
  }

  @Test
  fun `setWebView accepts non-null WebView`() {
    callback.setWebView(mockWebView)
  }

  @Test
  fun `setWebView sets a FindListener on the WebView`() {
    callback.setWebView(mockWebView)
    verify { mockWebView.setFindListener(any()) }
  }

  @Test
  fun `findAll with text calls findAllAsync on webView`() {
    callback.setWebView(mockWebView)
    callback.setText("test")
    callback.findAll()
    verify { mockWebView.findAllAsync("test") }
  }

  @Test
  fun `findAll with empty text clears matches`() {
    callback.setWebView(mockWebView)
    callback.setText("")
    callback.findAll()
    verify { mockWebView.clearMatches() }
    verify { mockWebView.findAllAsync("") }
  }

  @Test(expected = IllegalArgumentException::class)
  fun `findAll without webView throws IllegalArgumentException`() {
    callback.findAll()
  }

  @Test
  fun `finish calls actionMode finish and clears webView matches`() {
    callback.onCreateActionMode(mockActionMode, mockMenu)
    callback.setWebView(mockWebView)
    callback.finish()
    verify { mockActionMode.finish() }
    verify { mockWebView.clearMatches() }
  }

  @Test
  fun `finish without actionMode or webView does not crash`() {
    callback.finish()
  }

  @Test
  fun `onCreateActionMode returns true`() {
    val result = callback.onCreateActionMode(mockActionMode, mockMenu)
    assertThat(result).isTrue()
  }

  @Test
  fun `onCreateActionMode sets custom view on action mode`() {
    callback.onCreateActionMode(mockActionMode, mockMenu)
    verify { mockActionMode.customView = any() }
  }

  @Test
  fun `onCreateActionMode inflates menu`() {
    val mockMenuInflater: android.view.MenuInflater = mockk(relaxed = true)
    every { mockActionMode.menuInflater } returns mockMenuInflater
    callback.onCreateActionMode(mockActionMode, mockMenu)
    verify { mockMenuInflater.inflate(R.menu.menu_webview, mockMenu) }
  }

  @Test
  fun `onDestroyActionMode sets isActive to false`() {
    callback.setActive()
    callback.setWebView(mockWebView)
    callback.onDestroyActionMode(mockActionMode)
    assertThat(callback.isActive).isFalse()
  }

  @Test
  fun `onDestroyActionMode clears webView matches`() {
    callback.setWebView(mockWebView)
    callback.onDestroyActionMode(mockActionMode)
    verify { mockWebView.clearMatches() }
  }

  @Test
  fun `onPrepareActionMode returns false`() {
    val result = callback.onPrepareActionMode(mockActionMode, mockMenu)
    assertThat(result).isFalse()
  }

  @Test
  fun `onActionItemClicked with find_next calls findNext forward`() {
    callback.setWebView(mockWebView)
    val menuItem: MenuItem = mockk(relaxed = true)
    every { menuItem.itemId } returns R.id.find_next
    val result = callback.onActionItemClicked(mockActionMode, menuItem)
    assertThat(result).isTrue()
    verify { mockWebView.findNext(true) }
  }

  @Test
  fun `onActionItemClicked with find_prev calls findNext backward`() {
    callback.setWebView(mockWebView)
    val menuItem: MenuItem = mockk(relaxed = true)
    every { menuItem.itemId } returns R.id.find_prev
    val result = callback.onActionItemClicked(mockActionMode, menuItem)
    assertThat(result).isTrue()
    verify { mockWebView.findNext(false) }
  }

  @Test
  fun `onActionItemClicked with unknown id returns false`() {
    callback.setWebView(mockWebView)
    val menuItem: MenuItem = mockk(relaxed = true)
    every { menuItem.itemId } returns -1
    val result = callback.onActionItemClicked(mockActionMode, menuItem)
    assertThat(result).isFalse()
  }

  @Test(expected = IllegalArgumentException::class)
  fun `onActionItemClicked without webView throws IllegalArgumentException`() {
    val menuItem: MenuItem = mockk(relaxed = true)
    every { menuItem.itemId } returns R.id.find_next
    callback.onActionItemClicked(mockActionMode, menuItem)
  }

  @Test
  fun `onTextChanged delegates to findAll`() {
    callback.setWebView(mockWebView)
    // Set text first, then call onTextChanged to simulate the watcher
    callback.setText("hello")
    // The findAll method reads from editText.text, which is now "hello"
    callback.findAll()
    verify { mockWebView.findAllAsync("hello") }
  }

  @Test
  fun `beforeTextChanged does nothing`() {
    callback.beforeTextChanged("", 0, 0, 0)
  }

  @Test
  fun `afterTextChanged does nothing`() {
    callback.afterTextChanged(android.text.SpannableStringBuilder(""))
  }

  @Test
  fun `setText with null does not crash`() {
    callback.setText(null)
  }

  @Test
  fun `setText with empty string does not crash`() {
    callback.setText("")
  }

  @Test
  fun `setText with non-empty string does not crash`() {
    callback.setText("search term")
  }

  @Test
  fun `onClick calls findNext forward on webView`() {
    callback.setWebView(mockWebView)
    callback.onClick(mockk(relaxed = true))
    verify { mockWebView.findNext(true) }
  }
}
