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

package org.kiwix.kiwixmobile.settings;

import android.Manifest;
import android.view.View;
import androidx.annotation.StringRes;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions;
import org.hamcrest.Matcher;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.kiwix.kiwixmobile.core.R;
import org.kiwix.kiwixmobile.main.KiwixMainActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed;
import static org.hamcrest.Matchers.anyOf;
import static org.kiwix.kiwixmobile.testutils.TestUtils.TEST_PAUSE_MS;
import static org.kiwix.kiwixmobile.utils.StandardActions.enterSettings;
import static org.kiwix.kiwixmobile.utils.StandardActions.openDrawer;

public class KiwixSettingsFragmentTest {
  @Rule
  public ActivityTestRule<KiwixMainActivity> activityTestRule =
    new ActivityTestRule<>(KiwixMainActivity.class);
  @Rule
  public GrantPermissionRule readPermissionRule =
    GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
  @Rule
  public GrantPermissionRule writePermissionRule =
    GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  @Before
  public void setup() {
    BaristaSleepInteractions.sleep(TEST_PAUSE_MS);
    openDrawer();
    enterSettings();
  }

  @Test
  public void testToggle() {
    clickOn(R.string.pref_back_to_top);
    clickOn(R.string.pref_newtab_background_title);
    clickOn(R.string.pref_external_link_popup_title);
    clickOn(R.string.pref_wifi_only);
  }

  private void clickOn(@StringRes int... stringIds) {
    Matcher<View>[] matchers = new Matcher[stringIds.length];
    for (int i = 0; i < stringIds.length; i++) {
      matchers[i] = withText(stringIds[i]);
    }
    onView(withId(R.id.recycler_view))
      .perform(actionOnItem(hasDescendant(anyOf(matchers)), click()));
  }

  @Test
  public void testLanguageDialog() {
    clickOn(R.string.device_default);
    assertDisplayed(R.string.pref_language_title);
  }

  @Test
  public void testStorageDialog() {
    clickOn(R.string.internal_storage, R.string.external_storage);
    assertDisplayed(R.string.pref_storage);
  }

  @Test
  public void testHistoryDialog() {
    clickOn(R.string.pref_clear_all_history_title);
    assertDisplayed(R.string.clear_all_history_dialog_title);
  }

  @Test
  public void testNightModeDialog() {
    clickOn(R.string.pref_night_mode);
    for (String nightModeString : nightModeStrings()) {
      assertDisplayed(nightModeString);
    }
  }

  @NotNull private String[] nightModeStrings() {
    return activityTestRule.getActivity()
      .getResources()
      .getStringArray(R.array.pref_night_modes_entries);
  }
}

