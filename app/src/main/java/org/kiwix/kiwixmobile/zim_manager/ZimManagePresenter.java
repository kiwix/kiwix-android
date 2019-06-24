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
package org.kiwix.kiwixmobile.zim_manager;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.base.BasePresenter;
import org.kiwix.kiwixmobile.downloader.DownloadService;
import org.kiwix.kiwixmobile.main.MainActivity;
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil;

import static org.kiwix.kiwixmobile.zim_manager.ZimManageActivity.KIWIX_TAG;

/**
 * Presenter for {@link ZimManageActivity}
 */

class ZimManagePresenter extends BasePresenter<ZimManageViewCallback> {

  @Inject
  SharedPreferenceUtil mSharedPreferenceUtil;

  @Inject ZimManagePresenter() {
  }

  void showNoWifiWarning(Context context, String action) {
    int warningResId;
    if (mSharedPreferenceUtil.nightMode()) {
      warningResId = R.drawable.ic_warning_white;
    } else {
      warningResId = R.drawable.ic_warning_black;
    }
    if (DownloadService.ACTION_NO_WIFI.equals(action)) {
      new AlertDialog.Builder(context)
          .setTitle(R.string.wifi_only_title)
          .setMessage(R.string.wifi_only_msg)
          .setPositiveButton(R.string.yes, (dialog, i) -> {
            mSharedPreferenceUtil.putPrefWifiOnly(false);
            MainActivity.wifiOnly = false;
          })
          .setIcon(warningResId)
          .setNegativeButton(R.string.no, (dialog, i) -> {
          })
          .show();
      Log.i(KIWIX_TAG, "No WiFi, showing warning");
    }
  }
}
