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

package org.kiwix.kiwixmobile.language;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.intent.Intents;
import android.support.test.filters.FlakyTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions;
import com.schibsted.spain.barista.rule.BaristaRule;
import java.util.Locale;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.intro.IntroActivity;
import org.kiwix.kiwixmobile.models.Language;
import org.kiwix.kiwixmobile.splash.SplashActivity;

import static android.support.test.InstrumentationRegistry.getContext;
import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.kiwix.kiwixmobile.utils.RecyclerViewItemCountAssertion.withItemCount;
import static org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS;

public class LanguageActivityTest {

  @Rule
  public BaristaRule<IntroActivity> activityTestRule = BaristaRule.create(IntroActivity.class);
  @Rule
  public GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  @Before
  public void setUp(){
    Intents.init();
    activityTestRule.launchActivity();
  }

  @Test
  public void testIntroActivity() {
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    onView(withId(R.id.get_started)).perform(click());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
    onView(withText("Get Content")).perform(click());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

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

    onView(withContentDescription("Choose a language")).check(matches(notNullValue()));
    onView(withContentDescription("Choose a language")).perform(click());

    // TODO : figure out a way to get the refrence of the activity from the activitytestrule to test the toast
    //onView(withText("Content Still Loading")).inRoot(withDecorView(not(activityTestRule.getActivity().getWindow().getDecorView()))).check(matches(isDisplayed()));
    //onView(withText("Content Still Loading")).inRoot(withDecorView(not(is(activityTestRule.getActivity().getWindow().getDecorView())))).check(matches(isDisplayed()));


    BaristaSleepInteractions.sleep(TEST_PAUSE_MS * 20);

    try{
      onView(allOf(isDisplayed(), withText("Selected languages:"))).check(matches(notNullValue()));
      //onView(allOf(isDisplayed(), withText("Other languages:"))).check(matches(notNullValue()));
    }catch (Exception e){
      BaristaSleepInteractions.sleep(TEST_PAUSE_MS * 50);
    }

    onView(allOf(isDisplayed(), withText("Selected languages:"))).check(matches(notNullValue()));
    //onView(allOf(isDisplayed(), withText("Other languages:"))).check(matches(notNullValue()));

    //Locale deflocale = getCurrentLocale(getContext());

    //Language defLanguage = deflocale.getLanguage();

    onView(withContentDescription("Choose a language")).perform(click());

    // verify that the list of languages to select is present
    onView(withId(R.id.activity_language_recycler_view)).check(withItemCount(greaterThan(0)));

    ViewInteraction checkBox = onView(
        allOf(withId(R.id.item_language_checkbox),
            childAtPosition(
                childAtPosition(
                    withId(R.id.activity_language_recycler_view),
                    3),
                0),
            isDisplayed()));
    checkBox.perform(click());



    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);



    onView(withId(R.id.menu_language_save)).perform(click());

    // TODO: make sure the it is only possible to open the activity after the network call is finished and book list is updated
    // TODO: test that default language is based on device locale
    // TODO: test selecting language updates UI
    // TODO: make sure selecting new languages does not generate a new network request
    // TODO: verify all the correct books are displayed and are displayed correctly
    // TODO: test selecting no language is allowed
  }

  @After
  public void endTest(){
    Intents.release();
  }


  Locale getCurrentLocale(Context context){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
      return context.getResources().getConfiguration().getLocales().get(0);
    } else{
      //noinspection deprecation
      return context.getResources().getConfiguration().locale;
    }
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
