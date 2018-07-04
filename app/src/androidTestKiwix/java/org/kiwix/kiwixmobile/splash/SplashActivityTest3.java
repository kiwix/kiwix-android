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
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
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
public class SplashActivityTest3 {

    @Rule
    public ActivityTestRule<SplashActivity> mActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

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
        
         // Added a sleep statement to match the app's execution delay.
 // The recommended way to handle such scenarios is to use Espresso idling resources:
  // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
try {
 Thread.sleep(10000);
 } catch (InterruptedException e) {
 e.printStackTrace();
 }
        
        ViewInteraction recyclerView = onView(
allOf(withId(R.id.recycler_view),
childAtPosition(
withId(R.id.get_content_card),
1)));
        recyclerView.perform(actionOnItemAtPosition(1, click()));
        
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
 Thread.sleep(20000);
 } catch (InterruptedException e) {
 e.printStackTrace();
 }
        
        ViewInteraction appCompatTextView = onView(
allOf(withId(R.id.title), withText("Settings"),
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
        
        DataInteraction linearLayout = onData(anything())
.inAdapterView(allOf(withId(android.R.id.list),
childAtPosition(
withId(android.R.id.list_container),
0)))
.atPosition(5);
        linearLayout.perform(click());
        
         // Added a sleep statement to match the app's execution delay.
 // The recommended way to handle such scenarios is to use Espresso idling resources:
  // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
try {
 Thread.sleep(20000);
 } catch (InterruptedException e) {
 e.printStackTrace();
 }
        
        pressBack();
        
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
