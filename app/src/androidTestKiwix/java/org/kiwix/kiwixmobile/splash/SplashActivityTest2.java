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
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
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

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction appCompatTextView = onView(
        allOf(withId(R.id.title), withText("Help"),
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

    ViewInteraction textView = onView(
        allOf(withText("Help"),
            childAtPosition(
                allOf(withId(R.id.activity_help_toolbar),
                    childAtPosition(
                        withId(R.id.activity_help_appbar),
                        0)),
                1),
            isDisplayed()));
    textView.check(matches(withText("Help")));

    ViewInteraction imageButton = onView(
        allOf(withContentDescription("Navigate up"),
            childAtPosition(
                allOf(withId(R.id.activity_help_toolbar),
                    childAtPosition(
                        withId(R.id.activity_help_appbar),
                        0)),
                0),
            isDisplayed()));
    imageButton.check(matches(isDisplayed()));

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(40000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction appCompatImageButton = onView(
        allOf(withContentDescription("Navigate up"),
            childAtPosition(
                allOf(withId(R.id.activity_help_toolbar),
                    childAtPosition(
                        withId(R.id.activity_help_appbar),
                        0)),
                1),
            isDisplayed()));
    appCompatImageButton.perform(click());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction textView2 = onView(
        allOf(withText("Wikipedia"),
            childAtPosition(
                allOf(withId(R.id.toolbar),
                    childAtPosition(
                        withId(R.id.toolbar_layout),
                        0)),
                1),
            isDisplayed()));
    textView2.check(matches(withText("Wikipedia")));

    ViewInteraction webView = onView(
        allOf(childAtPosition(
            childAtPosition(
                withId(R.id.content_frame),
                0),
            0),
            isDisplayed()));
    webView.check(matches(isDisplayed()));

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(20000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());

    ViewInteraction appCompatTextView2 = onView(
        allOf(withId(R.id.title), withText("Help"),
            childAtPosition(
                childAtPosition(
                    withClassName(is("android.support.v7.view.menu.ListMenuItemView")),
                    0),
                0),
            isDisplayed()));
    appCompatTextView2.perform(click());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(80000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    pressBack();

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(5000);
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
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction appCompatTextView3 = onView(
        allOf(withId(R.id.title), withText("Help"),
            childAtPosition(
                childAtPosition(
                    withClassName(is("android.support.v7.view.menu.ListMenuItemView")),
                    0),
                0),
            isDisplayed()));
    appCompatTextView3.perform(click());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(20000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction textView3 = onView(
        allOf(withText("Help"),
            childAtPosition(
                allOf(withId(R.id.activity_help_toolbar),
                    childAtPosition(
                        withId(R.id.activity_help_appbar),
                        0)),
                1),
            isDisplayed()));
    textView3.check(matches(withText("Help")));

    ViewInteraction textView4 = onView(
        allOf(withId(R.id.activity_help_feedback_text_view), withText("Send feedback"),
            childAtPosition(
                childAtPosition(
                    withId(android.R.id.content),
                    0),
                2),
            isDisplayed()));
    textView4.check(matches(withText("Send feedback")));

    ViewInteraction textView5 = onView(
        allOf(withId(R.id.item_help_title), withText("What does Kiwix do?"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_help_recycler_view),
                    0),
                0),
            isDisplayed()));
    textView5.check(matches(withText("What does Kiwix do?")));

    ViewInteraction textView6 = onView(
        allOf(withId(R.id.item_help_title), withText("How to use large ZIM files?"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_help_recycler_view),
                    1),
                0),
            isDisplayed()));
    textView6.check(matches(withText("How to use large ZIM files?")));

    ViewInteraction textView7 = onView(
        allOf(withId(R.id.item_help_title), withText("Where is the content?"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_help_recycler_view),
                    2),
                0),
            isDisplayed()));
    textView7.check(matches(withText("Where is the content?")));

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(80000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction appCompatImageView = onView(
        allOf(withId(R.id.item_help_toggle_expand), withContentDescription("Expand"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_help_recycler_view),
                    0),
                1),
            isDisplayed()));
    appCompatImageView.perform(click());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(160000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction textView8 = onView(
        allOf(withId(R.id.item_help_description), withText(
            "Kiwix is an offline content reader. It acts very much like a browser but instead of accessing online web pages, it reads content from a file in ZIM format.\nWhile Kiwix has been originally designed to provide Wikipedia offline, it also reads other contents.\n"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_help_recycler_view),
                    0),
                1),
            isDisplayed()));
    textView8.check(matches(withText(
        "Kiwix is an offline content reader. It acts very much like a browser but instead of accessing online web pages, it reads content from a file in ZIM format. While Kiwix has been originally designed to provide Wikipedia offline, it also reads other contents. ")));

    ViewInteraction textView9 = onView(
        allOf(withId(R.id.item_help_description), withText(
            "Kiwix is an offline content reader. It acts very much like a browser but instead of accessing online web pages, it reads content from a file in ZIM format.\nWhile Kiwix has been originally designed to provide Wikipedia offline, it also reads other contents.\n"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_help_recycler_view),
                    0),
                1),
            isDisplayed()));
    textView9.check(matches(isDisplayed()));

    ViewInteraction textView10 = onView(
        allOf(withId(R.id.item_help_description), withText(
            "Kiwix is an offline content reader. It acts very much like a browser but instead of accessing online web pages, it reads content from a file in ZIM format.\nWhile Kiwix has been originally designed to provide Wikipedia offline, it also reads other contents.\n"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_help_recycler_view),
                    0),
                1),
            isDisplayed()));
    textView10.check(doesNotExist());

    ViewInteraction appCompatImageView2 = onView(
        allOf(withId(R.id.item_help_toggle_expand), withContentDescription("Expand"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_help_recycler_view),
                    0),
                1),
            isDisplayed()));
    appCompatImageView2.perform(click());

    ViewInteraction appCompatImageView3 = onView(
        allOf(withId(R.id.item_help_toggle_expand), withContentDescription("Expand"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_help_recycler_view),
                    1),
                1),
            isDisplayed()));
    appCompatImageView3.perform(click());

    ViewInteraction textView11 = onView(
        allOf(withId(R.id.item_help_description), withText(
            "If your ZIM file is larger than 4GB, you might not be able to store it on your SD card due to filesystem limitations.\nDownloading in-app automatically handles larger files, splitting them up for you.\nIf however you are downloading on another device, you may need to split the ZIM file up using the following software:\n• On Microsoft Windows: HJ-Split\n• On Apple Mac OSX: Split and Concat\n• On GNU/Linux and with the console: split --bytes=4000M my_big_file.zim\nNote: your resulting files must be named my_big_file.zimaa, my_big_file.zimab, etc.\n"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_help_recycler_view),
                    1),
                1),
            isDisplayed()));
    textView11.check(matches(isDisplayed()));

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(160000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction appCompatImageView4 = onView(
        allOf(withId(R.id.item_help_toggle_expand), withContentDescription("Expand"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_help_recycler_view),
                    2),
                1),
            isDisplayed()));
    appCompatImageView4.perform(click());

    ViewInteraction textView12 = onView(
        allOf(withId(R.id.item_help_description), withText(
            "Our content is hosted on the Kiwix website.\nThey are available as ZIM files. There are a lot of them:\n• Wikipedia is available separately for each language\n• Other contents like Wikileaks or Wikisource are also available\nYou can either download your chosen ZIM files in-app or carefuly select the one(s) you want and download from a Desktop computer before transferring the ZIM files to your SD card.\nZIM files download in-app are located in the external storage directory in a folder entitled Kiwix.\n"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_help_recycler_view),
                    1),
                1),
            isDisplayed()));
    textView12.check(matches(isDisplayed()));

    ViewInteraction textView13 = onView(
        allOf(withId(R.id.item_help_description), withText(
            "Our content is hosted on the Kiwix website.\nThey are available as ZIM files. There are a lot of them:\n• Wikipedia is available separately for each language\n• Other contents like Wikileaks or Wikisource are also available\nYou can either download your chosen ZIM files in-app or carefuly select the one(s) you want and download from a Desktop computer before transferring the ZIM files to your SD card.\nZIM files download in-app are located in the external storage directory in a folder entitled Kiwix.\n"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_help_recycler_view),
                    1),
                1),
            isDisplayed()));
    textView13.check(doesNotExist());

    ViewInteraction appCompatImageView5 = onView(
        allOf(withId(R.id.item_help_toggle_expand), withContentDescription("Expand"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_help_recycler_view),
                    1),
                1),
            isDisplayed()));
    appCompatImageView5.perform(click());

    ViewInteraction appCompatImageView6 = onView(
        allOf(withId(R.id.item_help_toggle_expand), withContentDescription("Expand"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_help_recycler_view),
                    1),
                1),
            isDisplayed()));
    appCompatImageView6.perform(click());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(160000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction appCompatImageButton2 = onView(
        allOf(withContentDescription("Navigate up"),
            childAtPosition(
                allOf(withId(R.id.activity_help_toolbar),
                    childAtPosition(
                        withId(R.id.activity_help_appbar),
                        0)),
                1),
            isDisplayed()));
    appCompatImageButton2.perform(click());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(160000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction textView14 = onView(
        allOf(withText("Wikipedia"),
            childAtPosition(
                allOf(withId(R.id.toolbar),
                    childAtPosition(
                        withId(R.id.toolbar_layout),
                        0)),
                1),
            isDisplayed()));
    textView14.check(matches(withText("Wikipedia")));
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
