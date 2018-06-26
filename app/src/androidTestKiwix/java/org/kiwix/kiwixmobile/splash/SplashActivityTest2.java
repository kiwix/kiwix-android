package org.kiwix.kiwixmobile.splash;

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
import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SplashActivityTest2 {

  @Rule
  public ActivityTestRule<SplashActivity> mActivityTestRule =
      new ActivityTestRule<>(SplashActivity.class);

  @Test
  public void splashActivityTest2() {
    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction customViewPager = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager.perform(swipeLeft());

    ViewInteraction view = onView(
        allOf(withId(android.R.id.statusBarBackground),
            childAtPosition(
                IsInstanceOf.<View>instanceOf(android.widget.FrameLayout.class),
                1),
            isDisplayed()));
    view.check(matches(isDisplayed()));

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction customViewPager2 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager2.perform(swipeRight());

    ViewInteraction customViewPager3 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager3.perform(swipeLeft());

    ViewInteraction customViewPager4 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager4.perform(swipeRight());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(20000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction customViewPager5 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager5.perform(swipeLeft());

    ViewInteraction customViewPager6 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager6.perform(swipeRight());

    ViewInteraction customViewPager7 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager7.perform(swipeLeft());

    ViewInteraction customViewPager8 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager8.perform(swipeRight());
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
