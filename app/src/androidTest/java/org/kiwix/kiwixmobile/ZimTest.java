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
package org.kiwix.kiwixmobile;

//@LargeTest
//@RunWith(AndroidJUnit4.class)
public class ZimTest {

  /*
  Temporary Disabled
  @Inject
  Context context;

  @Rule
  public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(
      MainActivity.class, false, false);

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

    try {
      onView(withId(R.id.menu_home)).perform(click());
    } catch (NoMatchingViewException e) {
      openContextualActionModeOverflowMenu();
      onView(withText("Home")).perform(click());
    }
    
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
  */
}
