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

package org.kiwix.kiwixmobile.splash;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.test.espresso.intent.Intents;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions;
import com.schibsted.spain.barista.rule.BaristaRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.intro.IntroActivity;
import org.kiwix.kiwixmobile.main.MainActivity;

import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static org.junit.Assert.assertEquals;
import static org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_SHOW_INTRO;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SplashActivityTest {

  @Rule
  public BaristaRule<SplashActivity> activityTestRule = BaristaRule.create(SplashActivity.class);
  @Rule
  public GrantPermissionRule readPermissionRule =
      GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule =
      GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);
  private Context context;

  @Before
  public void setUp() {
    Intents.init();
    context = getInstrumentation().getTargetContext();
  }

  @Test
  public void testFirstRun() {
    activityTestRule.launchActivity();
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    // Verify that the SplashActivity is followed by IntroActivity
    intended(hasComponent(IntroActivity.class.getName()));

    // Verify that the value of the "intro shown" boolean inside the SharedPreferences Database is not changed until the "Get started" button is pressed
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    assertEquals(true, preferences.getBoolean(PREF_SHOW_INTRO, true));
  }

  @Test
  public void testNormalRun() {
    SharedPreferences.Editor preferencesEditor =
        PreferenceManager.getDefaultSharedPreferences(context).edit();
    preferencesEditor.putBoolean(PREF_SHOW_INTRO, false).apply();

    activityTestRule.launchActivity();
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    // Verify that the SplashActivity is followed by MainActivity
    intended(hasComponent(MainActivity.class.getName()));
  }

  @After
  public void endTest() {
    Intents.release();
  }
}
