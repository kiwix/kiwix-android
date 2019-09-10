/*
 * Copyright (c) 2019 Kiwix
 * All rights reserved.
 */

package org.kiwix.kiwixmobile.intro;

import org.kiwix.kiwixmobile.base.BaseContract;

interface IntroContract {

  interface View extends BaseContract.View<Presenter> {

  }

  interface Presenter extends BaseContract.Presenter<View> {
    void setIntroShown();
  }
}
