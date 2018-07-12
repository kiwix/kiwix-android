package org.kiwix.kiwixmobile.splash;


import android.support.test.espresso.DataInteraction;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;

import org.kiwix.kiwixmobile.R;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class finaltest {

    @Rule
    public ActivityTestRule<SplashActivity> mActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Test
    public void finaltest() {
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
        
        ViewInteraction actionMenuItemView2 = onView(
allOf(withId(R.id.menu_language_search), withContentDescription("Search"),
childAtPosition(
childAtPosition(
withId(R.id.activity_language_toolbar),
2),
0),
isDisplayed()));
        actionMenuItemView2.perform(click());
        
         // Added a sleep statement to match the app's execution delay.
 // The recommended way to handle such scenarios is to use Espresso idling resources:
  // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
try {
 Thread.sleep(40000);
 } catch (InterruptedException e) {
 e.printStackTrace();
 }
        
        ViewInteraction searchAutoComplete = onView(
allOf(withId(R.id.search_src_text),
childAtPosition(
allOf(withId(R.id.search_plate),
childAtPosition(
withId(R.id.search_edit_frame),
1)),
0),
isDisplayed()));
        searchAutoComplete.perform(replaceText("b"), closeSoftKeyboard());
        
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
        
        ViewInteraction searchAutoComplete2 = onView(
allOf(withId(R.id.search_src_text),
childAtPosition(
allOf(withId(R.id.search_plate),
childAtPosition(
withId(R.id.search_edit_frame),
1)),
0),
isDisplayed()));
        searchAutoComplete2.perform(replaceText("bengali"), closeSoftKeyboard());
        
        ViewInteraction searchAutoComplete3 = onView(
allOf(withId(R.id.search_src_text), withText("bengali"),
childAtPosition(
allOf(withId(R.id.search_plate),
childAtPosition(
withId(R.id.search_edit_frame),
1)),
0),
isDisplayed()));
        searchAutoComplete3.perform(pressImeActionButton());
        
         // Added a sleep statement to match the app's execution delay.
 // The recommended way to handle such scenarios is to use Espresso idling resources:
  // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
try {
 Thread.sleep(80000);
 } catch (InterruptedException e) {
 e.printStackTrace();
 }
        
        ViewInteraction textView = onView(
allOf(withId(R.id.item_language_name), withText("Bengali"),
childAtPosition(
childAtPosition(
withId(R.id.activity_language_recycler_view),
0),
1),
isDisplayed()));
        textView.check(matches(withText("Bengali")));
        
        ViewInteraction textView2 = onView(
allOf(withId(R.id.item_language_localized_name), withText("বাংলা"),
childAtPosition(
childAtPosition(
withId(R.id.activity_language_recycler_view),
0),
2),
isDisplayed()));
        textView2.check(matches(withText("বাংলা")));
        
        ViewInteraction checkBox = onView(
allOf(withId(R.id.item_language_checkbox),
childAtPosition(
childAtPosition(
withId(R.id.activity_language_recycler_view),
0),
0),
isDisplayed()));
        checkBox.check(matches(isDisplayed()));
        
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
 Thread.sleep(5000);
 } catch (InterruptedException e) {
 e.printStackTrace();
 }
        
        ViewInteraction actionMenuItemView3 = onView(
allOf(withId(R.id.select_language), withContentDescription("Choose a language"),
childAtPosition(
childAtPosition(
withId(R.id.toolbar),
2),
1),
isDisplayed()));
        actionMenuItemView3.perform(click());
        
         // Added a sleep statement to match the app's execution delay.
 // The recommended way to handle such scenarios is to use Espresso idling resources:
  // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
try {
 Thread.sleep(20000);
 } catch (InterruptedException e) {
 e.printStackTrace();
 }
        
        ViewInteraction textView3 = onView(
allOf(withId(R.id.item_language_name), withText("Bengali"),
childAtPosition(
childAtPosition(
withId(R.id.activity_language_recycler_view),
3),
1),
isDisplayed()));
        textView3.check(matches(withText("Bengali")));
        
        ViewInteraction checkBox2 = onView(
allOf(withId(R.id.item_language_checkbox),
childAtPosition(
childAtPosition(
withId(R.id.activity_language_recycler_view),
3),
0),
isDisplayed()));
        checkBox2.check(matches(isDisplayed()));
        
        ViewInteraction textView4 = onView(
allOf(withId(R.id.item_language_localized_name), withText("বাংলা"),
childAtPosition(
childAtPosition(
withId(R.id.activity_language_recycler_view),
3),
2),
isDisplayed()));
        textView4.check(matches(withText("বাংলা")));
        
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
                        && view.equals(((ViewGroup)parent).getChildAt(position));
            }
        };
    }
    }
