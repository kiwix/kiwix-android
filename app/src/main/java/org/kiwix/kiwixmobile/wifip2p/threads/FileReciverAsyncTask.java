package org.kiwix.kiwixmobile.wifip2p.threads;

import android.os.AsyncTask;

/**
 * Created by Rishabh Rawat on 3/4/2018.
 */

public class FileReciverAsyncTask extends AsyncTask<String,Integer,String>{

    public FileReciverAsyncTask() {
        super();
    }

    @Override
    protected String doInBackground(String... strings) {
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onCancelled(String s) {
        super.onCancelled(s);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }
}
