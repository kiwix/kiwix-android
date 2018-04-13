/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.tests;

import android.Manifest;
import android.support.test.espresso.DataInteraction;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.ZimContentProvider;
import org.kiwix.kiwixmobile.di.components.DaggerTestComponent;
import org.kiwix.kiwixmobile.di.components.TestComponent;
import org.kiwix.kiwixmobile.di.modules.ApplicationModule;
import org.kiwix.kiwixmobile.testutils.TestUtils;
import org.kiwix.kiwixmobile.utils.KiwixIdlingResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn;
import static com.schibsted.spain.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton;
import static com.schibsted.spain.barista.interaction.BaristaMenuClickInteractions.clickMenu;
import static com.schibsted.spain.barista.interaction.BaristaSwipeRefreshInteractions.refresh;
import static org.hamcrest.Matchers.allOf;
import static org.kiwix.kiwixmobile.testutils.TestUtils.withContent;
import static org.kiwix.kiwixmobile.utils.StandardActions.enterHelp;

/**
 * Created by mhutti1 on 14/04/17.
 */

public class NetworkTest {
  private static String NETWORK_TEST_TAG = "KiwixNetworkTest";

  @Inject MockWebServer mockWebServer;


  @Rule
  public ActivityTestRule<KiwixMobileActivity> mActivityTestRule = new ActivityTestRule<>(
      KiwixMobileActivity.class, false, false);
  @Rule
  public GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  @BeforeClass
  public static void beforeClass() {
    IdlingPolicies.setMasterPolicyTimeout(180, TimeUnit.SECONDS);
    IdlingPolicies.setIdlingResourceTimeout(180, TimeUnit.SECONDS);
    Espresso.registerIdlingResources(KiwixIdlingResource.getInstance());
  }

  @Before
  public void setUp() {

    TestComponent component = DaggerTestComponent.builder().applicationModule
        (new ApplicationModule(
            (KiwixApplication) getInstrumentation().getTargetContext().getApplicationContext())).build();

    ((KiwixApplication) getInstrumentation().getTargetContext().getApplicationContext()).setApplicationComponent(component);

    new ZimContentProvider().setupDagger();
    component.inject(this);
    InputStream library = NetworkTest.class.getClassLoader().getResourceAsStream("library.xml");
    InputStream metalinks = NetworkTest.class.getClassLoader().getResourceAsStream("test.zim.meta4");
    InputStream testzim = NetworkTest.class.getClassLoader().getResourceAsStream("testzim.zim");
    try {
      byte[] libraryBytes = IOUtils.toByteArray(library);
      mockWebServer.enqueue(new MockResponse().setBody(new String(libraryBytes)));
      byte[] metalinkBytes = IOUtils.toByteArray(metalinks);
      mockWebServer.enqueue(new MockResponse().setBody(new String(metalinkBytes)));
      mockWebServer.enqueue(new MockResponse().setHeader("Content-Length", 357269));
      Buffer buffer = new Buffer();
      buffer.write(IOUtils.toByteArray(testzim));
      buffer.close();
      mockWebServer.enqueue(new MockResponse().setBody(buffer));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  @Test
  public void networkTest() {

    mActivityTestRule.launchActivity(null);

    enterHelp();
    clickOn(R.string.menu_zim_manager);

    TestUtils.allowPermissionsIfNeeded();

    try {
      onView(withId(R.id.network_permission_button)).perform(click());
    } catch (RuntimeException e) {
    }

    onData(withContent("wikipedia_ab_all_2017-03")).inAdapterView(withId(R.id.library_list)).perform(click());

    try {
      onView(withId(android.R.id.button1)).perform(click());
    } catch (RuntimeException e) {
    }

    clickOn(R.string.local_zims);

    try {
      onData(allOf(withId(R.id.zim_swiperefresh)));
      refresh(R.id.zim_swiperefresh);
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }


    // Commented out the following which assumes only 1 match - not always safe to assume as there may
    // already be a similar file on the device.
    // onData(withContent("wikipedia_ab_all_2017-03")).inAdapterView(withId(R.id.zimfilelist)).perform(click());

    // Find matching zim files on the device
    try {
      DataInteraction dataInteraction = onData(withContent("wikipedia_ab_all_2017-03")).inAdapterView(withId(R.id.zimfilelist));
      // TODO how can we get a count of the items matching the dataInteraction?
      dataInteraction.atPosition(0).perform(click());

      clickMenu(R.string.menu_zim_manager);

      DataInteraction dataInteraction1 = onData(withContent("wikipedia_ab_all_2017-03")).inAdapterView(withId(R.id.zimfilelist));
      dataInteraction1.atPosition(0).perform(longClick()); // to delete the zim file
      clickDialogPositiveButton();
    } catch (Exception e) {
      Log.w(NETWORK_TEST_TAG, "failed to interact with local ZIM file: " + e.getLocalizedMessage());
    }
  }

  @After
  public void finish() {
    Espresso.unregisterIdlingResources(KiwixIdlingResource.getInstance());
  }
}
