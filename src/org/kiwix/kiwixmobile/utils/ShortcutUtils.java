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
