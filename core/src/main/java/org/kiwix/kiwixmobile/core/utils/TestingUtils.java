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
package org.kiwix.kiwixmobile.core.utils;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by mhutti1 on 19/04/17.
 */

public class TestingUtils {

  private static TestingUtils.IdleListener callback;

  private static Set<Class> resources = new HashSet<>();

  public static void bindResource(Class bindClass) {
    if (callback != null) {
      resources.add(bindClass);
      if (resources.size() == 1) {
        callback.startTask();
      }
    }
  }

  public static void unbindResource(Class bindClass) {
    if (callback != null) {
      resources.remove(bindClass);
      if (resources.isEmpty()) {
        callback.finishTask();
      }
    }
  }

  public static void registerIdleCallback(TestingUtils.IdleListener listListener) {
    resources.clear();
    callback = listListener;
  }

  public interface IdleListener {
    void startTask();

    void finishTask();
  }
}
