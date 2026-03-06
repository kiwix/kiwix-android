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

import android.content.Context
import android.os.Build
import android.text.SpannableStringBuilder
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.view.ActionMode
import androidx.test.core.app.ApplicationProvider
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
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
@Config(sdk = [Build.VERSION_CODES.R])
class CompatFindActionModeCallbackTest {
  private lateinit var callback: CompatFindActionModeCallback
  private val mockWebView: WebView = mockk(relaxed = true)
  private val mockActionMode: ActionMode = mockk(relaxed = true)
  private val mockMenu: Menu = mockk(relaxed = true)

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
    assertThat(getPrivateWebView()).isEqualTo(mockWebView)
  }

  @Test
  fun `setWebView sets listener and shows result view`() {
    callback.setWebView(mockWebView)
    verify { mockWebView.setFindListener(any()) }
    assertThat(getPrivateTextView().visibility).isEqualTo(View.VISIBLE)
  }

  @Test
  fun `findAll with text calls findAllAsync`() {
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

  @Test(expected = IllegalArgumentException::class)
  fun `findNext throws when webView not set`() {
    callback.onClick(mockk(relaxed = true))
  }

  @Test
  fun `onClick calls findNext forward`() {
    callback.setWebView(mockWebView)
    callback.onClick(mockk(relaxed = true))

    verify { mockWebView.findNext(true) }
  }

  @Test(expected = IllegalArgumentException::class)
  fun `onClick throws when webView is null`() {
    callback.onClick(mockk(relaxed = true))
  }

  @Test
  fun `find listener updates result text`() {
    val slot = slot<WebView.FindListener>()
    every { mockWebView.setFindListener(capture(slot)) } just Runs

    callback.setWebView(mockWebView)
    callback.setText("hello")

    slot.captured.onFindResultReceived(0, 5, true)

    assertThat(getPrivateTextView().text.toString()).isEqualTo("1/5")
  }

  @Test
  fun `find listener shows zero results`() {
    val slot = slot<WebView.FindListener>()
    every { mockWebView.setFindListener(capture(slot)) } just Runs

    callback.setWebView(mockWebView)
    callback.setText("hello")

    slot.captured.onFindResultReceived(0, 0, true)

    assertThat(getPrivateTextView().text.toString()).isEqualTo("0/0")
  }

  @Test
  fun `find listener clears text when input empty`() {
    val slot = slot<WebView.FindListener>()
    every { mockWebView.setFindListener(capture(slot)) } just Runs

    callback.setWebView(mockWebView)
    callback.setText("")

    slot.captured.onFindResultReceived(0, 5, true)

    assertThat(getPrivateTextView().text.toString()).isEqualTo("")
  }

  @Test
  fun `find listener reacts to updated text`() {
    val slot = slot<WebView.FindListener>()
    every { mockWebView.setFindListener(capture(slot)) } just Runs

    callback.setWebView(mockWebView)
    callback.setText("first")
    callback.setText("second")

    slot.captured.onFindResultReceived(1, 2, true)

    assertThat(getPrivateTextView().text.toString()).isEqualTo("2/2")
  }

  @Test
  fun `setText handles null`() {
    callback.setText(null)
  }

  @Test
  fun `setText handles empty`() {
    callback.setText("")
  }

  @Test
  fun `setText handles non empty`() {
    callback.setText("search term")
  }

  @Test
  fun `setText sets cursor at end`() {
    callback.setText("hello")

    val editText = getPrivateEditText()
    assertThat(editText.selectionStart).isEqualTo(5)
    assertThat(editText.selectionEnd).isEqualTo(5)
  }

  @Test
  fun `onTextChanged triggers findAll`() {
    callback.setWebView(mockWebView)
    callback.setText("hello")

    callback.onTextChanged("hello", 0, 0, 5)

    verify { mockWebView.findAllAsync("hello") }
  }

  @Test
  fun `beforeTextChanged does nothing`() {
    callback.beforeTextChanged("", 0, 0, 0)
  }

  @Test
  fun `afterTextChanged does nothing`() {
    callback.afterTextChanged(SpannableStringBuilder(""))
  }

  @Test
  fun `onCreateActionMode returns true`() {
    assertThat(callback.onCreateActionMode(mockActionMode, mockMenu)).isTrue()
  }

  @Test
  fun `onCreateActionMode inflates menu`() {
    val inflater = mockk<MenuInflater>(relaxed = true)
    every { mockActionMode.menuInflater } returns inflater

    callback.onCreateActionMode(mockActionMode, mockMenu)

    verify { mockActionMode.customView = any() }
    verify { inflater.inflate(R.menu.menu_webview, mockMenu) }
  }

  @Test
  fun `onPrepareActionMode returns false`() {
    assertThat(callback.onPrepareActionMode(mockActionMode, mockMenu)).isFalse()
  }

  @Test
  fun `onDestroyActionMode resets state and clears matches`() {
    callback.setActive()
    callback.setWebView(mockWebView)

    callback.onDestroyActionMode(mockActionMode)

    assertThat(callback.isActive).isFalse()
    verify { mockWebView.clearMatches() }
  }

  @Test
  fun `onDestroyActionMode hides keyboard`() {
    val realContext = ApplicationProvider.getApplicationContext<Context>()
    val spyContext = spyk(realContext)
    val inputManager = mockk<InputMethodManager>(relaxed = true)

    every {
      spyContext.getSystemService(Context.INPUT_METHOD_SERVICE)
    } returns inputManager

    val callback = CompatFindActionModeCallback(spyContext)
    callback.setWebView(mockWebView)

    callback.onDestroyActionMode(mockActionMode)

    verify { inputManager.hideSoftInputFromWindow(any(), 0) }
  }

  @Test
  fun `finish clears matches and finishes action mode`() {
    callback.onCreateActionMode(mockActionMode, mockMenu)
    callback.setWebView(mockWebView)

    callback.finish()

    verify { mockActionMode.finish() }
    verify { mockWebView.clearMatches() }
  }

  @Test
  fun `finish without dependencies does not crash`() {
    callback.finish()
  }

  @Test
  fun `finish calls actionMode even if webView is null`() {
    callback.onCreateActionMode(mockActionMode, mockMenu)

    callback.finish()

    verify { mockActionMode.finish() }
  }

  @Test
  fun `find next menu action`() {
    callback.setWebView(mockWebView)
    val item = mockk<MenuItem>(relaxed = true)
    every { item.itemId } returns R.id.find_next

    assertThat(callback.onActionItemClicked(mockActionMode, item)).isTrue()
    verify { mockWebView.findNext(true) }
  }

  @Test
  fun `find previous menu action`() {
    callback.setWebView(mockWebView)
    val item = mockk<MenuItem>(relaxed = true)
    every { item.itemId } returns R.id.find_prev

    assertThat(callback.onActionItemClicked(mockActionMode, item)).isTrue()
    verify { mockWebView.findNext(false) }
  }

  @Test
  fun `unknown menu action returns false`() {
    callback.setWebView(mockWebView)
    val item = mockk<MenuItem>(relaxed = true)
    every { item.itemId } returns -1

    assertThat(callback.onActionItemClicked(mockActionMode, item)).isFalse()
  }

  @Test(expected = IllegalArgumentException::class)
  fun `menu action without webView throws`() {
    val item = mockk<MenuItem>(relaxed = true)
    every { item.itemId } returns R.id.find_next

    callback.onActionItemClicked(mockActionMode, item)
  }

  private fun getPrivateTextView(): TextView {
    val field = callback.javaClass.getDeclaredField("findResultsTextView")
    field.isAccessible = true
    return field.get(callback) as TextView
  }

  private fun getPrivateEditText(): EditText {
    val field = callback.javaClass.getDeclaredField("editText")
    field.isAccessible = true
    return field.get(callback) as EditText
  }

  private fun getPrivateWebView(): WebView {
    val field = callback.javaClass.getDeclaredField("webView")
    field.isAccessible = true
    return field.get(callback) as WebView
  }
}
