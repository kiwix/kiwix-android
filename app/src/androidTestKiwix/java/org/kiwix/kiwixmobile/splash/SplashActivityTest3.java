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
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SplashActivityTest3 {

  @Rule
  public ActivityTestRule<SplashActivity> mActivityTestRule =
      new ActivityTestRule<>(SplashActivity.class);

  @Test
  public void splashActivityTest3() {
    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction view = onView(
        allOf(withId(android.R.id.statusBarBackground),
            childAtPosition(
                IsInstanceOf.<View>instanceOf(android.widget.FrameLayout.class),
                1),
            isDisplayed()));
    view.check(matches(isDisplayed()));

    ViewInteraction textView = onView(
        allOf(withId(R.id.heading), withText("Welcome to the family"),
            childAtPosition(
                allOf(withId(R.id.root),
                    withParent(withId(R.id.view_pager))),
                1),
            isDisplayed()));
    textView.check(matches(withText("Welcome to the family")));

    ViewInteraction textView2 = onView(
        allOf(withId(R.id.subheading), withText("Human kind's knowledge, on your phone."),
            childAtPosition(
                allOf(withId(R.id.root),
                    withParent(withId(R.id.view_pager))),
                2),
            isDisplayed()));
    textView2.check(matches(withText("Human kind's knowledge, on your phone.")));

    ViewInteraction view3 = onView(
        allOf(withId(R.id.tab_indicator),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                2),
            isDisplayed()));
    view3.check(matches(isDisplayed()));

    ViewInteraction button = onView(
        allOf(withId(R.id.get_started),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                1),
            isDisplayed()));
    button.check(matches(isDisplayed()));

    ViewInteraction imageView = onView(
        allOf(withContentDescription("Kiwi"),
            childAtPosition(
                allOf(withId(R.id.root),
                    withParent(withId(R.id.view_pager))),
                0),
            isDisplayed()));
    imageView.check(matches(isDisplayed()));

    ViewInteraction customViewPager = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager.perform(swipeLeft());

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

    ViewInteraction customViewPager9 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager9.perform(swipeLeft());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(40000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction customViewPager10 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager10.perform(swipeRight());

    ViewInteraction customViewPager11 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager11.perform(swipeLeft());

    ViewInteraction customViewPager12 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager12.perform(swipeRight());

    ViewInteraction customViewPager13 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager13.perform(swipeLeft());

    ViewInteraction customViewPager14 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager14.perform(swipeRight());

    ViewInteraction customViewPager15 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager15.perform(swipeLeft());

    ViewInteraction customViewPager16 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager16.perform(swipeRight());

    ViewInteraction customViewPager17 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager17.perform(swipeLeft());

    ViewInteraction customViewPager18 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager18.perform(swipeRight());

    ViewInteraction customViewPager19 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager19.perform(swipeLeft());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(80000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction customViewPager20 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager20.perform(swipeRight());

    ViewInteraction customViewPager21 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager21.perform(swipeLeft());

    ViewInteraction customViewPager22 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager22.perform(swipeRight());

    ViewInteraction customViewPager23 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager23.perform(swipeLeft());

    ViewInteraction customViewPager24 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager24.perform(swipeRight());

    ViewInteraction customViewPager25 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager25.perform(swipeLeft());

    ViewInteraction customViewPager26 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager26.perform(swipeRight());

    ViewInteraction customViewPager27 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager27.perform(swipeLeft());

    ViewInteraction customViewPager28 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager28.perform(swipeRight());

    ViewInteraction view4 = onView(
        allOf(withId(android.R.id.statusBarBackground),
            childAtPosition(
                IsInstanceOf.<View>instanceOf(android.widget.FrameLayout.class),
                1),
            isDisplayed()));
    view4.check(matches(isDisplayed()));

    ViewInteraction textView3 = onView(
        allOf(withId(R.id.heading), withText("Save books offline"),
            childAtPosition(
                allOf(withId(R.id.root),
                    withParent(withId(R.id.view_pager))),
                0),
            isDisplayed()));
    textView3.check(matches(withText("Save books offline")));

    ViewInteraction textView4 = onView(
        allOf(withId(R.id.subheading), withText("Download books and read wherever you are."),
            childAtPosition(
                allOf(withId(R.id.root),
                    withParent(withId(R.id.view_pager))),
                1),
            isDisplayed()));
    textView4.check(matches(withText("Download books and read wherever you are.")));

    ViewInteraction button2 = onView(
        allOf(withId(R.id.get_started),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                1),
            isDisplayed()));
    button2.check(matches(isDisplayed()));

    ViewInteraction view5 = onView(
        allOf(withId(R.id.tab_indicator),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                2),
            isDisplayed()));
    view5.check(matches(isDisplayed()));

    ViewInteraction customViewPager29 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager29.perform(swipeLeft());

    ViewInteraction customViewPager30 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager30.perform(swipeRight());

    ViewInteraction customViewPager31 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager31.perform(swipeLeft());

    ViewInteraction customViewPager32 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager32.perform(swipeRight());

    ViewInteraction customViewPager33 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager33.perform(swipeLeft());

    ViewInteraction customViewPager34 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager34.perform(swipeRight());

    ViewInteraction customViewPager35 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager35.perform(swipeLeft());

    ViewInteraction customViewPager36 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager36.perform(swipeRight());

    ViewInteraction customViewPager37 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager37.perform(swipeLeft());

    ViewInteraction customViewPager38 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager38.perform(swipeRight());

    ViewInteraction customViewPager39 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager39.perform(swipeLeft());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(160000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction customViewPager40 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager40.perform(swipeRight());

    ViewInteraction customViewPager41 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager41.perform(swipeLeft());

    ViewInteraction customViewPager42 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager42.perform(swipeRight());

    ViewInteraction customViewPager43 = onView(
        allOf(withId(R.id.view_pager),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                0),
            isDisplayed()));
    customViewPager43.perform(swipeLeft());
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
