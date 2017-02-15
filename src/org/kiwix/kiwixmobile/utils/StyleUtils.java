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

import org.kiwix.kiwixmobile.KiwixMobileActivity;

public class StyleUtils {
  public static int dialogStyle() {
    if (KiwixMobileActivity.nightMode) {
      return android.R.style.Theme_Holo_Dialog;
    } else {
      return android.support.v7.appcompat.R.style.Theme_AppCompat_Light_Dialog_Alert;
    }
  }
}
