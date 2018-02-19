package org.kiwix.kiwixmobile.common.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.common.di.components.ApplicationComponent;

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
