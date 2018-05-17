package org.kiwix.kiwixmobile.tests.activities;

import android.Manifest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.modules.zimmanager.ZimManageActivity;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ZimManageActivityTest {

    @Rule
    public ActivityTestRule<ZimManageActivity> mActivityTestRule = new ActivityTestRule<>(
            ZimManageActivity.class);
    @Rule
    public GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
    @Rule
    public GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    public void ZimManageActivitySimple() {

    }

}
