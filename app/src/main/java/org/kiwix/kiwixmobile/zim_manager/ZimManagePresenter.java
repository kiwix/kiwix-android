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

import android.content.Context;
import android.util.Log;

import org.kiwix.kiwixmobile.base.BasePresenter;
import org.kiwix.kiwixmobile.downloader.DownloadFragment;
import org.kiwix.kiwixmobile.downloader.DownloadService;

import javax.inject.Inject;

import static org.kiwix.kiwixmobile.zim_manager.ZimManageActivity.KIWIX_TAG;

/**
 * Created by srv_twry on 15/2/18.
 */

public class ZimManagePresenter extends BasePresenter<ZimManageViewCallback> {

    @Inject
    public ZimManagePresenter() {}

    void showNoWifiWarning(Context context, String action) {
        if (DownloadService.ACTION_NO_WIFI.equals(action)) {
            DownloadFragment.showNoWiFiWarning(context, () -> {});
            Log.i(KIWIX_TAG, "No WiFi, showing warning");
        }
    }
}
