package org.kiwix.kiwixmobile.tests;


import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.Is.is;
import static org.kiwix.kiwixmobile.testutils.TestUtils.withContent;
import static org.kiwix.kiwixmobile.utils.StandardActions.enterHelp;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.utils.KiwixIdlingResource;
import org.kiwix.kiwixmobile.utils.SplashActivity;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class DownloadTest {

  @Inject KiwixIdlingResource kiwixIdlingResource;


  @Rule
  public ActivityTestRule<SplashActivity> mActivityTestRule = new ActivityTestRule<>(
      SplashActivity.class);

  @BeforeClass
  public static void beforeClass() {
    IdlingPolicies.setMasterPolicyTimeout(350, TimeUnit.SECONDS);
    IdlingPolicies.setIdlingResourceTimeout(350, TimeUnit.SECONDS);
  }

  @Before
  public void setUp() {
    Espresso.registerIdlingResources(KiwixIdlingResource.getInstance());
  }

  @Test
  public void downloadTest() {
    enterHelp();
    ViewInteraction appCompatButton = onView(
        allOf(withId(R.id.get_content_card), withText("Get Content")));
    appCompatButton.perform(scrollTo(), click());

    ViewInteraction appCompatTextView = onView(
        allOf(withText("Device"), isDisplayed()));
    appCompatTextView.perform(click());

    try {
      onData(withContent("ray_charles")).inAdapterView(withId(R.id.zimfilelist)).perform(longClick());
      onView(withId(android.R.id.button1)).perform(click());
    } catch (RuntimeException e) {

    }

    ViewInteraction appCompatTextView2 = onView(
        allOf(withText("Online"), isDisplayed()));
    appCompatTextView2.perform(click());

    try {
      onView(withId(R.id.network_permission_button)).perform(click());
    } catch (RuntimeException e) {
    }

    ViewInteraction viewPager2 = onView(
        allOf(withId(R.id.container),
            withParent(allOf(withId(R.id.zim_manager_main_activity),
                withParent(withId(android.R.id.content)))),
            isDisplayed()));

    onData(withContent("ray_charles")).inAdapterView(withId(R.id.library_list)).perform(click());

    try {
      onView(withId(android.R.id.button1)).perform(click());
    } catch (RuntimeException e) {
    }

    ViewInteraction appCompatTextView3 = onView(
        allOf(withText("Device"), isDisplayed()));
    appCompatTextView3.perform(click());

    onView(withId(R.id.menu_rescan_fs))
        .perform(click());

    onData(withContent("ray_charles")).inAdapterView(withId(R.id.zimfilelist)).perform(click());

    openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());

    onView(withText("Get Content"))
        .perform(click());

    onData(withContent("ray_charles")).inAdapterView(withId(R.id.zimfilelist)).perform(longClick());

    onView(withId(android.R.id.button1)).perform(click());
  }

  @After
  public void finish() {
    Espresso.unregisterIdlingResources(KiwixIdlingResource.getInstance());
  }

}
