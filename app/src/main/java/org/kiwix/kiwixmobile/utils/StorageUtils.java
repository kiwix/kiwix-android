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

public class StorageUtils {
  /**
   * url parts matching this regex are removed by {@link #removeParts(String)}
   */
  private static final String REMOVE_URL_PART_REGEX = "\\.meta4";

  /**
   * transform an <code>url</code> to a filename by applying specific String replacement rules
   * @param url the raw url to be adjusted
   * @see #removeParts(String)
   */
  public static String getFileNameFromUrl(String url) {
    return removeParts(NetworkUtils.getFileNameFromUrl(url));
  }

  /**
   * remove parts matching {@link #REMOVE_URL_PART_REGEX} from given <code>filename</code>
   * @param filename blank will return empty String
   */
  private static String removeParts(String filename) {
    if (filename == null || "".equals(filename)) {
      return  "";
    }

    return filename.replaceAll(REMOVE_URL_PART_REGEX,"");
  }
}
