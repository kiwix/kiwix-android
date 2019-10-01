package org.kiwix.kiwixmobile.core.intro;

import javax.inject.Inject;
import org.kiwix.kiwixmobile.core.base.BasePresenter;
import org.kiwix.kiwixmobile.core.di.ActivityScope;
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil;

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
