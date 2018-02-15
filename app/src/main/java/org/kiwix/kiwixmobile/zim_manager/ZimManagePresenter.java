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
