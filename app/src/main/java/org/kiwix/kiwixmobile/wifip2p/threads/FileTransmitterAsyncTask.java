package org.kiwix.kiwixmobile.wifip2p.threads;

import android.content.Context;
import android.os.AsyncTask;

import com.github.lzyzsd.circleprogress.ArcProgress;

/**
 * Created by Rishabh Rawat on 3/4/2018.
 */

public class FileTransmitterAsyncTask extends AsyncTask<String,Integer,String> {

    private static String path;
    private static String IP;
    private Context context;
    private ArcProgress progress;

    public FileTransmitterAsyncTask(String path, String IP, Context context, ArcProgress progress) {
        super();
        this.path=path;
        this.IP=IP;
        this.context=context;
        this.progress=progress;
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
