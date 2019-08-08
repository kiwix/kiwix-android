package org.kiwix.kiwixmobile.settings;
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

import android.preference.Preference;
import androidx.test.rule.ActivityTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.main.MainActivity;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.PreferenceMatchers.withKey;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.kiwix.kiwixmobile.utils.StandardActions.enterSettings;

public class KiwixSettingsActivityTest{
  @Rule
  public ActivityTestRule<MainActivity> activityTestRule =
      new ActivityTestRule<>(MainActivity.class);

  @Test
  public void testToggle() {
    enterSettings();
    onData(allOf(
        is(instanceOf(Preference.class)),
        withKey("pref_nightmode")))
        .perform(click());

    onData(allOf(
        is(instanceOf(Preference.class)),
        withKey("pref_auto_nightmode")))
        .perform(click());

    onData(allOf(
        is(instanceOf(Preference.class)),
        withKey("pref_backtotop")))
        .perform(click());

    onData(allOf(
        is(instanceOf(Preference.class)),
        withKey("pref_hidetoolbar")))
        .perform(click());


    onData(allOf(
        is(instanceOf(Preference.class)),
        withKey("pref_newtab_background")))
        .perform(click());

    onData(allOf(
        is(instanceOf(Preference.class)),
        withKey("pref_external_link_popup")))
        .perform(click());

    /*onData(allOf(
        is(instanceOf(Preference.class)),
        withKey("pref_full_text_search")))
        .perform(click());*/

    onData(allOf(
        is(instanceOf(Preference.class)),
        withKey("pref_wifi_only")))
        .perform(click());

    onData(allOf(
        is(instanceOf(Preference.class)),
        withKey("pref_zoom_enabled")))
        .perform(click());
  }

  @Test
  public void testZoomDialog() {
    enterSettings();
    onData(allOf(
        is(instanceOf(Preference.class)),
        withKey("pref_zoom_enabled")))
        .perform(click());

    onData(allOf(
        is(instanceOf(Preference.class)),
        withKey("pref_zoom_slider")))
        .perform(click());

    assertDisplayed(R.string.pref_zoom_dialog);
  }

  @Test
  public void testLanguageDialog() {
    enterSettings();
    onData(allOf(
        is(instanceOf(Preference.class)),
        withKey("pref_language_chooser")))
        .perform(click());

    assertDisplayed(R.string.pref_language_title);
  }

  @Test
  public void testStorageDialog() {
    enterSettings();
    onData(allOf(
        is(instanceOf(Preference.class)),
        withKey("pref_select_folder")))
        .perform(click());

    assertDisplayed(R.string.pref_storage);
  }

  @Test
  public void testHistoryDialog() {
    enterSettings();
    onData(allOf(
        is(instanceOf(Preference.class)),
        withKey("pref_clear_all_history")))
        .perform(click());

    assertDisplayed(R.string.clear_all_history_dialog_title);
  }
}

