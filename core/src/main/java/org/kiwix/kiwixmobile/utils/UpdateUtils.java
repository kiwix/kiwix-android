package org.kiwix.kiwixmobile.utils;

import static org.kiwix.kiwixmobile.utils.Constants.NEW_PROVIDER_DOMAIN;
import static org.kiwix.kiwixmobile.utils.Constants.OLD_PROVIDER_DOMAIN;

public class UpdateUtils {
  public static String reformatProviderUrl(String url) {
    return url.replace(OLD_PROVIDER_DOMAIN, NEW_PROVIDER_DOMAIN);
  }
}
