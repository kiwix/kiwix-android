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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Created by mhutti1 on 19/04/17.
 */

public class BookUtils {

  private final LanguageUtils.LanguageContainer container;
  public final Map<String, Locale> localeMap;

  // Create a map of ISO 369-2 language codes
  public BookUtils(LanguageUtils.LanguageContainer container) {
    this.container = container;
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
      return container.findLanguageName(languageCode).getLanguageName();
    } else if (languageCode.length() == 3) {
      try {
        return Objects.requireNonNull(localeMap.get(languageCode)).getDisplayLanguage();
      } catch (Exception e) {
        return "";
      }
    }
    return "";
  }
}
