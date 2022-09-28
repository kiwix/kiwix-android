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
package org.kiwix.kiwixmobile

import android.Manifest
import android.util.Log
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.clickMenu
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import com.adevinta.android.barista.interaction.BaristaSwipeRefreshInteractions.refresh
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.allowStoragePermissionsIfNeeded
import org.kiwix.kiwixmobile.utils.KiwixIdlingResource.Companion.getInstance
import org.kiwix.kiwixmobile.utils.StandardActions
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class DownloadTest {
  @Rule
  var activityTestRule = ActivityTestRule(
    KiwixMainActivity::class.java
  )

  @Rule
  var readPermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)

  @Rule
  var writePermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

  @Before fun setUp() {
    // default
  }

  @Test @Ignore("Broken in 2.5") // TODO: Fix in 3.0
  fun downloadTest() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    clickMenu(TestUtils.getResourceString(R.string.library))
    clickOn(R.string.local_zims)
    allowStoragePermissionsIfNeeded()
    StandardActions.deleteZimIfExists("ray_charles", R.id.zimfilelist)
    clickOn(R.string.remote_zims)
    TestUtils.captureAndSaveScreenshot("Before-checking-for-ZimManager-Main-Activity")
    TestUtils.captureAndSaveScreenshot("After-the-check-completed")
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    try {
      Espresso.onData(TestUtils.withContent("ray_charles"))
        .inAdapterView(ViewMatchers.withId(R.id.libraryList))
    } catch (e: Exception) {
      Assert.fail(
        """
        Couldn't find downloaded file 'ray_charles'
        Original Exception:
        ${e.localizedMessage}
        """.trimIndent()
      )
    }
    StandardActions.deleteZimIfExists("ray_charles", R.id.libraryList)
    assertDisplayed(R.string.local_zims)
    clickOn(R.string.local_zims)
    try {
      refresh(R.id.zim_swiperefresh)
    } catch (e: RuntimeException) {
      Log.w(KIWIX_DOWNLOAD_TEST, "Failed to refresh ZIM list: " + e.localizedMessage)
    }

    /**
     * Commented out the following as it uses another Activity.
     * TODO Once we find a good way to run cross-activity re-implement
     * this functionality in the tests.

     * onData(withContent("ray_charles")).inAdapterView(withId(R.id.zimfilelist)).perform(click());
     * openContextualActionModeOverflowMenu();
     * onView(withText("Get Content")).perform(click());
     */
    StandardActions.deleteZimIfExists("ray_charles", R.id.zimfilelist)
  }

  @After fun finish() {
    IdlingRegistry.getInstance().unregister(getInstance())
  }

  companion object {
    private const val KIWIX_DOWNLOAD_TEST = "kiwixDownloadTest"
    @BeforeClass fun beforeClass() {
      IdlingPolicies.setMasterPolicyTimeout(180, TimeUnit.SECONDS)
      IdlingPolicies.setIdlingResourceTimeout(180, TimeUnit.SECONDS)
      IdlingRegistry.getInstance().register(getInstance())
    }
  }
}
