/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
 *
 */

package org.kiwix.kiwixmobile.core.splash;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.core.intro.IntroActivity;
import org.kiwix.kiwixmobile.core.main.CoreMainActivity;

import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static org.junit.Assert.assertEquals;
import static org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.PREF_SHOW_INTRO;
import static org.kiwix.sharedFunctions.TestConstantsKt.TEST_PAUSE_MS;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SplashActivityTest {

  private ActivityTestRule<SplashActivity> activityTestRule =
    new ActivityTestRule<>(SplashActivity.class, true, false);
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
    shouldShowIntro(true);
    activityTestRule.launchActivity(new Intent());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    // Verify that the SplashActivity is followed by IntroActivity
    intended(hasComponent(IntroActivity.class.getName()));

    // Verify that the value of the "intro shown" boolean inside the SharedPreferences Database is not changed until the "Get started" button is pressed
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    assertEquals(true, preferences.getBoolean(PREF_SHOW_INTRO, true));
  }

  @Test
  public void testNormalRun() {
    shouldShowIntro(false);
    intending(hasAction(CoreMainActivity.class.getCanonicalName())).respondWith(
      new Instrumentation.ActivityResult(Activity.RESULT_OK, new Intent()));
    activityTestRule.launchActivity(new Intent());
  }

  @After
  public void endTest() {
    Intents.release();
  }

  private void shouldShowIntro(boolean value) {
    SharedPreferences.Editor preferencesEditor =
      PreferenceManager.getDefaultSharedPreferences(context).edit();
    preferencesEditor.putBoolean(PREF_SHOW_INTRO, value).apply();
  }
}
