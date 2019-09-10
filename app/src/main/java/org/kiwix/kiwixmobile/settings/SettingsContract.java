/*
 * Copyright (c) 2019 Kiwix
 * All rights reserved.
 */

package org.kiwix.kiwixmobile.settings;

import org.kiwix.kiwixmobile.base.BaseContract;

interface SettingsContract {
  interface View extends BaseContract.View<Presenter> {

  }

  interface Presenter extends BaseContract.Presenter<View> {
    void clearHistory();
  }
}
