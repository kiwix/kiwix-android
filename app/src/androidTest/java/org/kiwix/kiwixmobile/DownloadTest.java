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

package org.kiwix.kiwixmobile;

import android.Manifest;
import android.util.Log;
import androidx.test.espresso.IdlingPolicies;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import com.schibsted.spain.barista.interaction.BaristaMenuClickInteractions;
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.main.MainActivity;
import org.kiwix.kiwixmobile.utils.KiwixIdlingResource;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed;
import static com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn;
import static com.schibsted.spain.barista.interaction.BaristaSwipeRefreshInteractions.refresh;
import static junit.framework.Assert.fail;
import static org.hamcrest.Matchers.allOf;
import static org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS;
import static org.kiwix.kiwixmobile.testutils.TestUtils.allowPermissionsIfNeeded;
import static org.kiwix.kiwixmobile.testutils.TestUtils.captureAndSaveScreenshot;
import static org.kiwix.kiwixmobile.testutils.TestUtils.getResourceString;
import static org.kiwix.kiwixmobile.testutils.TestUtils.withContent;
import static org.kiwix.kiwixmobile.utils.StandardActions.deleteZimIfExists;


@LargeTest
@RunWith(AndroidJUnit4.class)
public class DownloadTest {

  private static final String KIWIX_DOWNLOAD_TEST = "kiwixDownloadTest";
  @Rule
  public ActivityTestRule<MainActivity> activityTestRule =
      new ActivityTestRule<>(MainActivity.class);
  @Rule
  public GrantPermissionRule readPermissionRule =
      GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule =
      GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  @BeforeClass
  public static void beforeClass() {
    IdlingPolicies.setMasterPolicyTimeout(180, TimeUnit.SECONDS);
    IdlingPolicies.setIdlingResourceTimeout(180, TimeUnit.SECONDS);
    IdlingRegistry.getInstance().register(KiwixIdlingResource.getInstance());
  }

  @Before
  public void setUp() {
  }

  @Test
  @Ignore("Broken in 2.5")//TODO: Fix in 3.0
  public void downloadTest() {
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    BaristaMenuClickInteractions.clickMenu(getResourceString(R.string.menu_zim_manager));

    clickOn(R.string.local_zims);

    allowPermissionsIfNeeded();

    deleteZimIfExists("ray_charles", R.id.zimfilelist);

    clickOn(R.string.remote_zims);

    captureAndSaveScreenshot("Before-checking-for-ZimManager-Main-Activity");
    ViewInteraction viewPager2 = onView(
            allOf(withId(R.id.manageViewPager),
                    withParent(allOf(withId(R.id.zim_manager_main_activity),
                            withParent(withId(android.R.id.content)))),
                    isDisplayed()));
    captureAndSaveScreenshot("After-the-check-completed");

    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    try {
        onData(withContent("ray_charles")).inAdapterView(withId(R.id.libraryList));
    } catch (Exception e) {
      fail("Couldn't find downloaded file 'ray_charles'\n\nOriginal Exception:\n" +
          e.getLocalizedMessage() + "\n\n");
    }

    deleteZimIfExists("ray_charles", R.id.libraryList);

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
    IdlingRegistry.getInstance().unregister(KiwixIdlingResource.getInstance());
  }
}
