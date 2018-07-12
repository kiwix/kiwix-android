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
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.replaceText;
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
public class finalrunthrough {

  @Rule
  public ActivityTestRule<SplashActivity> mActivityTestRule =
      new ActivityTestRule<>(SplashActivity.class);

  @Test
  public void finalrunthrough() {
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

    ViewInteraction appCompatButton = onView(
        allOf(withId(R.id.get_started), withText("Get started"),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                1),
            isDisplayed()));
    appCompatButton.perform(click());

    ViewInteraction appCompatButton2 = onView(
        allOf(withId(R.id.get_started), withText("Get started"),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                1),
            isDisplayed()));
    appCompatButton2.perform(click());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

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

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(20000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

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

    ViewInteraction linearLayout = onView(
        allOf(childAtPosition(
            allOf(withId(R.id.library_list),
                childAtPosition(
                    withId(R.id.library_swiperefresh),
                    0)),
            1),
            isDisplayed()));
    linearLayout.check(matches(isDisplayed()));

    ViewInteraction textView = onView(
        allOf(withId(R.id.select_language), withContentDescription("Choose a language"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.toolbar),
                    2),
                1),
            isDisplayed()));
    textView.check(matches(isDisplayed()));

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
        allOf(withText("Library"),
            childAtPosition(
                allOf(withId(R.id.toolbar),
                    childAtPosition(
                        withId(R.id.toolbar_layout),
                        0)),
                1),
            isDisplayed()));
    textView3.check(matches(withText("Library")));

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

    ViewInteraction imageButton2 = onView(
        allOf(withContentDescription("Navigate up"),
            childAtPosition(
                allOf(withId(R.id.toolbar),
                    childAtPosition(
                        withId(R.id.toolbar_layout),
                        0)),
                0),
            isDisplayed()));
    imageButton2.check(matches(isDisplayed()));

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(160000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction textView4 = onView(
        allOf(withId(R.id.title), withText("Wikipedia"),
            childAtPosition(
                childAtPosition(
                    IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                    1),
                0),
            isDisplayed()));
    textView4.check(matches(withText("Wikipedia")));

    ViewInteraction textView5 = onView(
        allOf(withId(R.id.divider_text), withText("Selected languages:"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.library_list),
                    0),
                0),
            isDisplayed()));
    textView5.check(matches(withText("Selected languages:")));

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(160000);
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

    ViewInteraction imageButton3 = onView(
        allOf(withContentDescription("Navigate up"),
            childAtPosition(
                allOf(withId(R.id.activity_language_toolbar),
                    childAtPosition(
                        withId(R.id.activity_language_appbar),
                        0)),
                0),
            isDisplayed()));
    imageButton3.check(matches(isDisplayed()));

    ViewInteraction textView6 = onView(
        allOf(withId(R.id.menu_language_save), withContentDescription("Save languages"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_toolbar),
                    2),
                1),
            isDisplayed()));
    textView6.check(matches(isDisplayed()));

    ViewInteraction textView7 = onView(
        allOf(withId(R.id.menu_language_search), withContentDescription("Search"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_toolbar),
                    2),
                0),
            isDisplayed()));
    textView7.check(matches(isDisplayed()));

    ViewInteraction textView8 = onView(
        allOf(withText("Select languages"),
            childAtPosition(
                allOf(withId(R.id.activity_language_toolbar),
                    childAtPosition(
                        withId(R.id.activity_language_appbar),
                        0)),
                1),
            isDisplayed()));
    textView8.check(matches(withText("Select languages")));

    ViewInteraction textView9 = onView(
        allOf(withId(R.id.item_language_name), withText("Abkhazian"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    0),
                1),
            isDisplayed()));
    textView9.check(matches(withText("Abkhazian")));

    ViewInteraction actionMenuItemView2 = onView(
        allOf(withId(R.id.menu_language_search), withContentDescription("Search"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_toolbar),
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
    searchAutoComplete.perform(replaceText("englissh"), closeSoftKeyboard());

    ViewInteraction searchAutoComplete2 = onView(
        allOf(withId(R.id.search_src_text), withText("englissh"),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        1)),
                0),
            isDisplayed()));
    searchAutoComplete2.perform(pressImeActionButton());

    ViewInteraction searchAutoComplete3 = onView(
        allOf(withId(R.id.search_src_text), withText("englissh"),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        1)),
                0),
            isDisplayed()));
    searchAutoComplete3.perform(click());

    ViewInteraction searchAutoComplete4 = onView(
        allOf(withId(R.id.search_src_text), withText("englissh"),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        1)),
                0),
            isDisplayed()));
    searchAutoComplete4.perform(click());

    ViewInteraction searchAutoComplete5 = onView(
        allOf(withId(R.id.search_src_text), withText("englissh"),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        1)),
                0),
            isDisplayed()));
    searchAutoComplete5.perform(replaceText("english"));

    ViewInteraction searchAutoComplete6 = onView(
        allOf(withId(R.id.search_src_text), withText("english"),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        1)),
                0),
            isDisplayed()));
    searchAutoComplete6.perform(closeSoftKeyboard());

    ViewInteraction textView10 = onView(
        allOf(withId(R.id.item_language_name), withText("English"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    0),
                1),
            isDisplayed()));
    textView10.check(matches(withText("English")));

    ViewInteraction textView11 = onView(
        allOf(withId(R.id.item_language_localized_name), withText("English"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    0),
                2),
            isDisplayed()));
    textView11.check(matches(withText("English")));

    ViewInteraction checkBox = onView(
        allOf(withId(R.id.item_language_checkbox),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    0),
                0),
            isDisplayed()));
    checkBox.check(matches(isDisplayed()));

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(320000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction appCompatCheckBox = onView(
        allOf(withId(R.id.item_language_checkbox),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    0),
                0),
            isDisplayed()));
    appCompatCheckBox.perform(click());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(320000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction appCompatImageView = onView(
        allOf(withId(R.id.search_close_btn), withContentDescription("Clear query"),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        1)),
                1),
            isDisplayed()));
    appCompatImageView.perform(click());

    ViewInteraction textView12 = onView(
        allOf(withId(R.id.item_language_name), withText("English"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    2),
                1),
            isDisplayed()));
    textView12.check(matches(withText("English")));

    ViewInteraction checkBox2 = onView(
        allOf(withId(R.id.item_language_checkbox),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    2),
                0),
            isDisplayed()));
    checkBox2.check(matches(isDisplayed()));

    ViewInteraction textView13 = onView(
        allOf(withId(R.id.item_language_localized_name), withText("English"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    2),
                2),
            isDisplayed()));
    textView13.check(matches(withText("English")));

    ViewInteraction actionMenuItemView3 = onView(
        allOf(withId(R.id.menu_language_save), withContentDescription("Save languages"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_toolbar),
                    0),
                1),
            isDisplayed()));
    actionMenuItemView3.perform(click());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(40000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction linearLayout2 = onView(
        allOf(childAtPosition(
            allOf(withId(R.id.library_list),
                childAtPosition(
                    withId(R.id.library_swiperefresh),
                    1)),
            2),
            isDisplayed()));
    linearLayout2.check(matches(isDisplayed()));

    ViewInteraction actionMenuItemView4 = onView(
        allOf(withId(R.id.select_language), withContentDescription("Choose a language"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.toolbar),
                    2),
                1),
            isDisplayed()));
    actionMenuItemView4.perform(click());

    ViewInteraction actionMenuItemView5 = onView(
        allOf(withId(R.id.menu_language_search), withContentDescription("Search"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_toolbar),
                    2),
                0),
            isDisplayed()));
    actionMenuItemView5.perform(click());

    ViewInteraction searchAutoComplete7 = onView(
        allOf(withId(R.id.search_src_text),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        1)),
                0),
            isDisplayed()));
    searchAutoComplete7.perform(replaceText("hindi"), closeSoftKeyboard());

    ViewInteraction searchAutoComplete8 = onView(
        allOf(withId(R.id.search_src_text), withText("hindi"),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        1)),
                0),
            isDisplayed()));
    searchAutoComplete8.perform(pressImeActionButton());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(80000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction textView14 = onView(
        allOf(withId(R.id.item_language_name), withText("Hindi"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    1),
                1),
            isDisplayed()));
    textView14.check(matches(withText("Hindi")));

    ViewInteraction textView15 = onView(
        allOf(withId(R.id.item_language_localized_name), withText("हिन्दी"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    1),
                2),
            isDisplayed()));
    textView15.check(matches(withText("हिन्दी")));

    ViewInteraction checkBox3 = onView(
        allOf(withId(R.id.item_language_checkbox),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    1),
                0),
            isDisplayed()));
    checkBox3.check(matches(isDisplayed()));

    ViewInteraction appCompatCheckBox2 = onView(
        allOf(withId(R.id.item_language_checkbox),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    1),
                0),
            isDisplayed()));
    appCompatCheckBox2.perform(click());

    ViewInteraction appCompatImageView2 = onView(
        allOf(withId(R.id.search_close_btn), withContentDescription("Clear query"),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        1)),
                1),
            isDisplayed()));
    appCompatImageView2.perform(click());

    ViewInteraction textView16 = onView(
        allOf(withId(R.id.item_language_name), withText("Hindi"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    2),
                1),
            isDisplayed()));
    textView16.check(matches(withText("Hindi")));

    ViewInteraction textView17 = onView(
        allOf(withId(R.id.item_language_localized_name), withText("हिन्दी"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    2),
                2),
            isDisplayed()));
    textView17.check(matches(withText("हिन्दी")));

    ViewInteraction checkBox4 = onView(
        allOf(withId(R.id.item_language_checkbox),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    2),
                0),
            isDisplayed()));
    checkBox4.check(matches(isDisplayed()));

    ViewInteraction searchAutoComplete9 = onView(
        allOf(withId(R.id.search_src_text),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        1)),
                0),
            isDisplayed()));
    searchAutoComplete9.perform(click());

    ViewInteraction searchAutoComplete10 = onView(
        allOf(withId(R.id.search_src_text),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        1)),
                0),
            isDisplayed()));
    searchAutoComplete10.perform(replaceText("en"), closeSoftKeyboard());

    ViewInteraction searchAutoComplete11 = onView(
        allOf(withId(R.id.search_src_text), withText("eng"),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        1)),
                0),
            isDisplayed()));
    searchAutoComplete11.perform(replaceText("english"));

    ViewInteraction searchAutoComplete12 = onView(
        allOf(withId(R.id.search_src_text), withText("english"),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        1)),
                0),
            isDisplayed()));
    searchAutoComplete12.perform(closeSoftKeyboard());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(160000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction searchAutoComplete13 = onView(
        allOf(withId(R.id.search_src_text), withText("english"),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        1)),
                0),
            isDisplayed()));
    searchAutoComplete13.perform(pressImeActionButton());

    ViewInteraction textView18 = onView(
        allOf(withId(R.id.item_language_name), withText("English"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    0),
                1),
            isDisplayed()));
    textView18.check(matches(withText("English")));

    ViewInteraction checkBox5 = onView(
        allOf(withId(R.id.item_language_checkbox),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    0),
                0),
            isDisplayed()));
    checkBox5.check(matches(isDisplayed()));

    ViewInteraction textView19 = onView(
        allOf(withId(R.id.item_language_localized_name), withText("English"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    0),
                2),
            isDisplayed()));
    textView19.check(matches(withText("English")));

    ViewInteraction appCompatCheckBox3 = onView(
        allOf(withId(R.id.item_language_checkbox),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    0),
                0),
            isDisplayed()));
    appCompatCheckBox3.perform(click());

    ViewInteraction appCompatImageView3 = onView(
        allOf(withId(R.id.search_close_btn), withContentDescription("Clear query"),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        1)),
                1),
            isDisplayed()));
    appCompatImageView3.perform(click());

    ViewInteraction textView20 = onView(
        allOf(withId(R.id.item_language_name), withText("English"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    2),
                1),
            isDisplayed()));
    textView20.check(matches(withText("English")));

    ViewInteraction checkBox6 = onView(
        allOf(withId(R.id.item_language_checkbox),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    2),
                0),
            isDisplayed()));
    checkBox6.check(matches(isDisplayed()));

    ViewInteraction textView21 = onView(
        allOf(withId(R.id.item_language_localized_name), withText("English"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    2),
                2),
            isDisplayed()));
    textView21.check(matches(withText("English")));

    ViewInteraction appCompatImageButton = onView(
        allOf(withContentDescription("Collapse"),
            childAtPosition(
                allOf(withId(R.id.activity_language_toolbar),
                    childAtPosition(
                        withId(R.id.activity_language_appbar),
                        0)),
                1),
            isDisplayed()));
    appCompatImageButton.perform(click());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(640000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction appCompatImageButton2 = onView(
        allOf(withContentDescription("Navigate up"),
            childAtPosition(
                allOf(withId(R.id.activity_language_toolbar),
                    childAtPosition(
                        withId(R.id.activity_language_appbar),
                        0)),
                2),
            isDisplayed()));
    appCompatImageButton2.perform(click());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(640000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction textView22 = onView(
        allOf(withId(R.id.divider_text), withText("Selected languages:"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.library_list),
                    0),
                0),
            isDisplayed()));
    textView22.check(matches(withText("Selected languages:")));

    ViewInteraction actionMenuItemView6 = onView(
        allOf(withId(R.id.select_language), withContentDescription("Choose a language"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.toolbar),
                    2),
                1),
            isDisplayed()));
    actionMenuItemView6.perform(click());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(20000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction actionMenuItemView7 = onView(
        allOf(withId(R.id.menu_language_search), withContentDescription("Search"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_toolbar),
                    2),
                0),
            isDisplayed()));
    actionMenuItemView7.perform(click());

    ViewInteraction searchAutoComplete14 = onView(
        allOf(withId(R.id.search_src_text),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        1)),
                0),
            isDisplayed()));
    searchAutoComplete14.perform(replaceText("hindi"), closeSoftKeyboard());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(40000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction searchAutoComplete15 = onView(
        allOf(withId(R.id.search_src_text), withText("hindi"),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        1)),
                0),
            isDisplayed()));
    searchAutoComplete15.perform(pressImeActionButton());

    ViewInteraction textView23 = onView(
        allOf(withId(R.id.item_language_name), withText("Hindi"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    1),
                1),
            isDisplayed()));
    textView23.check(matches(withText("Hindi")));

    ViewInteraction appCompatCheckBox4 = onView(
        allOf(withId(R.id.item_language_checkbox),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    1),
                0),
            isDisplayed()));
    appCompatCheckBox4.perform(click());

    ViewInteraction appCompatImageView4 = onView(
        allOf(withId(R.id.search_close_btn), withContentDescription("Clear query"),
            childAtPosition(
                allOf(withId(R.id.search_plate),
                    childAtPosition(
                        withId(R.id.search_edit_frame),
                        1)),
                1),
            isDisplayed()));
    appCompatImageView4.perform(click());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(320000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction textView24 = onView(
        allOf(withId(R.id.item_language_name), withText("Hindi"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    2),
                1),
            isDisplayed()));
    textView24.check(matches(withText("Hindi")));

    ViewInteraction checkBox7 = onView(
        allOf(withId(R.id.item_language_checkbox),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    2),
                0),
            isDisplayed()));
    checkBox7.check(matches(isDisplayed()));

    ViewInteraction appCompatImageButton3 = onView(
        allOf(withContentDescription("Collapse"),
            childAtPosition(
                allOf(withId(R.id.activity_language_toolbar),
                    childAtPosition(
                        withId(R.id.activity_language_appbar),
                        0)),
                1),
            isDisplayed()));
    appCompatImageButton3.perform(click());

    ViewInteraction actionMenuItemView8 = onView(
        allOf(withId(R.id.menu_language_save), withContentDescription("Save languages"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_toolbar),
                    0),
                1),
            isDisplayed()));
    actionMenuItemView8.perform(click());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction textView25 = onView(
        allOf(withId(R.id.divider_text), withText("Selected languages:"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.library_list),
                    0),
                0),
            isDisplayed()));
    textView25.check(matches(withText("Selected languages:")));
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
