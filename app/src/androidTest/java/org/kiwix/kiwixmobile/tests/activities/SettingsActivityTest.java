package org.kiwix.kiwixmobile.tests.activities;

import android.Manifest;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.AndroidJUnit4;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.settings.KiwixSettingsActivity;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SettingsActivityTest {

  @Rule
  public ActivityTestRule<KiwixSettingsActivity> mActivityTestRule = new ActivityTestRule<>(
      KiwixSettingsActivity.class);
  @Rule
  public GrantPermissionRule readPermissionRule =
      GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule =
      GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  @Test
  public void SettingsActivitySimple() {

  }
}
