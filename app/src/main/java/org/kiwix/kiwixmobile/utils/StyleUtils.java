/*
 * Copyright 2017 Isaac Hutt <mhutti1@gmail.com>
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
import android.os.Build;
import android.support.annotation.XmlRes;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.util.AttributeSet;
import android.util.Xml;

import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.R;
import org.xmlpull.v1.XmlPullParser;

public class StyleUtils {
  public static int dialogStyle() {
    if (KiwixMobileActivity.nightMode) {
      return android.R.style.Theme_Holo_Dialog;
    } else {
      return android.support.v7.appcompat.R.style.Theme_AppCompat_Light_Dialog_Alert;
    }
  }

  public static AttributeSet getAttributes(Context context, @XmlRes int xml) {
    XmlPullParser parser = context.getResources().getXml(R.xml.webview);
    try {
      parser.next();
      parser.nextTag();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return Xml.asAttributeSet(parser);
  }

  public static Spanned highlightUrl(String text, String url) {
    return fromHtml(text.replaceAll(url,"<u><font color='blue'>" + url + "</font></u>"));
  }

  @SuppressWarnings("deprecation")
  public static Spanned fromHtml(String source) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
    } else {
      return Html.fromHtml(source);
    }
  }
}
