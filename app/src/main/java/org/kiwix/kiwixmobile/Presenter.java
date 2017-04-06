package org.kiwix.kiwixmobile;

/**
 * Created by EladKeyshawn on 06/04/2017.
 */

public interface Presenter<V extends ViewCallback> {

  void attachView(V viewCallback);

  void detachView();
}