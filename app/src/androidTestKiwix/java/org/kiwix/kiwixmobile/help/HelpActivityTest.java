/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kiwix.kiwixmobile.help;

import android.Manifest;
import android.content.Context;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.intent.Intents;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions;
import com.schibsted.spain.barista.rule.BaristaRule;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.intro.IntroActivity;
import org.kiwix.kiwixmobile.main.MainActivity;
import org.kiwix.kiwixmobile.splash.SplashActivity;
import org.kiwix.kiwixmobile.zim_manager.library_view.LibraryFragment;

import static org.kiwix.kiwixmobile.testutils.Matcher.childAtPosition;
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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class HelpActivityTest {

  @Rule
  public BaristaRule<IntroActivity> activityTestRule = BaristaRule.create(IntroActivity.class);
  @Rule
  public GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  private Context context;

  @Before
  public void setUp() {
    Intents.init();
    context = getInstrumentation().getTargetContext();
    activityTestRule.launchActivity();
  }

  @Test
  public void testHelpActivity() {
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    try{
      onView(withId(R.id.get_started)).check(matches(notNullValue()));
      onView(withId(R.id.get_started)).perform(click());
    }catch (Exception e){
      // The app didn't start with the IntroActivity
    }
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);


    //onView(withText("Kiwix")).check(matches(notNullValue())); //TODO: verify that this is on the top toolbar

    openActionBarOverflowOrOptionsMenu(context);
    onView(withText("Help")).perform(click());

    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    // Verify that the help Activity is opened
    onView(withText("Help")).check(matches(notNullValue())); //TODO: verify that this is on the top toolbar

    // Verify that the back button is displayed
    onView(withContentDescription("Navigate up")).check(matches(notNullValue()));


    // Verify that going back from the help screen we go to the previous screen
    onView(withContentDescription("Navigate up")).perform(click());

    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    // Verify that the home activity is opened
    // TODO : fix this issue
    //onView(withText("Wikipedia")).check(matches(notNullValue())); //TODO: verify that this is on the top toolbar


    /*
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
    */

    /*
    ViewInteraction webView = onView(
        allOf(childAtPosition(
            childAtPosition(
                withId(R.id.content_frame),
                0),
            0),
            isDisplayed()));
    webView.check(matches(isDisplayed()));

    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    */

    /*
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

    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    
    pressBack();

    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

*/
    ViewInteraction recyclerView = onView(
        allOf(withId(R.id.recycler_view),
            childAtPosition(
                withId(R.id.get_content_card),
                1)));
    recyclerView.perform(actionOnItemAtPosition(1, click()));

    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);


    openActionBarOverflowOrOptionsMenu(context);
    onView(withText("Help")).perform(click());

    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);


    // Verify that the help Activity is opened
    onView(withText("Help")).check(matches(notNullValue())); //TODO: verify that this is on the top toolbar

    // Verify that the back button is displayed
    onView(withContentDescription("Navigate up")).check(matches(notNullValue()));


    // Verify that going back from the help screen we go to the previous screen
    onView(withContentDescription("Navigate up")).perform(click());

    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    openActionBarOverflowOrOptionsMenu(context);
    onView(withText("Help")).perform(click());

    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    // verify that all the headings are present:
    // send feedback
    onView(withText(context.getString(R.string.send_feedback))).check(matches(notNullValue()));
    //"What does Kiwix do?"
    onView(withText(context.getString(R.string.help_2))).check(matches(notNullValue()));
    //How to use large ZIM files?"
    onView(withText(context.getString(R.string.help_12))).check(matches(notNullValue()));
    //"Where is the content?"
    onView(withText(context.getString(R.string.help_5))).check(matches(notNullValue()));
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    /*

    ViewInteraction appCompatImageView = onView(
        allOf(withId(R.id.item_help_toggle_expand), withContentDescription("Expand"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_help_recycler_view),
                    0),
                1),
            isDisplayed()));
    appCompatImageView.perform(click());

    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);


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

    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);


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

    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);


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

    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

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

*/

  }

  @After
  public void endTest() {
    IdlingRegistry.getInstance().unregister(LibraryFragment.IDLING_RESOURCE);
    Intents.release();
  }
}
