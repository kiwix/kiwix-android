package org.kiwix.kiwixmobile.common.utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by mhutti1 on 19/04/17.
 */

public class BookUtils {

  public final Map<String, Locale> localeMap;

  // Create a map of ISO 369-2 language codes
  public BookUtils() {
    String[] languages = Locale.getISOLanguages();
    localeMap = new HashMap<>(languages.length);
    for (String language : languages) {
      Locale locale = new Locale(language);
      localeMap.put(locale.getISO3Language(), locale);
    }
  }

  // Get the language from the language codes of the parsed xml stream
  public String getLanguage(String languageCode) {

    if (languageCode == null) {
      return "";
    }

    if (languageCode.length() == 2) {
      return new LanguageUtils.LanguageContainer(languageCode).getLanguageName();
    } else if (languageCode.length() == 3) {
      try {
        return localeMap.get(languageCode).getDisplayLanguage();
      } catch (Exception e) {
        return "";
      }
    }
    return "";
  }
}
