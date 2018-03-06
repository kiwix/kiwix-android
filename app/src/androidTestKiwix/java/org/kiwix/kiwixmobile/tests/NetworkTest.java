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

import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
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

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openContextualActionModeOverflowMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.kiwix.kiwixmobile.testutils.TestUtils.withContent;
import static org.kiwix.kiwixmobile.utils.StandardActions.enterHelp;

/**
 * Created by mhutti1 on 14/04/17.
 */

public class NetworkTest {

  @Inject MockWebServer mockWebServer;


  @Rule
  public ActivityTestRule<KiwixMobileActivity> mActivityTestRule = new ActivityTestRule<>(
      KiwixMobileActivity.class, false, false);

  @BeforeClass
  public static void beforeClass() {
    IdlingPolicies.setMasterPolicyTimeout(350, TimeUnit.SECONDS);
    IdlingPolicies.setIdlingResourceTimeout(350, TimeUnit.SECONDS);
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
    ViewInteraction appCompatButton = onView(
        allOf(withId(R.id.get_content_card), withText("Get Content")));
    appCompatButton.perform(scrollTo(), click());

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


    onView(withText(R.string.local_zims))
        .perform(click());

    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    onView(withId(R.id.zim_swiperefresh))
        .perform(swipeDown());


    onData(withContent("wikipedia_ab_all_2017-03")).inAdapterView(withId(R.id.zimfilelist)).perform(click());


    openContextualActionModeOverflowMenu();

    onView(withText(R.string.menu_zim_manager))
        .perform(click());

    onData(withContent("wikipedia_ab_all_2017-03")).inAdapterView(withId(R.id.zimfilelist)).perform(longClick());

    onView(withId(android.R.id.button1)).perform(click());
  }

  @After
  public void finish() {
    Espresso.unregisterIdlingResources(KiwixIdlingResource.getInstance());
  }


  private static Matcher<View> childAtPosition(
      final Matcher<View> parentMatcher, final int position) {

    return new TypeSafeMatcher<View>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("Child at position " + position + " in parent ");
        parentMatcher.describeTo(description);
      }

      @Override
      public boolean matchesSafely(View view) {
        ViewParent parent = view.getParent();
        return parent instanceof ViewGroup && parentMatcher.matches(parent)
            && view.equals(((ViewGroup) parent).getChildAt(position));
      }
    };
  }
}
