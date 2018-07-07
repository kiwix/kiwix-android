package org.kiwix.kiwixmobile.splash;

import android.Manifest;
import android.support.test.espresso.DataInteraction;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions;
import com.schibsted.spain.barista.rule.BaristaRule;
import com.schibsted.spain.barista.rule.flaky.AllowFlaky;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.main.MainActivity;
import android.webkit.WebView;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS;
import static android.support.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.clearElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.getText;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.hamcrest.Matchers.containsString;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class newtest {

  @Rule
  public BaristaRule<SplashActivity> activityTestRule = BaristaRule.create(SplashActivity.class);
  @Rule
  public GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  @Test
  @AllowFlaky(attempts = 10)
  public void newtest() {
    // Launch the app, starting with the Splash Activity
    activityTestRule.launchActivity();
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    onView(withId(R.id.get_started)).perform(click());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    // Open the Zim file from Assets
    onView(withId(R.id.recycler_view)).perform(actionOnItemAtPosition(1, click()));
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    // Set up the bottom bar
    openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    onView(allOf(withId(R.id.title), withText("Settings"))).perform(click());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    DataInteraction linearLayout = onData(anything())
        .inAdapterView(allOf(withId(android.R.id.list),
            childAtPosition(
                withId(android.R.id.list_container),
                0)))
        .atPosition(5);
    linearLayout.perform(click());
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    pressBack();
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

    //onWebView().forceJavascriptEnabled();

    // Verify that the bottom toolbar and all its buttons are present and assigned correct content description
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    onView(withId(R.id.bottom_toolbar)).check(matches(notNullValue()));

    onView(withId(R.id.bottom_toolbar_bookmark)).check(matches(notNullValue()));
    //onView(withId(R.id.bottom_toolbar_bookmark)).check(matches(withText(R.string.menu_bookmarks)));

    onView(withId(R.id.bottom_toolbar_arrow_back)).check(matches(notNullValue()));
    //onView(withId(R.id.bottom_toolbar_arrow_back)).check(matches(withText(R.string.go_to_previous_page)));

    onView(withId(R.id.bottom_toolbar_home)).check(matches(notNullValue()));
    //onView(withId(R.id.bottom_toolbar_home)).check(matches(withText(R.string.menu_home)));

    onView(withId(R.id.bottom_toolbar_arrow_forward)).check(matches(notNullValue()));
    //onView(withId(R.id.bottom_toolbar_arrow_forward)).check(matches(withText(R.string.go_to_next_page)));

    onView(withId(R.id.bottom_toolbar_toc)).check(matches(notNullValue()));
    //onView(withId(R.id.bottom_toolbar_toc)).check(matches(withText(R.string.table_of_contents)));


    //onWebView(allOf(isDisplayed())).forceJavascriptEnabled();

    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);

/*        onWebView()
            .withElement(findElement(Locator.LINK_TEXT, "link_2")) // similar to onView(withId(...))
            .perform(webClick()) // Similar to perform(click())
*/
    // Test the Home Button
    onWebView(allOf(childAtPosition(
        allOf(withId(R.id.content_frame),
            childAtPosition(
                withId(R.id.drawer_layout),
                0)),
        0),
        isDisplayed()))
        .withElement(findElement(Locator.PARTIAL_LINK_TEXT, "Fool"))
        .perform(webClick());


    // Test the back and the forward buttons

    // Test the Bookmarks button
    // Test adding a bookMark
    // Test removing a bookmark

    // Test the Table of Contents button


    /*
    ViewInteraction appCompatImageView = onView(
        allOf(withId(R.id.bottom_toolbar_home), withContentDescription("Home"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.bottom_toolbar),
                    0),
                2),
            isDisplayed()));
    appCompatImageView.perform(click());

    ViewInteraction view = onView(
        allOf(withId(titleHeading), withContentDescription("Summary"),
            childAtPosition(
                childAtPosition(
                    withContentDescription("Summary"),
                    0),
                0),
            isDisplayed()));
    view.check(matches(isDisplayed()));

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(160000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction appCompatImageView2 = onView(
        allOf(withId(R.id.bottom_toolbar_toc), withContentDescription("Table of contents"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.bottom_toolbar),
                    0),
                4),
            isDisplayed()));
    appCompatImageView2.perform(click());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(160000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction appCompatImageView3 = onView(
        allOf(withId(R.id.bottom_toolbar_toc), withContentDescription("Table of contents"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.bottom_toolbar),
                    0),
                4),
            isDisplayed()));
    appCompatImageView3.perform(click());

    ViewInteraction appCompatImageView4 = onView(
        allOf(withId(R.id.bottom_toolbar_toc), withContentDescription("Table of contents"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.bottom_toolbar),
                    0),
                4),
            isDisplayed()));
    appCompatImageView4.perform(click());

    ViewInteraction appCompatImageView5 = onView(
        allOf(withId(R.id.bottom_toolbar_toc), withContentDescription("Table of contents"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.bottom_toolbar),
                    0),
                4),
            isDisplayed()));
    appCompatImageView5.perform(click());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(160000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction relativeLayout = onView(
        allOf(childAtPosition(
            allOf(withId(R.id.right_drawer_list),
                childAtPosition(
                    withId(R.id.right_drawer),
                    1)),
            0),
            isDisplayed()));
    relativeLayout.check(matches(isDisplayed()));

    ViewInteraction textView = onView(
        allOf(withId(R.id.titleText), withText("Summary"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.right_drawer_list),
                    0),
                0),
            isDisplayed()));
    textView.check(matches(withText("Summary")));

    ViewInteraction appCompatImageView6 = onView(
        allOf(withId(R.id.bottom_toolbar_arrow_back), withContentDescription("Go to previous page"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.bottom_toolbar),
                    0),
                1),
            isDisplayed()));
    appCompatImageView6.perform(click());

    ViewInteraction appCompatImageView7 = onView(
        allOf(withId(R.id.bottom_toolbar_arrow_back), withContentDescription("Go to previous page"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.bottom_toolbar),
                    0),
                1),
            isDisplayed()));
    appCompatImageView7.perform(click());

    ViewInteraction appCompatImageView8 = onView(
        allOf(withId(R.id.bottom_toolbar_arrow_forward), withContentDescription("Go to next page"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.bottom_toolbar),
                    0),
                3),
            isDisplayed()));
    appCompatImageView8.perform(click());

    ViewInteraction appCompatImageView9 = onView(
        allOf(withId(R.id.bottom_toolbar_arrow_forward), withContentDescription("Go to next page"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.bottom_toolbar),
                    0),
                3),
            isDisplayed()));
    appCompatImageView9.perform(click());

    ViewInteraction appCompatImageView10 = onView(
        allOf(withId(R.id.bottom_toolbar_arrow_back), withContentDescription("Go to previous page"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.bottom_toolbar),
                    0),
                1),
            isDisplayed()));
    appCompatImageView10.perform(click());

    ViewInteraction appCompatImageView11 = onView(
        allOf(withId(R.id.bottom_toolbar_arrow_back), withContentDescription("Go to previous page"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.bottom_toolbar),
                    0),
                1),
            isDisplayed()));
    appCompatImageView11.perform(click());

    ViewInteraction view2 = onView(
        allOf(withId(titleHeading), withContentDescription("A Fool for You"),
            childAtPosition(
                childAtPosition(
                    withContentDescription("A Fool for You"),
                    0),
                0),
            isDisplayed()));
    view2.check(matches(isDisplayed()));

    ViewInteraction appCompatImageView12 = onView(
        allOf(withId(R.id.bottom_toolbar_arrow_forward), withContentDescription("Go to next page"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.bottom_toolbar),
                    0),
                3),
            isDisplayed()));
    appCompatImageView12.perform(click());

    ViewInteraction appCompatImageView13 = onView(
        allOf(withId(R.id.bottom_toolbar_arrow_forward), withContentDescription("Go to next page"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.bottom_toolbar),
                    0),
                3),
            isDisplayed()));
    appCompatImageView13.perform(click());

    ViewInteraction view3 = onView(
        allOf(withId(titleHeading), withContentDescription("A Man and His Soul"),
            childAtPosition(
                childAtPosition(
                    withContentDescription("A Man and His Soul"),
                    0),
                0),
            isDisplayed()));
    view3.check(matches(isDisplayed()));

    ViewInteraction appCompatImageView14 = onView(
        allOf(withId(R.id.bottom_toolbar_bookmark), withContentDescription("Bookmarks"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.bottom_toolbar),
                    0),
                0),
            isDisplayed()));
    appCompatImageView14.perform(click());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(320000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction appCompatImageButton = onView(
        allOf(childAtPosition(
            allOf(withId(R.id.toolbar),
                childAtPosition(
                    withId(R.id.toolbar_layout),
                    0)),
            1),
            isDisplayed()));
    appCompatImageButton.perform(click());

    ViewInteraction appCompatImageButton2 = onView(
        allOf(childAtPosition(
            allOf(withId(R.id.toolbar),
                childAtPosition(
                    withId(R.id.toolbar_layout),
                    0)),
            1),
            isDisplayed()));
    appCompatImageButton2.perform(click());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(320000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(320000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction appCompatTextView2 = onView(
        allOf(withId(R.id.title), withText("Bookmarks"),
            childAtPosition(
                childAtPosition(
                    withClassName(is("android.support.v7.view.menu.ListMenuItemView")),
                    0),
                0),
            isDisplayed()));
    appCompatTextView2.perform(click());

    ViewInteraction textView2 = onView(
        allOf(withText("Bookmarks"),
            childAtPosition(
                allOf(withId(R.id.toolbar),
                    childAtPosition(
                        withId(R.id.toolbar_layout),
                        0)),
                1),
            isDisplayed()));
    textView2.check(matches(withText("Bookmarks")));

    ViewInteraction imageButton = onView(
        allOf(withContentDescription("Navigate up"),
            childAtPosition(
                allOf(withId(R.id.toolbar),
                    childAtPosition(
                        withId(R.id.toolbar_layout),
                        0)),
                0),
            isDisplayed()));
    imageButton.check(matches(isDisplayed()));

    ViewInteraction textView3 = onView(
        allOf(withId(R.id.bookmark_title), withText("A Man and His Soul"),
            childAtPosition(
                childAtPosition(
                    IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                    0),
                0),
            isDisplayed()));
    textView3.check(matches(withText("A Man and His Soul")));

    ViewInteraction linearLayout2 = onView(
        allOf(childAtPosition(
            childAtPosition(
                withId(R.id.bookmarks_list),
                0),
            0),
            isDisplayed()));
    linearLayout2.check(matches(isDisplayed()));

    // Added a sleep statement to match the app's execution delay.
    // The recommended way to handle such scenarios is to use Espresso idling resources:
    // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
    try {
      Thread.sleep(320000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ViewInteraction appCompatImageButton3 = onView(
        allOf(withContentDescription("Navigate up"),
            childAtPosition(
                allOf(withId(R.id.toolbar),
                    childAtPosition(
                        withId(R.id.toolbar_layout),
                        0)),
                1),
            isDisplayed()));
    appCompatImageButton3.perform(click());
    */
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
