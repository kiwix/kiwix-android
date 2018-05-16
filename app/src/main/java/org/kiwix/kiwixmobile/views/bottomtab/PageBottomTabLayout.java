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
package org.kiwix.kiwixmobile.views.bottomtab;

import android.content.Context;
import android.support.design.widget.TabLayout;
import android.util.AttributeSet;

import org.kiwix.kiwixmobile.R;

import butterknife.ButterKnife;

public class PageBottomTabLayout extends TabLayout {

  public PageBottomTabLayout(Context context) {
    this(context, null);
  }

  public PageBottomTabLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public PageBottomTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    inflate(getContext(), R.layout.page_bottom_tab_layout, this);
    ButterKnife.bind(this);
  }
}
