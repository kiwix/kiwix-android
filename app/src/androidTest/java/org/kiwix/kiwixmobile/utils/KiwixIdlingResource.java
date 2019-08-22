/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kiwix.kiwixmobile.utils;

import androidx.test.espresso.IdlingResource;
import org.kiwix.kiwixmobile.utils.TestingUtils.IdleListener;

/**
 * Created by mhutti1 on 19/04/17.
 */

public class KiwixIdlingResource implements IdlingResource, IdleListener {

  private static KiwixIdlingResource kiwixIdlingResource;
  private boolean idle = true;
  private ResourceCallback resourceCallback;

  public static KiwixIdlingResource getInstance() {
    if (kiwixIdlingResource == null) {
      kiwixIdlingResource = new KiwixIdlingResource();
    }
    kiwixIdlingResource.idle = true;
    TestingUtils.registerIdleCallback(kiwixIdlingResource);
    return kiwixIdlingResource;
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
    if (resourceCallback != null) {
      resourceCallback.onTransitionToIdle();
    }
  }
}
