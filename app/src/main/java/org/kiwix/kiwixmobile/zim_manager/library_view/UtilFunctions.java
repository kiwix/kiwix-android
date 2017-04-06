package org.kiwix.kiwixmobile.zim_manager.library_view;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Created by EladKeyshawn on 06/04/2017.
 */

public class UtilFunctions {
  public static String bytesToHuman(long size) {
    long KB = 1024;
    long MB = KB * 1024;
    long GB = MB * 1024;
    long TB = GB * 1024;
    long PB = TB * 1024;
    long EB = PB * 1024;

    if (size < KB) { return size + " Bytes"; }
    if (size >= KB && size < MB) { return round3SF((double) size / KB) + " KB"; }
    if (size >= MB && size < GB) { return round3SF((double) size / MB) + " MB"; }
    if (size >= GB && size < TB) { return round3SF((double) size / GB) + " GB"; }
    if (size >= TB && size < PB) { return round3SF((double) size / TB) + " TB"; }
    if (size >= PB && size < EB) { return round3SF((double) size / PB) + " PB"; }
    if (size >= EB) { return round3SF((double) size / EB) + " EB"; }

    return "???";
  }


  public static String round3SF(double size) {
    BigDecimal bd = new BigDecimal(size);
    bd = bd.round(new MathContext(3));
    return String.valueOf(bd.doubleValue());
  }


}
