package org.kiwix.kiwixmobile.intro;

import org.kiwix.kiwixmobile.base.BasePresenter;
import org.kiwix.kiwixmobile.di.PerActivity;
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil;

import javax.inject.Inject;

@PerActivity
class IntroPresenter extends BasePresenter<IntroContract.View> implements IntroContract.Presenter {
  private final SharedPreferenceUtil preferences;

  @Inject
  IntroPresenter(SharedPreferenceUtil preferences) {
    this.preferences = preferences;
  }


  @Override
  public void setIntroShown() {
    preferences.setIntroShown();
  }
}
