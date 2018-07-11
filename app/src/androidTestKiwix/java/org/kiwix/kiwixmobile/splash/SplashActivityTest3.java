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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
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

    ViewInteraction appCompatButton = onView(
        allOf(withId(R.id.get_started), withText("Get started"),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                1),
            isDisplayed()));
    appCompatButton.perform(click());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction appCompatButton2 = onView(
        allOf(withId(R.id.content_main_card_download_button), withText("Download books"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.content_main_card),
                    0),
                1),
            isDisplayed()));
    appCompatButton2.perform(click());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(20000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction actionMenuItemView = onView(
        allOf(withId(R.id.select_language), withContentDescription("Choose a language"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.toolbar),
                    2),
                1),
            isDisplayed()));
    actionMenuItemView.perform(click());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(40000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction imageButton = onView(
        allOf(withContentDescription("Navigate up"),
            childAtPosition(
                allOf(withId(R.id.activity_language_toolbar),
                    childAtPosition(
                        withId(R.id.activity_language_appbar),
                        0)),
                0),
            isDisplayed()));
    imageButton.check(matches(isDisplayed()));

    ViewInteraction textView = onView(
        allOf(withText("Select languages"),
            childAtPosition(
                allOf(withId(R.id.activity_language_toolbar),
                    childAtPosition(
                        withId(R.id.activity_language_appbar),
                        0)),
                1),
            isDisplayed()));
    textView.check(matches(withText("Select languages")));

    ViewInteraction textView2 = onView(
        allOf(withId(R.id.menu_language_save), withContentDescription("Save languages"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_toolbar),
                    2),
                1),
            isDisplayed()));
    textView2.check(matches(isDisplayed()));

    ViewInteraction textView3 = onView(
        allOf(withId(R.id.menu_language_search), withContentDescription("Search"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_toolbar),
                    2),
                0),
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
