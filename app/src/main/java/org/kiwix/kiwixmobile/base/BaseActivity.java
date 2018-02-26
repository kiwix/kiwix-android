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
    View decorView = getWindow().getDecorView();
    decorView.setOnSystemUiVisibilityChangeListener
            (new View.OnSystemUiVisibilityChangeListener() {
              @Override
              public void onSystemUiVisibilityChange(int visibility) {
                // Note that system bars will only be "visible" if none of the
                // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                  View decorView = getWindow().getDecorView();
// Hide both the navigation bar and the status bar.
                  int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                          | View.SYSTEM_UI_FLAG_FULLSCREEN;
                  decorView.setSystemUiVisibility(uiOptions);
                } 
              }
            });
  }

  @Override protected void onStart() {
    View decorView = getWindow().getDecorView();
    int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN;
    decorView.setSystemUiVisibility(uiOptions);
    super.onStart();
    //presenter.onStart();
  }

  @Override protected void onResume() {
    // Hide Navigation Activity added in onResume() method so that whenever the app is closed via home button and then opened again
    // then also the navigation bar remains transparent
    View decorView = getWindow().getDecorView();
    int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN;
    decorView.setSystemUiVisibility(uiOptions);
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
