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

import com.schibsted.spain.barista.interaction.BaristaSleepInteractions;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.utils.KiwixIdlingResource;
import org.kiwix.kiwixmobile.splash.SplashActivity;

import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed;
import static com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn;
import static com.schibsted.spain.barista.interaction.BaristaSwipeRefreshInteractions.refresh;
import static junit.framework.Assert.fail;
import static org.hamcrest.Matchers.allOf;
import static org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS;
import static org.kiwix.kiwixmobile.testutils.TestUtils.allowPermissionsIfNeeded;
import static org.kiwix.kiwixmobile.testutils.TestUtils.captureAndSaveScreenshot;
import static org.kiwix.kiwixmobile.testutils.TestUtils.withContent;
import static org.kiwix.kiwixmobile.utils.StandardActions.deleteZimIfExists;
import static org.kiwix.kiwixmobile.utils.StandardActions.enterHelp;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class DownloadTest {

  public static final String KIWIX_DOWNLOAD_TEST = "kiwixDownloadTest";
  @Rule
  public ActivityTestRule<SplashActivity> mActivityTestRule = new ActivityTestRule<>(SplashActivity.class);
  @Rule
  public GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  @BeforeClass
  public static void beforeClass() {
    IdlingPolicies.setMasterPolicyTimeout(180, TimeUnit.SECONDS);
    IdlingPolicies.setIdlingResourceTimeout(180, TimeUnit.SECONDS);
    Espresso.registerIdlingResources(KiwixIdlingResource.getInstance());
  }

  @Before
  public void setUp() {
  }

  @Test
  public void downloadTest() {
    enterHelp();
    clickOn(R.string.menu_zim_manager);

    clickOn(R.string.local_zims);

    allowPermissionsIfNeeded();

    deleteZimIfExists("ray_charles", R.id.zimfilelist);

    clickOn(R.string.remote_zims);

    try {
      clickOn(R.id.network_permission_button);
    } catch (RuntimeException e) {
      Log.d(KIWIX_DOWNLOAD_TEST, "Failed to click Network Permission Button", e);
    }

    captureAndSaveScreenshot("Before-checking-for-ZimManager-Main-Activity");
    ViewInteraction viewPager2 = onView(
            allOf(withId(R.id.container),
                    withParent(allOf(withId(R.id.zim_manager_main_activity),
                            withParent(withId(android.R.id.content)))),
                    isDisplayed()));
    captureAndSaveScreenshot("After-the-check-completed");

    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    try {
        onData(withContent("ray_charles")).inAdapterView(withId(R.id.library_list));
    } catch (Exception e) {
        fail("Couldn't find downloaded file 'ray_charles'\n\nOriginal Exception:\n" +
            e.getLocalizedMessage() + "\n\n" );
    }

    deleteZimIfExists("ray_charles", R.id.library_list);

    assertDisplayed(R.string.local_zims);
    clickOn(R.string.local_zims);

    try {
      refresh(R.id.zim_swiperefresh);
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

    deleteZimIfExists("ray_charles", R.id.zimfilelist);
  }

  @After
  public void finish() {
    Espresso.unregisterIdlingResources(KiwixIdlingResource.getInstance());
  }

}