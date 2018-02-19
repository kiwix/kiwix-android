package org.kiwix.kiwixmobile.common.utils;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by mhutti1 on 19/04/17.
 */

public class TestingUtils {

  private static TestingUtils.IdleListener callback;

  private static Set<Class> resources = new HashSet<>();

  public interface IdleListener {
    void startTask();
    void finishTask();
  }

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
}
