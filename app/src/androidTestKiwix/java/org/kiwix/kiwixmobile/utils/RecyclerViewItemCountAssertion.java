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

package org.kiwix.kiwixmobile.utils;

import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;
import org.hamcrest.Matcher;

import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static org.hamcrest.CoreMatchers.is;

public class RecyclerViewItemCountAssertion implements ViewAssertion {
  private final Matcher<Integer> matcher;

  private RecyclerViewItemCountAssertion(Matcher<Integer> matcher) {
    this.matcher = matcher;
  }

  public static RecyclerViewItemCountAssertion withItemCount(int expectedCount) {
    return withItemCount(is(expectedCount));
  }

  public static RecyclerViewItemCountAssertion withItemCount(Matcher<Integer> matcher) {
    return new RecyclerViewItemCountAssertion(matcher);
  }

  @Override
  public void check(View view, NoMatchingViewException noViewFoundException) {
    if (noViewFoundException != null) {
      throw noViewFoundException;
    }

    RecyclerView recyclerView = (RecyclerView) view;
    RecyclerView.Adapter adapter = recyclerView.getAdapter();
    assertThat(adapter.getItemCount(), matcher);
  }
}
