package org.kiwix.kiwixmobile.tests;


import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.utils.SplashActivity;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class BasicTest {

  @Rule
  public ActivityTestRule<SplashActivity> mActivityTestRule = new ActivityTestRule<>(
      SplashActivity.class);

  @Test
  public void basicTest() {
    onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());


    ViewInteraction textView7 = onView(
        allOf(withId(R.id.titleText), withText("Help"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.left_drawer_list),
                    0),
                0),
            isDisplayed()));
    textView7.check(matches(withText("Help")));

    ViewInteraction imageView = onView(
        allOf(childAtPosition(
            allOf(withId(R.id.new_tab_button),
                childAtPosition(
                    IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                    1)),
            0),
            isDisplayed()));
    imageView.check(matches(isDisplayed()));

    ViewInteraction imageView2 = onView(
        allOf(childAtPosition(
            allOf(withId(R.id.new_tab_button),
                childAtPosition(
                    IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                    1)),
            0),
            isDisplayed()));
    imageView2.check(matches(isDisplayed()));

    onView(withId(R.id.drawer_layout)).perform(DrawerActions.close());
  }

  @Test
  public void testRightDrawer() {
    onView(withId(R.id.drawer_layout)).perform(DrawerActions.open(Gravity.RIGHT));

    ViewInteraction textView = onView(
        allOf(withId(R.id.titleText), withText("No Content Headers Found"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.right_drawer_list),
                    0),
                0),
            isDisplayed()));
    textView.check(matches(withText("No Content Headers Found")));
    onView(withId(R.id.drawer_layout)).perform(DrawerActions.close(Gravity.RIGHT));
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
