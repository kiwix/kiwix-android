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

package org.kiwix.kiwixmobile.core.main;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import org.jetbrains.annotations.Nullable;
import org.kiwix.kiwixmobile.core.R;
import org.kiwix.kiwixmobile.core.base.BaseActivity;

public abstract class CoreMainActivity extends BaseActivity implements WebViewProvider {
  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode,
    @androidx.annotation.Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    for (Fragment it : getSupportFragmentManager().getFragments()) {
      it.onActivityResult(requestCode, resultCode, data);
    }
  }

  @androidx.annotation.Nullable @Override public KiwixWebView getCurrentWebView() {
    for (Fragment frag : getSupportFragmentManager().getFragments()) {
      if (frag instanceof WebViewProvider) {
        return ((WebViewProvider) frag).getCurrentWebView();
      }
    }
    return null;
  }
}
