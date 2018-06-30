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

package org.kiwix.kiwixmobile.main;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.intent.Intents;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

//import android.view.View;
//import android.view.ViewGroup;
//import android.view.ViewParent;
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
import org.kiwix.kiwixmobile.main.MainActivity;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_BOTTOM_TOOLBAR;

//import static android.support.test.espresso.Espresso.onView;
//import static android.support.test.espresso.action.ViewActions.click;
//import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
//import static android.support.test.espresso.matcher.ViewMatchers.withId;
//import static org.hamcrest.Matchers.allOf;
//import static org.junit.Assert.assertEquals;
//import static org.kiwix.kiwixmobile.utils.Constants.PREF_BOTTOM_TOOLBAR;
//import static org.kiwix.kiwixmobile.utils.Constants.PREF_SHOW_INTRO;
//import static android.support.test.InstrumentationRegistry.getInstrumentation;
//import static android.support.test.espresso.intent.Intents.intended;
//import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
//import static org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class BottomBarTest {

  private Context context;

  @Rule
  public BaristaRule<MainActivity> activityTestRule = BaristaRule.create(MainActivity.class);
  @Rule
  public GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  @Before
  public void setUp(){
    // Set the toolbar to appear
    context = getInstrumentation().getTargetContext();
    SharedPreferences.Editor preferencesEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();
    preferencesEditor.putBoolean(PREF_BOTTOM_TOOLBAR, true).apply();
  }

  @Test
  public void testFirstRun() {
    Intents.init();
    activityTestRule.launchActivity();
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    //ViewInteraction recyclerView = onView(
    //    allOf(withId(R.id.recycler_view),
    //        childAtPosition(
    //            withId(R.id.get_content_card),
    //            1)));
    //recyclerView.perform(actionOnItemAtPosition(1, click()));
    Intents.release();
  }

  //private static Matcher<View> childAtPosition(
  //    final Matcher<View> parentMatcher, final int position) {
  //
  //  return new TypeSafeMatcher<View>() {
  //    @Override
  //    public void describeTo(Description description) {
  //      description.appendText("Child at position " + position + " in parent ");
  //      parentMatcher.describeTo(description);
  //    }
  //
  //    @Override
  //    public boolean matchesSafely(View view) {
  //      ViewParent parent = view.getParent();
  //      return parent instanceof ViewGroup && parentMatcher.matches(parent)
  //          && view.equals(((ViewGroup) parent).getChildAt(position));
  //    }
  //  };
  //}
}
