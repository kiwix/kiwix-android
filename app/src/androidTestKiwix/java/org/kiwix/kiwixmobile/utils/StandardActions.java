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

import com.schibsted.spain.barista.interaction.BaristaMenuClickInteractions;
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.schibsted.spain.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton;
import static org.kiwix.kiwixmobile.testutils.TestUtils.withContent;

/**
 * Created by mhutti1 on 27/04/17.
 */

public class StandardActions {

  public static void enterHelp() {
    BaristaSleepInteractions.sleep(250);
    BaristaMenuClickInteractions.clickMenu("Help");
  }

  public static void enterSettings() {
    BaristaSleepInteractions.sleep(250);
    BaristaMenuClickInteractions.clickMenu("Settings");
  }

  public static void deleteZimIfExists(String zimName, Integer adapterId) {
    try {
      onData(withContent(zimName)).inAdapterView(withId(adapterId)).perform(longClick());
      clickDialogPositiveButton();
    } catch (RuntimeException e) {

    }
  }
}

