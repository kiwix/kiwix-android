package org.kiwix.kiwixmobile;

import android.app.Activity;
import android.app.ProgressDialog;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;

import org.kiwix.kiwixmobile.R;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

/**
 * Created by sakchham on 22/11/13.
 */

public class ZimFileScanner extends AsyncTask<Void,Void,Void> implements MediaScannerConnection.MediaScannerConnectionClient
{
    public ZimFileScanner(Activity context)
    {
        super();
        callingActivity = context;
    }

    private ProgressDialog progressDialog = null;
    private MediaScannerConnection mMediaScannerConnection = null;
    private Activity callingActivity = null;
    private long filesScanned = 0l;
    private ArrayList<WeakReference<File>> zimFiles = new ArrayList();

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = new ProgressDialog(callingActivity);
        mMediaScannerConnection = new MediaScannerConnection(callingActivity,this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();
        progressDialog.setMessage(callingActivity.getString(R.string.rescan_fs_warning));
    }

    @Override
    protected Void doInBackground(Void... voids) {
        mMediaScannerConnection.connect();
        scanForFiles(Environment.getExternalStorageDirectory());
        return null;
    }

    private void scanForFiles(File file)
    {
        for (File f : file.listFiles())
        {
            if(f.isDirectory()){
                scanForFiles(f);
                continue;
            }
            filesScanned++;
            String fname = f.getPath();
            if (fname.substring(fname.lastIndexOf('.')+1).toLowerCase().equals("zim"))
            {
                zimFiles.add(new WeakReference<File>(f));
                Log.d("kiwix", "Scanning file :" + f.getAbsolutePath());
            }
        }
    }

    @Override
    public void onMediaScannerConnected() {
        Log.d("kiwix","Connected Media Scanner");
        Iterator<WeakReference<File>> zim = zimFiles.iterator();
        while (zim.hasNext())
        {
            mMediaScannerConnection.scanFile(zim.next().get().getAbsolutePath(),null);
        }
        mMediaScannerConnection.disconnect();
    }

    @Override
    public void onScanCompleted(String s, Uri uri) {
        //Scan completed don't log here it will print for each file
        Log.d("kiwix","Scanned file : "+s);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        Log.d("kiwix","Browsed : "+filesScanned+" files");
        mMediaScannerConnection.connect();
        progressDialog.dismiss();
    }
}


