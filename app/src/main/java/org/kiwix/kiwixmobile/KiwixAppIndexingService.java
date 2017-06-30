package org.kiwix.kiwixmobile;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.IndexableBuilder;

public class KiwixAppIndexingService extends IntentService {

    private static final String TAG = "KiwixAppIndexing";

    public KiwixAppIndexingService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG , "Received update index intent");

        boolean val = ZimContentProvider.searchSuggestions("b", 100);
        Log.d(TAG , "Preparing suggestions " + val);
        String suggestion;
        Log.d(TAG , "Received update index intent");
        while((suggestion = ZimContentProvider.getNextSuggestion()) != null) {
            Log.d(TAG , "got suggestion: " + suggestion);
            Indexable.Builder builder = new Indexable.Builder().setName(suggestion).setUrl(ZimContentProvider.getAppIndexingUrl(suggestion));
            FirebaseAppIndex.getInstance().update(builder.build());
            Log.d(TAG , "Received update index intent");
        }
        // TODO index documents here
    }
}
