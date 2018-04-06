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

}

