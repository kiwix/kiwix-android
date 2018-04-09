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

import org.kiwix.kiwixmobile.R;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openContextualActionModeOverflowMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.kiwix.kiwixmobile.testutils.TestUtils.withContent;

/**
 * Created by mhutti1 on 27/04/17.
 */

public class StandardActions {

  public static void enterHelp() {
    openContextualActionModeOverflowMenu();

    onView(withText("Help"))
            .perform(click());
  }

  public static void deleteZimIfExists(String zimName, Integer adapterId) {
    try { // Delete ray_charles ZIM if it exists
      onData(withContent(zimName)).inAdapterView(withId(adapterId)).perform(longClick());
      onView(withId(android.R.id.button1)).perform(click());
    } catch (RuntimeException e) {
      // Otherwise continue with download
    }
  }
  
  public static void enterSettings() {
    openContextualActionModeOverflowMenu();

    onView(withText("Settings"))
        .perform(click());
  }
  
}

