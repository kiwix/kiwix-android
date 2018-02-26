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
package org.kiwix.kiwixmobile.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.di.components.ApplicationComponent;

public abstract class BaseActivity extends AppCompatActivity {


  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setupDagger(KiwixApplication.getInstance().getApplicationComponent());
    //attachPresenter();
  }

  @Override protected void onStart() {
    super.onStart();
    //presenter.onStart();
  }

  @Override protected void onResume() {
    super.onResume();
    //presenter.onResume();
  }

  @Override protected void onPause() {
    super.onPause();
    //presenter.onPause();
  }

  @Override protected void onStop() {
    super.onStop();
    //presenter.onStop();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    //presenter.onDestroy();
  }

  //protected void attachPresenter(Presenter presenter) {
  //  this.presenter = presenter;
  //}

  protected abstract void setupDagger(ApplicationComponent appComponent);

  //public abstract void attachPresenter();
}
