/*
 * Copyright 2013  Elad Keyshawn <elad.keyshawn@gmail.com>
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


package org.kiwix.kiwixmobile.utils;

import android.content.Context;
import android.content.SharedPreferences;


public class RateAppCounter {

  private String MASTER_NAME = "visitCounter";
  private String NOTHANKS_CLICKED = "clickedNoThanks";
  private SharedPreferences visitCounter;

  public RateAppCounter(Context context) {
    visitCounter = context.getSharedPreferences(MASTER_NAME, 0);
    visitCounter = context.getSharedPreferences(NOTHANKS_CLICKED, 0);
  }

  public boolean getNoThanksState() {
    return visitCounter.getBoolean(NOTHANKS_CLICKED, false);
  }

  public void setNoThanksState(boolean val) {
    SharedPreferences.Editor CounterEditor = visitCounter.edit();
    CounterEditor.putBoolean(NOTHANKS_CLICKED, val);
    CounterEditor.commit();
  }

  public SharedPreferences.Editor getEditor() {
    return visitCounter.edit();
  }

  public int getCount() {
    return visitCounter.getInt("count", 0);
  }

  public void setCount(int count) {
    SharedPreferences.Editor CounterEditor = visitCounter.edit();
    CounterEditor.putInt("count", count);
    CounterEditor.commit();

  }
}
