/*
 * Copyright (c) 2019 Kiwix
 * All rights reserved.
 */

package org.kiwix.kiwixmobile.base;

public class BaseContract {

  /**
   * The contract for a view must extend this interface.
   *
   * @param <T> the type of presenter associated with the view
   */
  public interface View<T> {

  }

  public interface Presenter<T> {

    /**
     * Binds presenter with a view when resumed. The Presenter will perform initialization here.
     *
     * @param view the view associated with this presenter
     */
    void attachView(T view);

    /**
     * Drops the reference to the view when destroyed
     */
    void detachView();
  }
}
