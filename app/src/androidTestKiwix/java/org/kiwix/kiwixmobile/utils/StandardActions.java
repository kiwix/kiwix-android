package org.kiwix.kiwixmobile.utils;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

/**
 * Created by mhutti1 on 27/04/17.
 */

public class StandardActions {

  public static void enterHelp() {
    openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());

    onView(withText("Help"))
        .perform(click());
  }


}
