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
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
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
  }

  @Before
  public void setUp() {
    Espresso.registerIdlingResources(KiwixIdlingResource.getInstance());

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

    ViewInteraction linearLayout = onView(
        allOf(childAtPosition(
            withId(R.id.library_list),
            0),
            isDisplayed()));
    linearLayout.perform(click());

    try {
      onView(withId(android.R.id.button1)).perform(click());
    } catch (RuntimeException e) {
    }


    onView(withText(R.string.local_zims))
        .perform(click());

    onView(withId(R.id.menu_rescan_fs))
        .perform(click());


    ViewInteraction linearLayout2 = onView(
        allOf(childAtPosition(
            withId(R.id.zimfilelist),
            0),
            isDisplayed()));
    linearLayout2.perform(click());

    openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());

    onView(withText(R.string.menu_zim_manager))
        .perform(click());

    ViewInteraction linearLayout4 = onView(
        allOf(childAtPosition(
            withId(R.id.zimfilelist),
            0),
            isDisplayed()));
    linearLayout2.perform(longClick());
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
