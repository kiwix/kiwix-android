package org.kiwix.kiwixmobile.core.utils;

import org.kiwix.kiwixmobile.core.KiwixApplication;

import static org.kiwix.kiwixmobile.core.utils.Constants.OLD_PROVIDER_DOMAIN;

public class UpdateUtils {
  public static String reformatProviderUrl(String url) {
    return url.replace(OLD_PROVIDER_DOMAIN,
      KiwixApplication.getInstance().getPackageName() + ".zim.base");
  }
}
