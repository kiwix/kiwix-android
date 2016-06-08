/*
 * Copyright 2013  Elad Keyshawn <elad.keyshawn@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */


package org.kiwix.kiwixmobile.utils;

import android.content.Context;


/*
Helper class containing basic useful functions
that are meant to make shortcuts and beautifying code.
*/

public class ShortcutUtils {

  public static String stringsGetter(int strId, Context context) {
    return context.getResources().getString(strId);
  }

  public static String escapeSqlSyntax(String search) {//Escapes sql ' if exists
    if (search != null) {
      String tempStr = "";
      char[] charArray = search.toCharArray();
      for (char a : charArray) {
        if (a != '\'')
          tempStr += a;
        else
          tempStr += "''";
      }
      return tempStr;
    } else {
      return search;
    }
  }

}
