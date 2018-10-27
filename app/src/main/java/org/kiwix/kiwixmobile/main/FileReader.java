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
package org.kiwix.kiwixmobile.main;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

class FileReader {

  public String readFile(String filePath, Context context) {
    try {
      StringBuilder buf = new StringBuilder();
      InputStream json = context.getAssets().open(filePath);
      BufferedReader in =
          new BufferedReader(new InputStreamReader(json, "UTF-8"));
      String str;

      while ((str = in.readLine()) != null) {
        buf.append(str);
      }

      in.close();
      return buf.toString();
    } catch (Exception e) {
      return "";
    }
  }
}
