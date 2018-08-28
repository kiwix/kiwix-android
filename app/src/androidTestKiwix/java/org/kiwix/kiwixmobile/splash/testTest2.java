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
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class testTest2 {

  @Rule
  public ActivityTestRule<SplashActivity> mActivityTestRule =
      new ActivityTestRule<>(SplashActivity.class);

  @Test
  public void testTest2() {
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
      Thread.sleep(40000);
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

    ViewInteraction textView = onView(
        allOf(withId(R.id.item_language_name), withText("English"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.recycler_view),
                    1),
                1),
            isDisplayed()));
    textView.check(matches(withText("English")));

    ViewInteraction checkBox = onView(
        allOf(withId(R.id.item_language_checkbox),
            childAtPosition(
                childAtPosition(
                    withId(R.id.recycler_view),
                    1),
                0),
            isDisplayed()));
    checkBox.check(matches(isDisplayed()));

    ViewInteraction textView2 = onView(
        allOf(withId(R.id.item_language_name), withText("Achinese"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.recycler_view),
                    4),
                1),
            isDisplayed()));
    textView2.check(matches(withText("Achinese")));

    ViewInteraction textView3 = onView(
        allOf(withId(R.id.item_language_name), withText("Abkhazian"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.recycler_view),
                    3),
                1),
            isDisplayed()));
    textView3.check(matches(withText("Abkhazian")));

    ViewInteraction checkBox2 = onView(
        allOf(withId(R.id.item_language_checkbox),
            childAtPosition(
                childAtPosition(
                    withId(R.id.recycler_view),
                    3),
                0),
            isDisplayed()));
    checkBox2.check(matches(isDisplayed()));

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(80000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction appCompatCheckBox = onView(
        allOf(withId(R.id.item_language_checkbox),
            childAtPosition(
                childAtPosition(
                    withId(R.id.recycler_view),
                    5),
                0),
            isDisplayed()));
    appCompatCheckBox.perform(click());

    ViewInteraction checkBox3 = onView(
        allOf(withId(R.id.item_language_checkbox),
            childAtPosition(
                childAtPosition(
                    withId(R.id.recycler_view),
                    1),
                0),
            isDisplayed()));
    checkBox3.check(matches(isDisplayed()));

    ViewInteraction actionMenuItemView2 = onView(
        allOf(withId(R.id.menu_language_search), withContentDescription("Search"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.toolbar),
                    2),
                0),
            isDisplayed()));
    actionMenuItemView2.perform(click());

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

    ViewInteraction searchAutoComplete2 = onView(
        allOf(withId(R.id.search_src_text),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        1)),
                0),
            isDisplayed()));
    searchAutoComplete2.perform(replaceText("en"), closeSoftKeyboard());

    ViewInteraction viewGroup = onView(
        allOf(childAtPosition(
            allOf(withId(R.id.recycler_view),
                childAtPosition(
                    IsInstanceOf.<View>instanceOf(android.view.ViewGroup.class),
                    1)),
            1),
            isDisplayed()));
    viewGroup.check(matches(isDisplayed()));

    ViewInteraction viewGroup2 = onView(
        allOf(childAtPosition(
            allOf(withId(R.id.recycler_view),
                childAtPosition(
                    IsInstanceOf.<View>instanceOf(android.view.ViewGroup.class),
                    1)),
            1),
            isDisplayed()));
    viewGroup2.check(matches(isDisplayed()));

    ViewInteraction textView4 = onView(
        allOf(withId(R.id.item_language_name), withText("English"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.recycler_view),
                    1),
                1),
            isDisplayed()));
    textView4.check(matches(withText("English")));

    ViewInteraction checkBox4 = onView(
        allOf(withId(R.id.item_language_checkbox),
            childAtPosition(
                childAtPosition(
                    withId(R.id.recycler_view),
                    1),
                0),
            isDisplayed()));
    checkBox4.check(matches(isDisplayed()));

    ViewInteraction textView5 = onView(
        allOf(withId(R.id.item_language_name), withText("Bengali"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.recycler_view),
                    4),
                1),
            isDisplayed()));
    textView5.check(matches(withText("Bengali")));

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(160000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction searchAutoComplete3 = onView(
        allOf(withId(R.id.search_src_text), withText("en"),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        1)),
                0),
            isDisplayed()));
    searchAutoComplete3.perform(click());

    ViewInteraction searchAutoComplete4 = onView(
        allOf(withId(R.id.search_src_text), withText("en"),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        1)),
                0),
            isDisplayed()));
    searchAutoComplete4.perform(replaceText("enga"));

    ViewInteraction searchAutoComplete5 = onView(
        allOf(withId(R.id.search_src_text), withText("enga"),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        1)),
                0),
            isDisplayed()));
    searchAutoComplete5.perform(closeSoftKeyboard());

    ViewInteraction textView6 = onView(
        allOf(withId(R.id.item_language_name), withText("Bengali"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.recycler_view),
                    2),
                1),
            isDisplayed()));
    textView6.check(matches(withText("Bengali")));

    ViewInteraction checkBox5 = onView(
        allOf(withId(R.id.item_language_checkbox),
            childAtPosition(
                childAtPosition(
                    withId(R.id.recycler_view),
                    2),
                0),
            isDisplayed()));
    checkBox5.check(matches(isDisplayed()));
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
