/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.base;

import android.content.res.Resources;
import android.os.Bundle;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dagger.android.AndroidInjection;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.utils.LanguageUtils;
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil;

public abstract class BaseActivity extends AppCompatActivity {

  @Inject
  protected SharedPreferenceUtil sharedPreferenceUtil;

  private Unbinder unbinder;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    injection();
    super.onCreate(savedInstanceState);
    LanguageUtils.handleLocaleChange(this, sharedPreferenceUtil);
  }

  protected void injection() {
    AndroidInjection.inject(this);
  }

  @Override
  public Resources.Theme getTheme() {
    Resources.Theme theme = super.getTheme();
    if (sharedPreferenceUtil != null && sharedPreferenceUtil.nightMode()) {
      setTheme(R.style.AppTheme_Night);
    } else {
      theme.applyStyle(R.style.StatusBarTheme, true);
    }
    return theme;
  }

  @Override
  public void setContentView(@LayoutRes int layoutResID) {
    super.setContentView(layoutResID);
    unbinder = ButterKnife.bind(this);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (unbinder != null) {
      unbinder.unbind();
    }
  }
}
