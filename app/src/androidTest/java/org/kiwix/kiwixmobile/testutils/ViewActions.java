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

package org.kiwix.kiwixmobile.testutils;

import android.view.View;
import android.widget.Checkable;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import static org.hamcrest.CoreMatchers.isA;

public class ViewActions {
  public static ViewAction setChecked(final boolean checked) {
    return new ViewAction() {
      @Override
      public BaseMatcher<View> getConstraints() {
        return new BaseMatcher<View>() {
          @Override
          public boolean matches(Object item) {
            return isA(Checkable.class).matches(item);
          }

          @Override
          public void describeMismatch(Object item, Description mismatchDescription) {
          }

          @Override
          public void describeTo(Description description) {
          }
        };
      }

      @Override
      public String getDescription() {
        return null;
      }

      @Override
      public void perform(UiController uiController, View view) {
        Checkable checkableView = (Checkable) view;
        checkableView.setChecked(checked);
      }
    };
  }
}
