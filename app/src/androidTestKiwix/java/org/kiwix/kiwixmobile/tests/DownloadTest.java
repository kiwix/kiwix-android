package org.kiwix.kiwixmobile.tests;


import android.Manifest;
import android.app.Activity;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.rule.GrantPermissionRule;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import com.squareup.spoon.Spoon;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.utils.KiwixIdlingResource;
import org.kiwix.kiwixmobile.utils.SplashActivity;

import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.kiwix.kiwixmobile.testutils.TestUtils.allowPermissionsIfNeeded;
import static org.kiwix.kiwixmobile.testutils.TestUtils.withContent;
import static org.kiwix.kiwixmobile.testutils.TestUtils.getCurrentActivity;
import static org.kiwix.kiwixmobile.utils.StandardActions.deleteZimIfExists;
import static org.kiwix.kiwixmobile.utils.StandardActions.enterHelp;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class DownloadTest {

  public static final String KIWIX_DOWNLOAD_TEST = "kiwixDownloadTest";
  @Rule
  public ActivityTestRule<SplashActivity> mActivityTestRule = new ActivityTestRule(
          SplashActivity.class);
  @Rule
  public GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  @BeforeClass
  public static void beforeClass() {
    IdlingPolicies.setMasterPolicyTimeout(10, TimeUnit.SECONDS);
    IdlingPolicies.setIdlingResourceTimeout(10, TimeUnit.SECONDS);
    Espresso.registerIdlingResources(KiwixIdlingResource.getInstance());
  }

  @Before
  public void setUp() {
  }

  @Test
  public void downloadTest() {
    enterHelp();
    onView(withText("Get Content")).perform(click());

    ViewInteraction appCompatTextView = onView(
            allOf(withText("Device"), isDisplayed()));
    appCompatTextView.perform(click());

    Activity currentActivity = getCurrentActivity();

    allowPermissionsIfNeeded();

    deleteZimIfExists("ray_charles", R.id.zimfilelist);

    ViewInteraction appCompatTextView2 = onView(
            allOf(withText("Online"), isDisplayed()));
    appCompatTextView2.perform(click());

    try {
      onView(withId(R.id.network_permission_button)).perform(click());
      Log.d(KIWIX_DOWNLOAD_TEST, "Clicked Network Permission Button");
    } catch (RuntimeException e) {
      Log.d(KIWIX_DOWNLOAD_TEST, "Failed to click Network Permission Button", e);
    }

    Spoon.screenshot(currentActivity, "Before-checking-for-ZimManager-Main-Activity");
    ViewInteraction viewPager2 = onView(
            allOf(withId(R.id.container),
                    withParent(allOf(withId(R.id.zim_manager_main_activity),
                            withParent(withId(android.R.id.content)))),
                    isDisplayed()));
    Spoon.screenshot(currentActivity, "After-the-check-completed");

    onData(withContent("ray_charles")).inAdapterView(withId(R.id.library_list)).perform(click());

    try {
      onView(withId(android.R.id.button1)).perform(click());
    } catch (RuntimeException e) {
      // Confirmation not shown
    }

    ViewInteraction appCompatTextView3 = onView(
            allOf(withText("Device"), isDisplayed()));
    appCompatTextView3.perform(click());

    try {
      onData(allOf(withId(R.id.zim_swiperefresh)))
              .perform(swipeDown());
    } catch (RuntimeException e) {
      Log.w(KIWIX_DOWNLOAD_TEST, "Failed to refresh ZIM list: " + e.getLocalizedMessage());
    }

/*
Commented out the following as it uses another Activity.
TODO Once we find a good way to run cross-activity re-implement
this functionality in the tests.

    onData(withContent("ray_charles")).inAdapterView(withId(R.id.zimfilelist)).perform(click());
    openContextualActionModeOverflowMenu();
    onView(withText("Get Content")).perform(click());
*/

    onData(withContent("ray_charles")).inAdapterView(withId(R.id.zimfilelist)).perform(longClick());
    onView(withId(android.R.id.button1)).perform(click());
  }

  @After
  public void finish() {
    Espresso.unregisterIdlingResources(KiwixIdlingResource.getInstance());
  }

}