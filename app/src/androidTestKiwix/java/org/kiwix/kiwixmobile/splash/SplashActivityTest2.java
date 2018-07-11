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

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
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
      Thread.sleep(20000);
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
      Thread.sleep(40000);
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
    viewPager.perform(swipeLeft());

    ViewInteraction actionMenuItemView = onView(
        allOf(withId(R.id.select_language), withContentDescription("Choose a language"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.toolbar),
                    2),
                1),
            isDisplayed()));
    actionMenuItemView.perform(click());

    ViewInteraction textView = onView(
        allOf(withId(R.id.select_language), withContentDescription("Choose a language"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.toolbar),
                    2),
                1),
            isDisplayed()));
    textView.check(matches(isDisplayed()));

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(80000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction textView2 = onView(
        allOf(withId(R.id.divider_text), withText("Selected languages:"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.library_list),
                    0),
                0),
            isDisplayed()));
    textView2.check(matches(withText("Selected languages:")));

    ViewInteraction textView3 = onView(
        allOf(withText("Library"),
            childAtPosition(
                allOf(withId(R.id.toolbar),
                    childAtPosition(
                        withId(R.id.toolbar_layout),
                        0)),
                1),
            isDisplayed()));
    textView3.check(matches(withText("Library")));

    ViewInteraction textView4 = onView(
        allOf(withId(R.id.action_search), withContentDescription("Search"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.toolbar),
                    2),
                0),
            isDisplayed()));
    textView4.check(matches(isDisplayed()));

    ViewInteraction imageButton = onView(
        allOf(withContentDescription("Navigate up"),
            childAtPosition(
                allOf(withId(R.id.toolbar),
                    childAtPosition(
                        withId(R.id.toolbar_layout),
                        0)),
                0),
            isDisplayed()));
    imageButton.check(matches(isDisplayed()));

    ViewInteraction toolbar = onView(
        allOf(withId(R.id.toolbar),
            childAtPosition(
                allOf(withId(R.id.toolbar_layout),
                    childAtPosition(
                        withId(R.id.appbar),
                        0)),
                0),
            isDisplayed()));
    toolbar.perform(click());

    ViewInteraction searchAutoComplete = onView(
        allOf(withId(R.id.search_src_text),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        1)),
                0),
            isDisplayed()));
    searchAutoComplete.perform(click());

    ViewInteraction editText = onView(
        allOf(withId(R.id.search_src_text), withText("Search…"),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        0)),
                0),
            isDisplayed()));
    editText.check(matches(withText("Search…")));

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(160000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction appCompatImageButton = onView(
        allOf(withContentDescription("Collapse"),
            childAtPosition(
                allOf(withId(R.id.toolbar),
                    childAtPosition(
                        withId(R.id.toolbar_layout),
                        0)),
                1),
            isDisplayed()));
    appCompatImageButton.perform(click());

    ViewInteraction actionMenuItemView2 = onView(
        allOf(withId(R.id.select_language), withContentDescription("Choose a language"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.toolbar),
                    0),
                1),
            isDisplayed()));
    actionMenuItemView2.perform(click());
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
