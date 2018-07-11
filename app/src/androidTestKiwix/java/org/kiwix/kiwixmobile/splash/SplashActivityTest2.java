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

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

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

    openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());

    ViewInteraction appCompatTextView = onView(
        allOf(withId(R.id.title), withText("Get Content"),
            childAtPosition(
                childAtPosition(
                    withClassName(is("android.support.v7.view.menu.ListMenuItemView")),
                    0),
                0),
            isDisplayed()));
    appCompatTextView.perform(click());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(20000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction viewPager = onView(
        allOf(withId(R.id.container),
            childAtPosition(
                allOf(withId(R.id.zim_manager_main_activity),
                    childAtPosition(
                        withId(android.R.id.content),
                        0)),
                1),
            isDisplayed()));
    viewPager.perform(swipeRight());

    ViewInteraction viewPager2 = onView(
        allOf(withId(R.id.container),
            childAtPosition(
                allOf(withId(R.id.zim_manager_main_activity),
                    childAtPosition(
                        withId(android.R.id.content),
                        0)),
                1),
            isDisplayed()));
    viewPager2.perform(swipeLeft());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(40000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction linearLayout = onView(
        allOf(childAtPosition(
            allOf(withId(R.id.library_list),
                childAtPosition(
                    withId(R.id.library_swiperefresh),
                    0)),
            2),
            isDisplayed()));
    linearLayout.check(matches(isDisplayed()));

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
      Thread.sleep(80000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction viewGroup = onView(
        allOf(childAtPosition(
            allOf(withId(R.id.activity_language_recycler_view),
                childAtPosition(
                    IsInstanceOf.<View>instanceOf(android.view.ViewGroup.class),
                    1)),
            3),
            isDisplayed()));
    viewGroup.check(matches(isDisplayed()));

    ViewInteraction checkBox = onView(
        allOf(withId(R.id.item_language_checkbox),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    3),
                0),
            isDisplayed()));
    checkBox.check(matches(isDisplayed()));
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
