/*
 * Copyright 2013 Rashiq Ahmad <rashiq.z@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
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
import org.kiwix.kiwixmobile.core.R;

public class CompatFindActionModeCallback
  implements ActionMode.Callback, TextWatcher, View.OnClickListener {

  public boolean mIsActive;

  private View mCustomView;

  private EditText mEditText;

  private TextView mFindResultsTextView;

  private WebView mWebView;

  private InputMethodManager mInput;

  private ActionMode mActionMode;

  CompatFindActionModeCallback(Context context) {
    mCustomView = LayoutInflater.from(context).inflate(R.layout.webview_search, null);
    mEditText = mCustomView.findViewById(R.id.edit);
    mEditText.setOnClickListener(this);
    mFindResultsTextView = mCustomView.findViewById(R.id.find_results);
    mInput = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    mIsActive = false;
    setText("");
  }

  public void setActive() {
    mIsActive = true;
  }

  public void finish() {
    mActionMode.finish();
    mWebView.clearMatches();
  }

  // Place text in the text field so it can be searched for.  Need to press
  // the find next or find previous button to find all of the matches.
  public void setText(String text) {
    mEditText.setText(text);
    Spannable span = mEditText.getText();
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
    mWebView = webView;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      mFindResultsTextView.setVisibility(View.VISIBLE);
      mWebView.setFindListener((activeMatchOrdinal, numberOfMatches, isDoneCounting) -> {
        String result;
        if (mEditText.getText().toString().isEmpty()) {
          result = "";
        } else if (numberOfMatches == 0) {
          result = "0/0";
        } else {
          result = (activeMatchOrdinal + 1) + "/" + numberOfMatches;
        }
        mFindResultsTextView.setText(result);
      });
    } else {
      mFindResultsTextView.setVisibility(View.GONE);
    }
  }

  // Move the highlight to the next match.
  // If true, find the next match further down in the document.
  // If false, find the previous match, up in the document.
  private void findNext(boolean next) {

    if (mWebView == null) {
      throw new AssertionError("No WebView for CompatFindActionModeCallback::findNext");
    }

    mWebView.findNext(next);
  }

  // Highlight all the instances of the string from mEditText in mWebView.
  public void findAll() {
    if (mWebView == null) {
      throw new AssertionError("No WebView for CompatFindActionModeCallback::findAll");
    }
    CharSequence find = mEditText.getText();
    if (find.length() == 0) {
      mWebView.clearMatches();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        mWebView.findAllAsync(null);
      } else {
        mWebView.findAll(null);
      }
    } else {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        mWebView.findAllAsync(find.toString());
      } else {
        mWebView.findAll(find.toString());
      }

      // Enable word highlighting with reflection
      try {
        for (Method ms : WebView.class.getDeclaredMethods()) {
          if (ms.getName().equals("setFindIsUp")) {
            ms.setAccessible(true);
            ms.invoke(mWebView, true);
            break;
          }
        }
      } catch (Exception ignored) {

      }
    }
  }

  // Show on screen keyboard
  public void showSoftInput() {
    mEditText.requestFocus();
    mEditText.setFocusable(true);
    mEditText.setFocusableInTouchMode(true);
    mEditText.requestFocusFromTouch();

    if (mEditText.requestFocus()) {
      mInput.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
    }
  }

  @Override
  public void onClick(View v) {
    findNext(true);
  }

  @Override
  public boolean onCreateActionMode(ActionMode mode, Menu menu) {
    mode.setCustomView(mCustomView);
    mode.getMenuInflater().inflate(R.menu.menu_webview, menu);
    mActionMode = mode;
    Editable edit = mEditText.getText();
    Selection.setSelection(edit, edit.length());
    mEditText.requestFocus();
    return true;
  }

  @Override
  public void onDestroyActionMode(ActionMode mode) {
    mActionMode = null;
    mIsActive = false;
    mWebView.clearMatches();
    mInput.hideSoftInputFromWindow(mWebView.getWindowToken(), 0);
  }

  @Override
  public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
    return false;
  }

  @Override
  public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
    if (mWebView == null) {
      throw new AssertionError(
        "No WebView for CompatFindActionModeCallback::onActionItemClicked");
    }

    mInput.hideSoftInputFromWindow(mWebView.getWindowToken(), 0);

    int itemId = item.getItemId();
    if (itemId == R.id.find_prev) {
      findNext(false);
    } else if (itemId == R.id.find_next) {
      findNext(true);
    } else {
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
