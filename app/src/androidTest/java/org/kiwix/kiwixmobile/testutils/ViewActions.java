package org.kiwix.kiwixmobile.testutils;

import android.view.View;
import android.widget.Checkable;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

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
