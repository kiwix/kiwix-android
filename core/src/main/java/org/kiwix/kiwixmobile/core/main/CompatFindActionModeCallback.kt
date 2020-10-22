/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.Selection
import android.text.Spannable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.view.ActionMode
import org.kiwix.kiwixmobile.core.R

class CompatFindActionModeCallback internal constructor(context: Context) :
  ActionMode.Callback, TextWatcher, View.OnClickListener {
  private val tag = "CompatFindActionMode"
  @JvmField var isActive: Boolean = false

  @SuppressLint("InflateParams")
  private val customView: View = LayoutInflater.from(context).inflate(R.layout.webview_search, null)
  private val editText: EditText = customView.findViewById(R.id.edit)
  private val findResultsTextView: TextView = customView.findViewById(R.id.find_results)
  private var webView: WebView? = null
  private val input: InputMethodManager =
    context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
  private var actionMode: ActionMode? = null

  init {
    editText.setOnClickListener(this)
    setText("")
  }

  fun setActive() {
    isActive = true
  }

  fun finish() {
    actionMode?.finish()
    webView?.clearMatches()
  }

  // Place text in the text field so it can be searched for.  Need to press
  // the find next or find previous button to find all of the matches.
  fun setText(text: String?) {
    editText.setText(text)
    val span: Spannable = editText.text
    val length = span.length

    // Ideally, we would like to set the selection to the whole field,
    // but this brings up the Text selection CAB, which dismisses this
    // one.
    Selection.setSelection(span, length, length)

    // Necessary each time we set the text, so that this will watch
    // changes to it.
    span.setSpan(this, 0, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
  }

  // Set the WebView to search.  Must be non null, and set before calling startActionMode.
  fun setWebView(webView: WebView?) {
    if (null == webView) {
      throw AssertionError(
        "WebView supplied to CompatFindActionModeCallback cannot be null"
      )
    }
    this.webView = webView
    findResultsTextView.visibility = View.VISIBLE
    this.webView?.setFindListener { activeMatchOrdinal: Int, numberOfMatches: Int, _: Boolean ->
      val result: String = when {
        editText.text.toString().isEmpty() -> {
          ""
        }
        numberOfMatches == 0 -> {
          "0/0"
        }
        else -> {
          (activeMatchOrdinal + 1).toString() + "/" + numberOfMatches
        }
      }
      findResultsTextView.text = result
    }
  }

  // Move the highlight to the next match.
  // If true, find the next match further down in the document.
  // If false, find the previous match, up in the document.
  private fun findNext(next: Boolean) {
    if (webView == null) {
      throw AssertionError("No WebView for CompatFindActionModeCallback::findNext")
    }
    webView?.findNext(next)
  }

  // Highlight all the instances of the string from mEditText in mWebView.
  fun findAll() {
    if (webView == null) {
      throw AssertionError("No WebView for CompatFindActionModeCallback::findAll")
    }
    val find: CharSequence? = editText.text
    if (find == null || find.isEmpty()) {
      webView?.clearMatches()
      webView?.findAllAsync("")
    } else {
      webView?.findAllAsync("$find")

      // Enable word highlighting with reflection
      try {
        for (ms in WebView::class.java.declaredMethods) {
          if (ms.name == "setFindIsUp") {
            ms.isAccessible = true
            ms.invoke(webView, true)
            break
          }
        }
      } catch (exception: Exception) {
        Log.e(tag, "Exception in findAll", exception)
      }
    }
  }

  // Show on screen keyboard
  fun showSoftInput() {
    // wait for any hidden show/hide processes to finish
    editText.postDelayed({
      editText.requestFocus()
      // show the keyboard
      input.showSoftInput(editText, 0)
    }, 100)
  }

  override fun onClick(v: View) {
    findNext(true)
  }

  override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
    mode.customView = customView
    mode.menuInflater.inflate(R.menu.menu_webview, menu)
    actionMode = mode
    val edit = editText.text
    Selection.setSelection(edit, edit.length)
    editText.requestFocus()
    return true
  }

  override fun onDestroyActionMode(mode: ActionMode) {
    actionMode = null
    isActive = false
    webView?.clearMatches()
    input.hideSoftInputFromWindow(webView?.windowToken, 0)
  }

  override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

  override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
    if (webView == null) {
      throw AssertionError(
        "No WebView for CompatFindActionModeCallback::onActionItemClicked"
      )
    }
    input.hideSoftInputFromWindow(webView?.windowToken, 0)
    when (item.itemId) {
      R.id.find_prev -> {
        findNext(false)
      }
      R.id.find_next -> {
        findNext(true)
      }
      else -> {
        return false
      }
    }
    return true
  }

  override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
    // Does nothing. Needed to implement a TextWatcher.
  }

  override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
    findAll()
  }

  override fun afterTextChanged(s: Editable) {
    // Does nothing. Needed to implement a TextWatcher.
  }
}
