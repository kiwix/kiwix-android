package org.kiwix.kiwixmobile

import android.Manifest.permission
import android.app.Activity
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule

abstract class BaseActivityTest<T: Activity> {
  @get:Rule
  abstract var activityRule: ActivityTestRule<T>
  @get:Rule
  var readPermissionRule =
    GrantPermissionRule.grant(permission.READ_EXTERNAL_STORAGE)
  @get:Rule
  var writePermissionRule =
    GrantPermissionRule.grant(permission.WRITE_EXTERNAL_STORAGE)

  inline fun <reified T : Activity> activityTestRule() =
    ActivityTestRule(T::class.java)
}
