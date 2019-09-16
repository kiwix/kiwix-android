package org.kiwix.kiwixmobile.intro;

import javax.inject.Inject;
import org.kiwix.kiwixmobile.base.BasePresenter;
import org.kiwix.kiwixmobile.di.ActivityScope;
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil;

@ActivityScope
class IntroPresenter extends BasePresenter<IntroContract.View> implements IntroContract.Presenter {
  private final SharedPreferenceUtil preferences;

  @Inject IntroPresenter(SharedPreferenceUtil preferences) {
    this.preferences = preferences;
  }

  @Override
  public void setIntroShown() {
    preferences.setIntroShown();
  }
}
