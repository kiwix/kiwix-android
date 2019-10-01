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

package org.kiwix.kiwixmobile.core.settings;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import org.kiwix.kiwixmobile.core.R;
import org.kiwix.kiwixmobile.core.base.BaseActivity;

import static org.kiwix.kiwixmobile.core.utils.Constants.EXTRA_WEBVIEWS_LIST;
import static org.kiwix.kiwixmobile.core.utils.Constants.RESULT_HISTORY_CLEARED;

public class KiwixSettingsActivity extends BaseActivity {

  public static boolean allHistoryCleared = false;

  @Override
  public void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings);

    allHistoryCleared = false;

    getFragmentManager()
      .beginTransaction().
      replace(R.id.content_frame, new PrefsFragment())
      .commit();

    setUpToolbar();
  }

  @Override
  public void onBackPressed() {
    getWindow().setWindowAnimations(0);
    if (allHistoryCleared) {
      Intent data = new Intent();
      data.putExtra(EXTRA_WEBVIEWS_LIST, allHistoryCleared);
      setResult(RESULT_HISTORY_CLEARED, data);
    }
    super.onBackPressed();
  }

  private void setUpToolbar() {
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setTitle(getString(R.string.menu_settings));
    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    toolbar.setNavigationOnClickListener(v -> onBackPressed());
  }
}
