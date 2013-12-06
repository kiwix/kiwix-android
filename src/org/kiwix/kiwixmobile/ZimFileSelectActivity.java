package org.kiwix.kiwixmobile;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

public class ZimFileSelectActivity extends FragmentActivity
        implements LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener {

    private static final int LOADER_ID = 0x02;

    // array of valid audio file extensions
    private static final String[] zimFiles = {"zim", "zimaa"};

    // Adapter of the Data populated by the MediaStore
    private SimpleCursorAdapter mCursorAdapter;

    // Adapter of the Data populated by recanning the Filesystem by ourselves
    private RescanDataAdapter mRescanAdapter;

    private ArrayList<RescanDataModel> mFiles;

    private ListView mZimFileList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminateVisibility(true);
        setContentView(R.layout.zimfilelist);

        mZimFileList = (ListView) findViewById(R.id.zimfilelist);
        mFiles = new ArrayList<RescanDataModel>();

        selectZimFile();
    }

    private void finishResult(String path) {
        if (path != null) {
            File file = new File(path);
            Uri uri = Uri.fromFile(file);
            setResult(RESULT_OK, new Intent().setData(uri));
            finish();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    protected void selectZimFile() {
        // Defines a list of columns to retrieve from the Cursor and load into an output row
        String[] mZimListColumns = {MediaStore.Files.FileColumns.TITLE, MediaStore.Files.FileColumns.DATA};

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

        mZimFileList.setAdapter(mCursorAdapter);
        mZimFileList.setOnItemClickListener(this);

        getSupportLoaderManager().initLoader(LOADER_ID, null, this);
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
                MediaStore.Images.Media.DATA + ") LIKE '%." + zimFiles[0] + "'"
                + " OR LOWER(" +
                MediaStore.Images.Media.DATA + ") LIKE '%." + zimFiles[1] + "'"
                + " ) ";

        String[] selectionArgs = null; // There is no ? in query so null here

        String sortOrder = MediaStore.Images.Media.TITLE; // Sorted alphabetical
        Log.d("kiwix", " Performing query for zim files...");

        return new CursorLoader(this, uri, projection, query, selectionArgs, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.d("kiwix", "DONE querying Mediastore for .zim files");
        mCursorAdapter.swapCursor(cursor);
        // Done here to avoid that shown while loading.
        mZimFileList.setEmptyView(findViewById(R.id.zimfilelist_nozimfilesfound_view));
	setProgressBarIndeterminateVisibility(false);
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
            outState.putParcelableArrayList("rescanData", mFiles);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {

        // Get the rescanned data, if available. Create an Adapter for the ListView and display the list
        if (savedInstanceState.getParcelableArrayList("rescanData") != null) {
            ArrayList<RescanDataModel> data = savedInstanceState.getParcelableArrayList("rescanData");
            mRescanAdapter = new RescanDataAdapter(ZimFileSelectActivity.this, 0, data);

            mZimFileList.setAdapter(mRescanAdapter);
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.fileselector, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_rescan:
                // Execute our AsyncTask, that scans the file system for the actual data
                new RescanFileSystem().execute();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Log.d("kiwix", " mZimFileList.onItemClick");

        String file;

        // Check which one of the Adapters is currently filling the ListView.
        // If the data is populated by the LoaderManager cast the current selected item to Cursor,
        // if the data is populated by the ArrayAdapter, then cast it to the RescanDataModel class.
        if (mZimFileList.getItemAtPosition(position) instanceof RescanDataModel) {

            RescanDataModel data = (RescanDataModel) mZimFileList.getItemAtPosition(position);
            file = data.getPath();

        } else {
            Cursor cursor = (Cursor) mZimFileList.getItemAtPosition(position);
            file = cursor.getString(1);
        }

        finishResult(file);
    }

    // This AsyncTask will scan the file system for files with the Extension ".zim" or ".zimaa"
    private class RescanFileSystem extends AsyncTask<Void, Void, Void> {

        ProgressBar mProgressBar;

        @Override
        protected void onPreExecute() {
            mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
            mProgressBar.setVisibility(View.VISIBLE);

            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {

            mFiles = FindFiles();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mRescanAdapter = new RescanDataAdapter(ZimFileSelectActivity.this, 0, mFiles);

            mZimFileList.setAdapter(mRescanAdapter);

            mProgressBar.setVisibility(View.GONE);

            super.onPostExecute(result);
        }

        private ArrayList<RescanDataModel> FindFiles() {
            String directory = new File(
                    Environment.getExternalStorageDirectory().getAbsolutePath()).toString();
            final List<String> fileList = new ArrayList<String>();
            FilenameFilter[] filter = new FilenameFilter[zimFiles.length];

            int i = 0;
            for (final String extension : zimFiles) {
                filter[i] = new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith("." + extension);
                    }
                };
                i++;
            }

            File[] foundFiles = listFilesAsArray(new File(directory), filter, -1);
            for (File f : foundFiles) {
                fileList.add(f.getAbsolutePath());
            }

            return createDataForAdapter(fileList);
        }

        private Collection<File> listFiles(File directory, FilenameFilter[] filter,
                int recurse) {

            Vector<File> files = new Vector<File>();

            File[] entries = directory.listFiles();

            if (entries != null) {
                for (File entry : entries) {
                    for (FilenameFilter filefilter : filter) {
                        if (filter == null || filefilter.accept(directory, entry.getName())) {
                            files.add(entry);
                        }
                    }
                    if ((recurse <= -1) || (recurse > 0 && entry.isDirectory())) {
                        recurse--;
                        files.addAll(listFiles(entry, filter, recurse));
                        recurse++;
                    }
                }
            }
            return files;
        }

        public File[] listFilesAsArray(File directory, FilenameFilter[] filter, int recurse) {
            Collection<File> files = listFiles(directory, filter, recurse);

            File[] arr = new File[files.size()];
            return files.toArray(arr);
        }

        // Create an ArrayList with our RescanDataModel
        private ArrayList<RescanDataModel> createDataForAdapter(List<String> list) {

            ArrayList<RescanDataModel> data = new ArrayList<RescanDataModel>();
            for (String file : list) {

                data.add(new RescanDataModel(getTitleFromFilePath(file), file));
            }

            // Sorting the data in alphabetical order
            Collections.sort(data, new Comparator<RescanDataModel>() {
                @Override
                public int compare(RescanDataModel a, RescanDataModel b) {
                    return a.getTitle().compareToIgnoreCase(b.getTitle());
                }
            });

            return data;
        }

        // Remove the file path and the extension and return a file name for the given file path
        private String getTitleFromFilePath(String path) {
            return new File(path).getName().replaceFirst("[.][^.]+$", "");
        }
    }

    // This items class stores the Data for the ArrayAdapter.
    // We Have to implement Parcelable, so we can store ArrayLists with this generic type in the Bundle
    // of onSaveInstanceState() and retrieve it later on in onRestoreInstanceState()
    private class RescanDataModel implements Parcelable {

        // Interface that must be implemented and provided as a public CREATOR field.
        // It generates instances of your Parcelable class from a Parcel.
        public Parcelable.Creator<RescanDataModel> CREATOR = new Parcelable.Creator<RescanDataModel>() {

            @Override
            public RescanDataModel createFromParcel(Parcel source) {
                return new RescanDataModel(source);
            }

            @Override
            public RescanDataModel[] newArray(int size) {
                return new RescanDataModel[size];
            }

        };

        private String mTitle;

        private String mPath;

        private RescanDataModel(String title, String path) {
            mTitle = title;
            mPath = path;
        }

        // This constructor will be called when this class is generated by a Parcel.
        // We have to read the previously written Data in this Parcel.
        public RescanDataModel(Parcel parcel) {
            String[] data = new String[2];
            parcel.readStringArray(data);
            mTitle = data[0];
            mTitle = data[1];
        }

        public String getTitle() {
            return mTitle;
        }

        public String getPath() {
            return mPath;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            // Write the data to the Parcel, so we can restore this Data later on.
            // It will be restored by the RescanDataModel(Parcel parcel) constructor.
            dest.writeArray(new String[]{mTitle, mPath});
        }
    }

    // The Adapter for the ListView for when the ListView is populated with the rescanned files
    private class RescanDataAdapter extends ArrayAdapter<RescanDataModel> {

        public RescanDataAdapter(Context context, int textViewResourceId, List<RescanDataModel> objects) {
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
        // Views and saving them to this item class, and not inlating the layout every time
        // we need to create a row.
        private class ViewHolder {

            TextView title;

            TextView path;
        }
    }
}
