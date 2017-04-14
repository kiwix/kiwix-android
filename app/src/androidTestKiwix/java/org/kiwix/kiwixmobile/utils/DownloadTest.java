package org.kiwix.kiwixmobile.utils;


import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.kiwix.kiwixmobile.testutils.TestUtils.withContent;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import javax.inject.Inject;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.R;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class DownloadTest {

  @Rule
  public ActivityTestRule<SplashActivity> mActivityTestRule = new ActivityTestRule<>(
      SplashActivity.class);

  @Test
  public void donwloadTest() {
    ViewInteraction appCompatButton = onView(
        allOf(withId(R.id.get_content_card), withText("Get Content")));
    appCompatButton.perform(scrollTo(), click());

    ViewInteraction appCompatTextView = onView(
        allOf(withText("Device"), isDisplayed()));
    appCompatTextView.perform(click());


    ViewInteraction appCompatTextView2 = onView(
        allOf(withText("Online"), isDisplayed()));
    appCompatTextView2.perform(click());

    ViewInteraction viewPager2 = onView(
        allOf(withId(R.id.container),
            withParent(allOf(withId(R.id.zim_manager_main_activity),
                withParent(withId(android.R.id.content)))),
            isDisplayed()));

    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    onData(withContent("ray_charles")).inAdapterView(withId(R.id.library_list)).perform(click());

    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction appCompatTextView3 = onView(
        allOf(withText("Device"), isDisplayed()));
    appCompatTextView3.perform(click());

    onData(withContent("ray_charles")).inAdapterView(withId(R.id.zimfilelist)).perform(click());

    openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());

    onView(withText("Get Content"))
        .perform(click());

    try {
      Thread.sleep(6000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    onData(withContent("ray_charles")).inAdapterView(withId(R.id.zimfilelist)).perform(longClick());

    onView(withId(android.R.id.button1)).perform(click());

  }
}
