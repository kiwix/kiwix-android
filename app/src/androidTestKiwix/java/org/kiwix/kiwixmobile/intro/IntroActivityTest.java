package org.kiwix.kiwixmobile.intro;

import android.Manifest;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.intent.Intents;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.schibsted.spain.barista.interaction.BaristaSleepInteractions;
import com.schibsted.spain.barista.rule.BaristaRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.main.MainActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class IntroActivityTest {

  @Rule
  public BaristaRule<IntroActivity> activityTestRule = BaristaRule.create(IntroActivity.class);
  @Rule
  public GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  @Test
  public void testUI(){
    activityTestRule.launchActivity();
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    ViewInteraction viewPager = onView(withId(R.id.view_pager));

    // Verify that the sliding view is working properly
    viewPager.perform(swipeLeft());
    onView(allOf(withId(R.id.heading), withText(R.string.save_books_offline), isDisplayed()))
        .check(matches(notNullValue()));
    onView(allOf(withId(R.id.subheading), withText(R.string.download_books_message), isDisplayed()))
        .check(matches(notNullValue()));

    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    viewPager.perform(swipeRight());
    onView(allOf(withId(R.id.heading), withText(R.string.welcome_to_the_family), isDisplayed()))
        .check(matches(notNullValue()));
    onView(allOf(withId(R.id.subheading), withText(R.string.human_kind_knowledge), isDisplayed()))
        .check(matches(notNullValue()));
  }

  @Test
  public void testIntentCreated() {
    activityTestRule.launchActivity();
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    Intents.init();

    // Verify that the button is there
    onView(withId(R.id.get_started)).check(matches(notNullValue()));
    onView(withId(R.id.get_started)).check(matches(withText(R.string.get_started)));
    onView(withId(R.id.get_started)).perform(click());

    // Test the intent generated for MainActivity
    intended(hasComponent(MainActivity.class.getName()));
  }
}
