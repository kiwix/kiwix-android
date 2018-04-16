package org.kiwix.kiwixmobile.tests.activities;

import android.Manifest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.bookmarks_view.BookmarksActivity;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class BookmarksActivityTest {

    @Rule
    public ActivityTestRule<BookmarksActivity> mActivityTestRule = new ActivityTestRule<>(
            BookmarksActivity.class);
    @Rule
    public GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
    @Rule
    public GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    public void BookmarksActivitySimple() {

    }

}
