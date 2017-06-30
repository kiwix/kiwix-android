package org.kiwix.kiwixmobile;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

public class AppIndexingService extends IntentService {

    private static final String TAG = "AppIndexingService";

    public AppIndexingService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d("TAG" , "Received update index intent");
        // TODO index documents here
    }
}
