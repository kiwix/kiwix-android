package org.kiwix.kiwixmobile.core.settings;

import org.kiwix.kiwixmobile.core.base.BaseContract;

interface SettingsContract {
  interface View extends BaseContract.View<Presenter> {

  }

  interface Presenter extends BaseContract.Presenter<View> {
    void clearHistory();
  }
}
