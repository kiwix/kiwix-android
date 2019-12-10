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

import android.content.Context;
import android.content.SharedPreferences;

public class RateAppCounter {

  private static final String MASTER_NAME = "visitCounter";
  private static final String NOTHANKS_CLICKED = "clickedNoThanks";

  private SharedPreferences visitCounter;

  RateAppCounter(Context context) {
    visitCounter = context.getSharedPreferences(MASTER_NAME, 0);
    visitCounter = context.getSharedPreferences(NOTHANKS_CLICKED, 0);
  }

  public boolean getNoThanksState() {
    return visitCounter.getBoolean(NOTHANKS_CLICKED, false);
  }

  public void setNoThanksState(boolean val) {
    SharedPreferences.Editor CounterEditor = visitCounter.edit();
    CounterEditor.putBoolean(NOTHANKS_CLICKED, val);
    CounterEditor.apply();
  }

  public int getCount() {
    return visitCounter.getInt("count", 0);
  }

  public void setCount(int count) {
    SharedPreferences.Editor CounterEditor = visitCounter.edit();
    CounterEditor.putInt("count", count);
    CounterEditor.apply();
  }
}
