package org.kiwix.kiwixmobile.common.utils;

import android.support.test.espresso.IdlingResource;

import org.kiwix.kiwixmobile.common.utils.TestingUtils.IdleListener;

/**
 * Created by mhutti1 on 19/04/17.
 */

public class KiwixIdlingResource implements IdlingResource, IdleListener {

  private static KiwixIdlingResource kiwixIdlingResource;

  public static KiwixIdlingResource getInstance() {
    if (kiwixIdlingResource == null) {
      kiwixIdlingResource = new KiwixIdlingResource();
    }
    kiwixIdlingResource.idle = true;
    TestingUtils.registerIdleCallback(kiwixIdlingResource);
    return kiwixIdlingResource;
  }

  private boolean idle = true;
  private ResourceCallback resourceCallback;

  @Override
  public String getName() {
    return "Standard Kiwix Idling Resource";
  }

  @Override
  public boolean isIdleNow() {
    return idle;
  }

  @Override
  public void registerIdleTransitionCallback(ResourceCallback callback) {
    this.resourceCallback = callback;
  }

  @Override
  public void startTask() {
    idle = false;
  }

  @Override
  public void finishTask() {
    idle = true;
    if (resourceCallback != null) {
      resourceCallback.onTransitionToIdle();
    }
  }
}
