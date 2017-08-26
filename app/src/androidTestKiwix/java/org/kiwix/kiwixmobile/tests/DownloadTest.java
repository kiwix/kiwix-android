package org.kiwix.kiwixmobile.tests;


import android.Manifest;
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
import org.kiwix.kiwixmobile.zim_manager.ZimManageActivity;

import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.kiwix.kiwixmobile.testutils.TestUtils.withContent;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class DownloadTest {

  @Rule
  public ActivityTestRule<ZimManageActivity> mActivityTestRule = new ActivityTestRule<>(
      ZimManageActivity.class);
  @Rule
  public GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  @BeforeClass
  public static void beforeClass() {
    IdlingPolicies.setMasterPolicyTimeout(350, TimeUnit.SECONDS);
    IdlingPolicies.setIdlingResourceTimeout(350, TimeUnit.SECONDS);
  }

  @Before
  public void setUp() {
    Espresso.registerIdlingResources(KiwixIdlingResource.getInstance());

  }

  @Test
  public void downloadTest() {
    ViewInteraction appCompatTextView = onView(
        allOf(withText("Device"), isDisplayed()));
    appCompatTextView.perform(click());

    try {
      onData(withContent("ray_charles")).inAdapterView(withId(R.id.zimfilelist)).perform(longClick());
      onView(withId(android.R.id.button1)).perform(click());
    } catch (RuntimeException e) {

    }

    ViewInteraction appCompatTextView2 = onView(
        allOf(withText("Online"), isDisplayed()));
    appCompatTextView2.perform(click());

    try {
      onView(withId(R.id.network_permission_button)).perform(click());
      Log.d("kiwixDownloadTest", "Clicked Network Permission Button");
    } catch (RuntimeException e) {
      Log.d("kiwixDownloadTest", "Failed to click Network Permission Button", e);
    }

    Spoon.screenshot(mActivityTestRule.getActivity(), "Before-checking-for-ZimManager-Main-Activity");
    ViewInteraction viewPager2 = onView(
        allOf(withId(R.id.container),
            withParent(allOf(withId(R.id.zim_manager_main_activity),
                withParent(withId(android.R.id.content)))),
            isDisplayed()));
    Spoon.screenshot(mActivityTestRule.getActivity(), "After-the-check-completed");

    onData(withContent("ray_charles")).inAdapterView(withId(R.id.library_list)).perform(click());

    try {
      onView(withId(android.R.id.button1)).perform(click());
    } catch (RuntimeException e) {
    }

    ViewInteraction appCompatTextView3 = onView(
        allOf(withText("Device"), isDisplayed()));
    appCompatTextView3.perform(click());

    onView(withId(R.id.menu_rescan_fs))
        .perform(click());

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
