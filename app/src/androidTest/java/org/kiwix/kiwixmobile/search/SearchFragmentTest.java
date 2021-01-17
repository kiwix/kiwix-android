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

package org.kiwix.kiwixmobile.search;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.statement.UiThreadStatement;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.BaseActivityTest;
import org.kiwix.kiwixmobile.R;

import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SearchFragmentTest extends BaseActivityTest {
  @Before
  public void setUp() throws Throwable {
    UiThreadStatement.runOnUiThread(
      () -> getActivityRule().getActivity().navigate(R.id.searchFragment));
  }

  @Test
  public void SearchFragmentSimple() {
    assertDisplayed(R.string.menu_search_in_text);
  }
}
