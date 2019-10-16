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

package org.kiwix.kiwixmobile.main;

import android.content.Context;
import android.os.Build;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.view.ActionMode;
import java.lang.reflect.Method;
import org.kiwix.kiwixmobile.R;

public class CompatFindActionModeCallback
  implements ActionMode.Callback, TextWatcher, View.OnClickListener {

  public boolean isActive;

  private View customView;

  private EditText editText;

  private TextView findResultsTextView;

  private WebView webView;

  private InputMethodManager input;

  private ActionMode actionMode;

  CompatFindActionModeCallback(Context context) {
    customView = LayoutInflater.from(context).inflate(R.layout.webview_search, null);
    editText = customView.findViewById(R.id.edit);
    editText.setOnClickListener(this);
    findResultsTextView = customView.findViewById(R.id.find_results);
    input = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    isActive = false;
    setText("");
  }

  public void setActive() {
    isActive = true;
  }

  public void finish() {
    actionMode.finish();
    webView.clearMatches();
  }

  // Place text in the text field so it can be searched for.  Need to press
  // the find next or find previous button to find all of the matches.
  public void setText(String text) {
    editText.setText(text);
    Spannable span = editText.getText();
    int length = span.length();

    // Ideally, we would like to set the selection to the whole field,
    // but this brings up the Text selection CAB, which dismisses this
    // one.
    Selection.setSelection(span, length, length);

    // Necessary each time we set the text, so that this will watch
    // changes to it.
    span.setSpan(this, 0, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
  }

  // Set the WebView to search.  Must be non null, and set before calling startActionMode.
  public void setWebView(WebView webView) {
    if (null == webView) {
      throw new AssertionError(
        "WebView supplied to CompatFindActionModeCallback cannot be null");
    }
    this.webView = webView;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      findResultsTextView.setVisibility(View.VISIBLE);
      this.webView.setFindListener((activeMatchOrdinal, numberOfMatches, isDoneCounting) -> {
        String result;
        if (editText.getText().toString().isEmpty()) {
          result = "";
        } else if (numberOfMatches == 0) {
          result = "0/0";
        } else {
          result = (activeMatchOrdinal + 1) + "/" + numberOfMatches;
        }
        findResultsTextView.setText(result);
      });
    } else {
      findResultsTextView.setVisibility(View.GONE);
    }
  }

  // Move the highlight to the next match.
  // If true, find the next match further down in the document.
  // If false, find the previous match, up in the document.
  private void findNext(boolean next) {

    if (webView == null) {
      throw new AssertionError("No WebView for CompatFindActionModeCallback::findNext");
    }

    webView.findNext(next);
  }

  // Highlight all the instances of the string from mEditText in mWebView.
  public void findAll() {
    if (webView == null) {
      throw new AssertionError("No WebView for CompatFindActionModeCallback::findAll");
    }
    CharSequence find = editText.getText();
    if (find.length() == 0) {
      webView.clearMatches();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        webView.findAllAsync(null);
      } else {
        webView.findAll(null);
      }
    } else {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        webView.findAllAsync(find.toString());
      } else {
        webView.findAll(find.toString());
      }

      // Enable word highlighting with reflection
      try {
        for (Method ms : WebView.class.getDeclaredMethods()) {
          if (ms.getName().equals("setFindIsUp")) {
            ms.setAccessible(true);
            ms.invoke(webView, true);
            break;
          }
        }
      } catch (Exception ignored) {

      }
    }
  }

  // Show on screen keyboard
  public void showSoftInput() {
    //wait for any hidden show/hide processes to finish
    editText.postDelayed(() -> {

      editText.requestFocus();
      //show the keyboard
      input.showSoftInput(editText, 0);

    }, 100);

  }

  @Override
  public void onClick(View v) {
    findNext(true);
  }

  @Override
  public boolean onCreateActionMode(ActionMode mode, Menu menu) {
    mode.setCustomView(customView);
    mode.getMenuInflater().inflate(R.menu.menu_webview, menu);
    actionMode = mode;
    Editable edit = editText.getText();
    Selection.setSelection(edit, edit.length());
    editText.requestFocus();
    return true;
  }

  @Override
  public void onDestroyActionMode(ActionMode mode) {
    actionMode = null;
    isActive = false;
    webView.clearMatches();
    input.hideSoftInputFromWindow(webView.getWindowToken(), 0);
  }

  @Override
  public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
    return false;
  }

  @Override
  public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
    if (webView == null) {
      throw new AssertionError(
        "No WebView for CompatFindActionModeCallback::onActionItemClicked");
    }

    input.hideSoftInputFromWindow(webView.getWindowToken(), 0);

    switch (item.getItemId()) {
      case R.id.find_prev:
        findNext(false);
        break;
      case R.id.find_next:
        findNext(true);
        break;
      default:
        return false;
    }
    return true;
  }

  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    // Does nothing. Needed to implement a TextWatcher.
  }

  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {
    findAll();
  }

  @Override
  public void afterTextChanged(Editable s) {
    // Does nothing. Needed to implement a TextWatcher.
  }
}
