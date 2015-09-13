/*
 * Copyright 2013  Rashiq Ahmad <rashiq.z@gmail.com>
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

package org.kiwix.kiwixmobile;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ZimFileSelectActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener {

    public static final String TAG_KIWIX = "kiwix";

    private static final int LOADER_ID = 0x02;

    // Adapter of the Data populated by the MediaStore
    private SimpleCursorAdapter mCursorAdapter;

    // Adapter of the Data populated by recanning the Filesystem by ourselves
    private RescanDataAdapter mRescanAdapter;

    private ArrayList<DataModel> mFiles;

    private ListView mZimFileList;

    private ProgressBar mProgressBar;

    private TextView mProgressBarMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new LanguageUtils(this).changeFont(getLayoutInflater());

        setContentView(R.layout.zim_list);
        setUpToolbar();

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBarMessage = (TextView) findViewById(R.id.progressbar_message);
        mZimFileList = (ListView) findViewById(R.id.zimfilelist);
        mFiles = new ArrayList<DataModel>();

        mZimFileList.setOnItemClickListener(this);

        mProgressBar.setVisibility(View.VISIBLE);
        setAlpha(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            startQuery();
        } else {
            new RescanFileSystem().execute();
        }
    }

    private void setUpToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        Uri uri = MediaStore.Files.getContentUri("external");

        String[] projection = {
                MediaStore.Files.FileColumns._ID,
                // File Name
                MediaStore.Files.FileColumns.TITLE,
                // File Path
                MediaStore.Files.FileColumns.DATA
        };

        // Exclude media files, they would be here also (perhaps
        // somewhat better performance), and filter for zim files
        // (normal and first split)
        String query = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_NONE + " AND"
                + " ( LOWER(" +
                MediaStore.Images.Media.DATA + ") LIKE '%." + FileSearch.zimFiles[0] + "'"
                + " OR LOWER(" +
                MediaStore.Images.Media.DATA + ") LIKE '%." + FileSearch.zimFiles[1] + "'"
                + " ) ";

        String[] selectionArgs = null; // There is no ? in query so null here

        String sortOrder = MediaStore.Files.FileColumns.TITLE; // Sorted alphabetical
        Log.d(TAG_KIWIX, " Performing query for zim files...");

        return new CursorLoader(this, uri, projection, query, selectionArgs, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.d(TAG_KIWIX, "DONE querying Mediastore for .zim files");
        buildArrayAdapter(cursor);
        mCursorAdapter.swapCursor(cursor);
        mRescanAdapter = buildArrayAdapter(cursor);
        mZimFileList.setAdapter(mRescanAdapter);

        // Done here to avoid that shown while loading.
        mZimFileList.setEmptyView(findViewById(R.id.zimfilelist_nozimfilesfound_view));

        if (mProgressBarMessage.getVisibility() == View.GONE) {
            mProgressBar.setVisibility(View.GONE);
            setAlpha(false);
        }

        mCursorAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursorAdapter.swapCursor(null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        // Check, if the user has rescanned the file system, if he has, then we want to save this list,
        // so this can be shown again, if the actvitity is recreated (on a device rotation for example)
        if (!mFiles.isEmpty()) {
            Log.i(TAG_KIWIX, "Saved state of the ListView");
            outState.putParcelableArrayList("rescanData", mFiles);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {

        // Get the rescanned data, if available. Create an Adapter for the ListView and display the list
        if (savedInstanceState.getParcelableArrayList("rescanData") != null) {
            ArrayList<DataModel> data = savedInstanceState.getParcelableArrayList("rescanData");
            mRescanAdapter = new RescanDataAdapter(ZimFileSelectActivity.this, 0, data);

            mZimFileList.setAdapter(mRescanAdapter);
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_files, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_rescan_fs:
                // Execute our AsyncTask, that scans the file system for the actual data
                new RescanFileSystem().execute();

                // Make sure, that we set mNeedsUpdate to true and to false, after the MediaStore has been
                // updated. Otherwise it will result in a endless loop.
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Log.d(TAG_KIWIX, " mZimFileList.onItemClick");

        String file;

        // Check which one of the Adapters is currently filling the ListView.
        // If the data is populated by the LoaderManager cast the current selected mLibrary to Cursor,
        // if the data is populated by the ArrayAdapter, then cast it to the DataModel class.
        if (mZimFileList.getItemAtPosition(position) instanceof DataModel) {

            DataModel data = (DataModel) mZimFileList.getItemAtPosition(position);
            file = data.getPath();

        } else {
            Cursor cursor = (Cursor) mZimFileList.getItemAtPosition(position);
            file = cursor.getString(2);
        }

        finishResult(file);
    }

    // Query through the MediaStore
    protected void startQuery() {

        // Defines a list of columns to retrieve from the Cursor and load into an output row
        String[] mZimListColumns = {MediaStore.Files.FileColumns.TITLE,
                MediaStore.Files.FileColumns.DATA};

        // Defines a list of View IDs that will receive the Cursor columns for each row
        int[] mZimListItems = {android.R.id.text1, android.R.id.text2};

        mCursorAdapter = new SimpleCursorAdapter(
                // The Context object
                ZimFileSelectActivity.this,
                // A layout in XML for one row in the ListView
                android.R.layout.simple_list_item_2,
                // The cursor, swapped later by cursorloader
                null,
                // A string array of column names in the cursor
                mZimListColumns,
                // An integer array of view IDs in the row layout
                mZimListItems,
                // Flags for the Adapter
                Adapter.NO_SELECTION);

        getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }

    // Get the data of our cursor and wrap it all in our ArrayAdapter.
    // We are doing this because the CursorAdapter does not allow us do remove rows from its dataset.
    private RescanDataAdapter buildArrayAdapter(Cursor cursor) {

        ArrayList<DataModel> files = new ArrayList<>();

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {

            if (new File(cursor.getString(2)).exists()) {
                files.add(new DataModel(cursor.getString(1), cursor.getString(2)));
            }
        }

        files = new FileWriter(ZimFileSelectActivity.this, files).getDataModelList();

        for (int i = 0; i < files.size(); i++) {

            if (!new File(files.get(i).getPath()).exists()) {
                Log.e(TAG_KIWIX, "File removed: " + files.get(i).getTitle());
                files.remove(i);
            }
        }

        files = new FileSearch().sortDataModel(files);
        mFiles = files;

        return new RescanDataAdapter(ZimFileSelectActivity.this, 0, mFiles);
    }

    // Get the selected file and return the result to the Activity, that called this Activity
    private void finishResult(String path) {

        if (path != null) {
            File file = new File(path);
            Uri uri = Uri.fromFile(file);
            Log.i(TAG_KIWIX, "Opening " + uri);
            setResult(RESULT_OK, new Intent().setData(uri));
            finish();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    // Make the View transparent or opaque
    private void setAlpha(boolean transparent) {

        float viewTransparency = transparent ? 0.4F : 1F;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mZimFileList.setAlpha(viewTransparency);
        } else {
            AlphaAnimation alpha = new AlphaAnimation(viewTransparency, viewTransparency);
            alpha.setDuration(0);
            alpha.setFillAfter(true);
            mZimFileList.startAnimation(alpha);
        }
    }

    // The Adapter for the ListView for when the ListView is populated with the rescanned files
    private class RescanDataAdapter extends ArrayAdapter<DataModel> {

        public RescanDataAdapter(Context context, int textViewResourceId, List<DataModel> objects) {
            super(context, textViewResourceId, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder;

            // Check if we should inflate the layout for a new row, or if we can reuse a view.
            if (convertView == null) {
                convertView = View.inflate(getContext(), android.R.layout.simple_list_item_2, null);
                holder = new ViewHolder();
                holder.title = (TextView) convertView.findViewById(android.R.id.text1);
                holder.path = (TextView) convertView.findViewById(android.R.id.text2);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.title.setText(getItem(position).getTitle());
            holder.path.setText(getItem(position).getPath());
            return convertView;
        }

        // We are using the ViewHolder pattern in order to optimize the ListView by reusing
        // Views and saving them to this mLibrary class, and not inlating the layout every time
        // we need to create a row.
        private class ViewHolder {

            TextView title;

            TextView path;
        }
    }

    // This AsyncTask will scan the file system for files with the Extension ".zim" or ".zimaa"
    private class RescanFileSystem extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {

            mProgressBarMessage.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.VISIBLE);
            setAlpha(true);

            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {

            mFiles = new FileSearch().findFiles();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mRescanAdapter = new RescanDataAdapter(ZimFileSelectActivity.this, 0, mFiles);

            mZimFileList.setAdapter(mRescanAdapter);

            mProgressBarMessage.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.GONE);
            setAlpha(false);

            new FileWriter(ZimFileSelectActivity.this).saveArray(mFiles);

            super.onPostExecute(result);
        }
    }
}
