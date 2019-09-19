package org.kiwix.kiwixmobile

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.di.components.DaggerTestComponent

@RunWith(AndroidJUnit4::class)
abstract class BaseActivityTest<T : Activity> {
  @get:Rule
  abstract var activityRule: ActivityTestRule<T>
  @get:Rule
  var readPermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(permission.READ_EXTERNAL_STORAGE)
  @get:Rule
  var writePermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(permission.WRITE_EXTERNAL_STORAGE)

  val context: Context by lazy {
    getInstrumentation().targetContext.applicationContext
  }

  inline fun <reified T : Activity> activityTestRule(noinline beforeActivityAction: (() -> Unit)? = null) =
    object : ActivityTestRule<T>(T::class.java) {
      override fun beforeActivityLaunched() {
        super.beforeActivityLaunched()
        beforeActivityAction?.invoke()
      }
    }

  protected fun testComponent() = DaggerTestComponent.builder()
    .context(context)
    .build()
}
