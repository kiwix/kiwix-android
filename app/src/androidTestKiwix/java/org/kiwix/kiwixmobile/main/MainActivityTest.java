package org.kiwix.kiwixmobile.main;

import android.Manifest;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.schibsted.spain.barista.interaction.BaristaMenuClickInteractions;
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions;
import com.schibsted.spain.barista.rule.BaristaRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.R;

import static com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn;
import static org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS;
import static org.kiwix.kiwixmobile.testutils.TestUtils.getResourceString;
import static org.kiwix.kiwixmobile.utils.StandardActions.enterHelp;
import static org.kiwix.kiwixmobile.utils.StandardActions.enterSettings;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

  @Rule
  public BaristaRule<MainActivity> activityTestRule = BaristaRule.create(MainActivity.class);
  @Rule
  public GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  @Test
  public void MainActivitySimple() {

  }

  @Test
  public void navigateHelp() {
    activityTestRule.launchActivity();
    enterHelp();
  }

  @Test
  public void navigateSettings() {
    activityTestRule.launchActivity();
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    enterSettings();
  }

  @Test
  public void navigateBookmarks() {
    activityTestRule.launchActivity();
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    BaristaMenuClickInteractions.clickMenu(getResourceString(R.string.menu_bookmarks));
  }

  @Test
  public void navigateDeviceContent() {
    activityTestRule.launchActivity();
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    BaristaMenuClickInteractions.clickMenu(getResourceString(R.string.menu_zim_manager));
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    clickOn(R.string.local_zims);
  }

  @Test
  public void navigateOnlineContent() {
    activityTestRule.launchActivity();
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    BaristaMenuClickInteractions.clickMenu(getResourceString(R.string.menu_zim_manager));
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    clickOn(R.string.remote_zims);
  }

  @Test
  public void navigateDownloadingContent() {
    activityTestRule.launchActivity();
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    BaristaMenuClickInteractions.clickMenu(getResourceString(R.string.menu_zim_manager));
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    clickOn(R.string.zim_downloads);
  }
}
