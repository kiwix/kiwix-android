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

package org.kiwix.kiwixmobile.intro;

import android.Manifest;
import android.os.SystemClock;
import android.support.test.espresso.intent.Intents;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.main.MainActivity;
import org.kiwix.kiwixmobile.splash.SplashActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.intent.matcher.IntentMatchers.toPackage;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;

import static android.support.test.espresso.matcher.ViewMatchers.withId;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class IntroActivityTest {

  @Rule
  public ActivityTestRule<IntroActivity> activityTestRule = new ActivityTestRule<>(IntroActivity.class);
  @Rule
  public GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  //@Test
  //public void SplashActivitySimple() {
  //
  //}

  //@Before
  //public void setUp(){
  //  //context = InstrumentationRegistry.getContext();
  //  //activityTestRule.launchActivity(null);
  //  //Intents.init();
  //  //preferences = PreferenceManager.getDefaultSharedPreferences(context);
  //}

  @Test
  public void testIntentforMainActivity() {
    Intents.init();

    // verify that the cards are displayed properly
    boolean found1, found2;
    found1 = found2 = false;
    int i;

    i = 0;
    while(!found1 && i < 3) {
      try {
        onView(withId(R.id.get_started)).perform(swipeLeft());
        SystemClock.sleep(500);
        onView(withId(R.id.get_started)).check(matches(withText("Save books offline")));
        found1 = true;
      } catch (Exception e) {
        // Do Nothing
      }
      i++;
    }
    assertEquals("siddharth", true, found1);


    // Verify that the button is there
    onView(withId(R.id.get_started)).check(matches(notNullValue()));
    onView(withId(R.id.get_started)).check(matches(withText("Get started")));
    onView(withId(R.id.get_started)).perform(click());



    // Test the intent generated for MainActivity
    intended(hasComponent(MainActivity.class.getName()));
  }

}
