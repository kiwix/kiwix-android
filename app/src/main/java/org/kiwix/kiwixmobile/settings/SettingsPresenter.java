/*
 * Copyright (c) 2019 Kiwix
 * All rights reserved.
 */

package org.kiwix.kiwixmobile.settings;

import android.util.Log;
import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.base.BasePresenter;
import org.kiwix.kiwixmobile.data.DataSource;

class SettingsPresenter extends BasePresenter<SettingsContract.View>
    implements SettingsContract.Presenter {

  private final DataSource dataSource;

  @Inject SettingsPresenter(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void clearHistory() {
    dataSource.clearHistory()
        .subscribe(new CompletableObserver() {
          @Override
          public void onSubscribe(Disposable d) {

          }

          @Override
          public void onComplete() {

          }

          @Override
          public void onError(Throwable e) {
            Log.e("SettingsPresenter", e.toString());
          }
        });
  }
}
