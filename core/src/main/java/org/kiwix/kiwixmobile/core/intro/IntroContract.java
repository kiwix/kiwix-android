package org.kiwix.kiwixmobile.core.intro;

import org.kiwix.kiwixmobile.core.base.BaseContract;

interface IntroContract {

  interface View extends BaseContract.View<Presenter> {

  }

  interface Presenter extends BaseContract.Presenter<View> {
    void setIntroShown();
  }
}
