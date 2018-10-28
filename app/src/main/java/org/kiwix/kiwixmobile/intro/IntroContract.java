package org.kiwix.kiwixmobile.intro;

import org.kiwix.kiwixmobile.base.BaseContract;

interface IntroContract {

  interface View extends BaseContract.View<Presenter> {

  }

  interface Presenter extends BaseContract.Presenter<View> {
    void setIntroShown();
  }
}
