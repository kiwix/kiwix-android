package org.kiwix.kiwixmobile.tests.activities;

import android.Manifest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.search.SearchActivity;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.AndroidJUnit4;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SearchActivityTest {

  @Rule
  public ActivityTestRule<SearchActivity> mActivityTestRule = new ActivityTestRule<>(
      SearchActivity.class);
  @Rule
  public GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  @Test
  public void SearchActivitySimple() {

  }
}
