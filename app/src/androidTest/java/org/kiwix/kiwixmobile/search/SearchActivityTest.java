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

import android.Manifest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SearchActivityTest {

  @Rule
  public ActivityTestRule<SearchActivity> mActivityTestRule = new ActivityTestRule<>(
    SearchActivity.class);
  @Rule
  public GrantPermissionRule readPermissionRule =
    GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule =
    GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  @Test
  public void SearchActivitySimple() {

  }
}
