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
package org.kiwix.kiwixmobile.zim_manager.library_view;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Created by EladKeyshawn on 06/04/2017.
 */

public class LibraryUtils {
  public static String bytesToHuman(long size) {
    long KB = 1024;
    long MB = KB * 1024;
    long GB = MB * 1024;
    long TB = GB * 1024;
    long PB = TB * 1024;
    long EB = PB * 1024;

    if (size < KB) {
      return size + " Bytes";
    }
    if (size < MB) {
      return round3SF((double) size / KB) + " KB";
    }
    if (size >= MB && size < GB) {
      return round3SF((double) size / MB) + " MB";
    }
    if (size >= GB && size < TB) {
      return round3SF((double) size / GB) + " GB";
    }
    if (size >= TB && size < PB) {
      return round3SF((double) size / TB) + " TB";
    }
    if (size >= PB && size < EB) {
      return round3SF((double) size / PB) + " PB";
    }
    if (size >= EB) {
      return round3SF((double) size / EB) + " EB";
    }

    return "???";
  }


  public static String round3SF(double size) {
    BigDecimal bd = new BigDecimal(size);
    bd = bd.round(new MathContext(3));
    return String.valueOf(bd.doubleValue());
  }


}
