package org.kiwix.kiwixmobile.base;

import org.kiwix.kiwixmobile.base.ViewCallback;

/**
 * Created by EladKeyshawn on 05/04/2017.
 */
public interface Presenter<V extends ViewCallback> {

  void attachView(V mvpView);

  void detachView();
}
