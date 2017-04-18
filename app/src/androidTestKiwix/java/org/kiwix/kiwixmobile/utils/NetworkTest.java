package org.kiwix.kiwixmobile.utils;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.kiwix.kiwixmobile.testutils.TestUtils.withContent;

import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import java.io.IOException;
import java.io.InputStream;
import javax.inject.Inject;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
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

/**
 * Created by mhutti1 on 14/04/17.
 */

public class NetworkTest {

  @Inject OkHttpClient okHttpClient;
  @Inject MockWebServer mockWebServer;


  @Rule
  public ActivityTestRule<KiwixMobileActivity> mActivityTestRule = new ActivityTestRule<>(
      KiwixMobileActivity.class, false, false);

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
    try {
      byte[] libraryBytes = IOUtils.toByteArray(library);
      mockWebServer.enqueue(new MockResponse().setBody(new String(libraryBytes)));
      byte[] metalinkBytes = IOUtils.toByteArray(metalinks);
      mockWebServer.enqueue(new MockResponse().setBody(new String(metalinkBytes)));
      mockWebServer.enqueue(new MockResponse().setHeader("Content-Length", 63973123));
      Buffer buffer = new Buffer();
      buffer.write(new byte[63973123]);
      buffer.close();
      mockWebServer.enqueue(new MockResponse().setBody(buffer));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  @Test
  public void zimTest() {

    mActivityTestRule.launchActivity(null);

    ViewInteraction appCompatButton = onView(
        allOf(withId(R.id.get_content_card), withText("Get Content")));
    appCompatButton.perform(scrollTo(), click());

    TestUtils.allowPermissionsIfNeeded();


    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction linearLayout = onView(
        allOf(childAtPosition(
            withId(R.id.library_list),
            0),
            isDisplayed()));
    linearLayout.perform(click());


    try {
      Thread.sleep(16000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction viewPager = onView(
        allOf(withId(R.id.container),
            withParent(allOf(withId(R.id.zim_manager_main_activity),
                withParent(withId(android.R.id.content)))),
            isDisplayed()));
    viewPager.perform(swipeLeft());

    ViewInteraction viewPager2 = onView(
        allOf(withId(R.id.container),
            withParent(allOf(withId(R.id.zim_manager_main_activity),
                withParent(withId(android.R.id.content)))),
            isDisplayed()));
    viewPager2.perform(swipeRight());

    ViewInteraction viewPager3 = onView(
        allOf(withId(R.id.container),
            withParent(allOf(withId(R.id.zim_manager_main_activity),
                withParent(withId(android.R.id.content)))),
            isDisplayed()));
    viewPager3.perform(swipeRight());

    onView(withId(R.id.menu_rescan_fs))
        .perform(click());

    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction linearLayout2 = onView(
        allOf(childAtPosition(
            withId(R.id.zimfilelist),
            0),
            isDisplayed()));
    linearLayout2.perform(click());

    openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());

    onView(withText("Get Content"))
        .perform(click());

    try {
      Thread.sleep(6000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction linearLayout4 = onView(
        allOf(childAtPosition(
            withId(R.id.zimfilelist),
            0),
            isDisplayed()));
    linearLayout2.perform(longClick());
    onView(withId(android.R.id.button1)).perform(click());
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
