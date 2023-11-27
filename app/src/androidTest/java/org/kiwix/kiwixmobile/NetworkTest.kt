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
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.clickMenu
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import com.adevinta.android.barista.interaction.BaristaSwipeRefreshInteractions.refresh
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.CoreApp.Companion.coreComponent
import org.kiwix.kiwixmobile.core.di.components.DaggerTestComponent
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.utils.KiwixIdlingResource.Companion.getInstance
import java.util.concurrent.TimeUnit
import org.kiwix.kiwixmobile.core.R.string

/**
 * Created by mhutti1 on 14/04/17.
 */
@RunWith(AndroidJUnit4::class)
class NetworkTest {

  // @Inject
  // MockWebServer mockWebServer

  private val permissions = arrayOf(
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE
  )

  @Rule
  @JvmField
  var permissionRules: GrantPermissionRule =
    GrantPermissionRule.grant(*permissions)

  @Before fun setUp() {
    val component = DaggerTestComponent.builder().context(
      InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    ).build()
    coreComponent = component
    component.inject(this)
    val library = NetworkTest::class.java.classLoader.getResourceAsStream("library.xml")
    val metalinks = NetworkTest::class.java.classLoader.getResourceAsStream("test.zim.meta4")
    val testzim = NetworkTest::class.java.classLoader.getResourceAsStream("testzim.zim")
    // try {
    //  byte[] libraryBytes = IOUtils.toByteArray(library);
    //  mockWebServer.enqueue(new MockResponse().setBody(new String(libraryBytes)));
    //  byte[] metalinkBytes = IOUtils.toByteArray(metalinks);
    //  mockWebServer.enqueue(new MockResponse().setBody(new String(metalinkBytes)));
    //  mockWebServer.enqueue(new MockResponse().setHeader("Content-Length", 357269));
    //  Buffer buffer = new Buffer();
    //  buffer.write(IOUtils.toByteArray(testzim));
    //  buffer.close();
    //  mockWebServer.enqueue(new MockResponse().setBody(buffer));
    // } catch (IOException e) {
    //  e.printStackTrace();
    // }
  }

  @Test @Ignore("Broken in 2.5") // TODO Fix in 3.0
  fun networkTest() {
    ActivityScenario.launch(KiwixMainActivity::class.java)
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    clickMenu(TestUtils.getResourceString(string.library))
    TestUtils.allowStoragePermissionsIfNeeded()
    Espresso.onData(TestUtils.withContent("wikipedia_ab_all_2017-03"))
      .inAdapterView(ViewMatchers.withId(R.id.libraryList))
      .perform(ViewActions.click())
    try {
      Espresso.onView(ViewMatchers.withId(android.R.id.button1)).perform(ViewActions.click())
    } catch (e: RuntimeException) {
      Log.w(NETWORK_TEST_TAG, "failed to perform click action on the view : ${e.localizedMessage} ")
    }
    clickOn(string.local_zims)
    try {
      Espresso.onData(CoreMatchers.allOf(ViewMatchers.withId(R.id.zim_swiperefresh)))
      refresh(R.id.zim_swiperefresh)
      Thread.sleep(500)
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }

    // Commented out the following which assumes only 1 match - not always safe to assume as there may
    // already be a similar file on the device.
    // onData(withContent("wikipedia_ab_all_2017-03")).inAdapterView(withId(R.id.zimfilelist)).perform(click());

    // Find matching zim files on the device
    try {
      val dataInteraction = Espresso.onData(TestUtils.withContent("wikipedia_ab_all_2017-03"))
        .inAdapterView(ViewMatchers.withId(R.id.zimfilelist))
      // TODO how can we get a count of the items matching the dataInteraction?
      dataInteraction.atPosition(0).perform(ViewActions.click())
      clickMenu(string.library)
      val dataInteraction1 = Espresso.onData(TestUtils.withContent("wikipedia_ab_all_2017-03"))
        .inAdapterView(ViewMatchers.withId(R.id.zimfilelist))
      dataInteraction1.atPosition(0).perform(ViewActions.longClick()) // to delete the zim file
      BaristaDialogInteractions.clickDialogPositiveButton()
    } catch (e: Exception) {
      Log.w(NETWORK_TEST_TAG, "failed to interact with local ZIM file: " + e.localizedMessage)
    }
  }

  @After fun finish() {
    IdlingRegistry.getInstance().unregister(getInstance())
  }

  companion object {
    private const val NETWORK_TEST_TAG = "KiwixNetworkTest"

    @BeforeClass fun beforeClass() {
      IdlingPolicies.setMasterPolicyTimeout(180, TimeUnit.SECONDS)
      IdlingPolicies.setIdlingResourceTimeout(180, TimeUnit.SECONDS)
      IdlingRegistry.getInstance().register(getInstance())
    }
  }
}
