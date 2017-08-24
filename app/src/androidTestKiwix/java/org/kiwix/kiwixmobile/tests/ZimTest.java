package org.kiwix.kiwixmobile.tests;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.ZimContentProvider;
import org.kiwix.kiwixmobile.di.components.DaggerTestComponent;
import org.kiwix.kiwixmobile.di.components.TestComponent;
import org.kiwix.kiwixmobile.di.modules.ApplicationModule;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openContextualActionModeOverflowMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ZimTest {

  @Inject
  Context context;

  @Rule
  public ActivityTestRule<KiwixMobileActivity> mActivityTestRule = new ActivityTestRule<>(
      KiwixMobileActivity.class, false, false);

  @Before public void setUp() {
    TestComponent component = DaggerTestComponent.builder().applicationModule
        (new ApplicationModule(
            (KiwixApplication) getInstrumentation().getTargetContext().getApplicationContext())).build();

    ((KiwixApplication) getInstrumentation().getTargetContext().getApplicationContext()).setApplicationComponent(component);

    component.inject(this);
    new ZimContentProvider().setupDagger();
  }

  @Test
  @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
  public void zimTest() {
    Intent intent = new Intent();
    File file = new File(context.getFilesDir(), "test.zim");
    try {
      file.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }
    intent.setData(Uri.fromFile(file));

    mActivityTestRule.launchActivity(intent);

    openContextualActionModeOverflowMenu();

    onView(withText("Home"))
        .perform(click());

    onWebView().withElement(findElement(Locator.LINK_TEXT, "A Fool for You"));

    onView(withId(R.id.drawer_layout)).perform(DrawerActions.open(Gravity.RIGHT));

    ViewInteraction textView = onView(
        allOf(withId(R.id.titleText), withText("Summary"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.right_drawer_list),
                    0),
                0),
            isDisplayed()));
    textView.check(matches(withText("Summary")));

    onView(withId(R.id.drawer_layout)).perform(DrawerActions.close(Gravity.RIGHT));

    onWebView().withElement(findElement(Locator.LINK_TEXT, "A Fool for You")).perform(webClick());

    onView(withId(R.id.drawer_layout)).perform(DrawerActions.open(Gravity.RIGHT));

    ViewInteraction textView2 = onView(
        allOf(withId(R.id.titleText), withText("A Fool for You"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.right_drawer_list),
                    0),
                0),
            isDisplayed()));
    textView2.check(matches(withText("A Fool for You")));

    ViewInteraction textView3 = onView(
        allOf(withId(R.id.titleText), withText("Personnel"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.right_drawer_list),
                    1),
                0),
            isDisplayed()));
    textView3.check(matches(withText("Personnel")));

    ViewInteraction textView4 = onView(
        allOf(withId(R.id.titleText), withText("Covers"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.right_drawer_list),
                    2),
                0),
            isDisplayed()));
    textView4.check(matches(withText("Covers")));

    openContextualActionModeOverflowMenu();

    onView(withText("Help"))
        .perform(click());
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
