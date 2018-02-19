package org.kiwix.kiwixmobile.tests;


import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.testutils.TestUtils;
import org.kiwix.kiwixmobile.modules.main.SplashActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.kiwix.kiwixmobile.common.utils.StandardActions.enterHelp;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ContentTest {

  @Rule
  public ActivityTestRule<SplashActivity> mActivityTestRule = new ActivityTestRule<>(
      SplashActivity.class);

  @Test
  public void contentTest() {
    enterHelp();
    ViewInteraction appCompatButton = onView(
        allOf(withId(R.id.get_content_card), withText("Get Content")
        ));
    appCompatButton.perform(scrollTo(), click());

    TestUtils.allowPermissionsIfNeeded();

    ViewInteraction textView2 = onView(
        allOf(withId(R.id.action_search), withContentDescription("Search"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.toolbar),
                    2),
                0),
            isDisplayed()));
    textView2.check(matches(isDisplayed()));

    ViewInteraction textView3 = onView(
        allOf(withId(R.id.select_language), withContentDescription("Choose a language"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.toolbar),
                    2),
                1),
            isDisplayed()));
    textView3.check(matches(isDisplayed()));
  }

  private static Matcher<View> childAtPosition(
      final Matcher<View> parentMatcher, final int position) {

    return new TypeSafeMatcher<View>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("Child at position " + position + " in parent ");
        parentMatcher.describeTo(description);
      }

      @Override
      public boolean matchesSafely(View view) {
        ViewParent parent = view.getParent();
        return parent instanceof ViewGroup && parentMatcher.matches(parent)
            && view.equals(((ViewGroup) parent).getChildAt(position));
      }
    };
  }
}
