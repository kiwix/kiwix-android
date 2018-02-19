package org.kiwix.kiwixmobile.common.utils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openContextualActionModeOverflowMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

/**
 * Created by mhutti1 on 27/04/17.
 */

public class StandardActions {

  public static void enterHelp() {
    openContextualActionModeOverflowMenu();

    onView(withText("Help"))
        .perform(click());
  }


}
