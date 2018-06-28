package org.kiwix.kiwixmobile.splash;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.espresso.intent.Intents;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.schibsted.spain.barista.interaction.BaristaSleepInteractions;
import com.schibsted.spain.barista.rule.BaristaRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.intro.IntroActivity;
import org.kiwix.kiwixmobile.main.MainActivity;

import static org.junit.Assert.assertEquals;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_SHOW_INTRO;
import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SplashActivityTest {

  private Context context;

  @Rule
  public BaristaRule<SplashActivity> activityTestRule = BaristaRule.create(SplashActivity.class);
  @Rule
  public GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  @Before
  public void setUp(){
    context = getInstrumentation().getTargetContext();
  }

  @Test
  public void testFirstRun() {
    Intents.init();
    activityTestRule.launchActivity();
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    // Verify that the SplashActivity is followed by IntroActivity
    intended(hasComponent(IntroActivity.class.getName()));

    // Verify that the value of the "intro shown" boolean inside the SharedPreferences Database is not changed until the "Get started" button is pressed
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    assertEquals("verify that 'intro shown' boolean value is not changed in the ",
        true, preferences.getBoolean(PREF_SHOW_INTRO, true));
  }

  @Test
  public void testNormalRun() {
    SharedPreferences.Editor preferencesEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();
    preferencesEditor.putBoolean(PREF_SHOW_INTRO, false).apply();

    activityTestRule.launchActivity();
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    // Verify that the SplashActivity is followed by MainActivity
    intended(hasComponent(MainActivity.class.getName()));
  }
}