package org.kiwix.kiwixmobile.tests.activities;

import android.Manifest;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
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
import org.kiwix.kiwixmobile.utils.SplashActivity;

import static com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn;
import static org.kiwix.kiwixmobile.testutils.TestUtils.getResourceString;
import static org.kiwix.kiwixmobile.utils.StandardActions.enterHelp;
import static org.kiwix.kiwixmobile.utils.StandardActions.enterSettings;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SplashActivityTest {

    @Rule
    public BaristaRule<SplashActivity> mActivityTestRule = BaristaRule.create(SplashActivity.class);
    @Rule
    public GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
    @Rule
    public GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    public void empty() {

    }

    @Test
    public void navigateHelp() {
        mActivityTestRule.launchActivity();
        enterHelp();
    }

    /*
    This file contains various instances of:
        BaristaSleepInteractions.sleep(250);
    The number 250 is fairly arbitrary. I found 100 to be insufficient, and 250 seems to work on all
    devices I've tried.

    The line combats an intermittent issue caused by tests executing before the app/activity is ready.
    This isn't necessary on all devices (particularly more recent ones), however I'm unsure if
    it's speed related, or Android Version related.
     */

    @Test
    public void navigateSettings() {
        mActivityTestRule.launchActivity();
        BaristaSleepInteractions.sleep(250);
        enterSettings();
    }

    @Test
    public void navigateBookmarks() {
        mActivityTestRule.launchActivity();
        BaristaSleepInteractions.sleep(250);
        BaristaMenuClickInteractions.clickMenu(getResourceString(R.string.menu_bookmarks));
    }

    @Test
    public void navigateDeviceContent() {
        mActivityTestRule.launchActivity();
        BaristaSleepInteractions.sleep(250);
        BaristaMenuClickInteractions.clickMenu(getResourceString(R.string.menu_zim_manager));
        BaristaSleepInteractions.sleep(250);
        clickOn(R.string.local_zims);
    }

    @Test
    public void navigateOnlineContent() {
        mActivityTestRule.launchActivity();
        BaristaSleepInteractions.sleep(250);
        BaristaMenuClickInteractions.clickMenu(getResourceString(R.string.menu_zim_manager));
        BaristaSleepInteractions.sleep(250);
        clickOn(R.string.remote_zims);
    }

    @Test
    public void navigateDownloadingContent() {
        mActivityTestRule.launchActivity();
        BaristaSleepInteractions.sleep(250);
        BaristaMenuClickInteractions.clickMenu(getResourceString(R.string.menu_zim_manager));
        BaristaSleepInteractions.sleep(250);
        clickOn(R.string.zim_downloads);
    }

}
