package org.kiwix.kiwixmobile.utils;

import android.support.test.espresso.IdlingResource;
import org.kiwix.kiwixmobile.utils.TestingUtils.IdleListener;
import org.kiwix.kiwixmobile.zim_manager.library_view.LibraryFragment;

/**
 * Created by mhutti1 on 19/04/17.
 */

public class LibraryIdlingResource implements IdlingResource, IdleListener {

  private boolean idle = true;
  private ResourceCallback resourceCallback;


  public LibraryIdlingResource() {
    LibraryFragment.registerIdleCallback(this);
  }

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
    resourceCallback.onTransitionToIdle();
  }
}
