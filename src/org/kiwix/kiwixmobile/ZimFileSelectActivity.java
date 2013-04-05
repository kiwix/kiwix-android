package org.kiwix.kiwixmobile;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleCursorAdapter;
//TODO API level 11 (honeycomb). use compatiblity packages instead   
import android.content.CursorLoader;
import android.app.LoaderManager;

public class ZimFileSelectActivity extends Activity implements
LoaderManager.LoaderCallbacks<Cursor> {

	private static final int LOADER_ID = 0x02;
	private SimpleCursorAdapter mCursorAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.zimfilelist);
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

	protected void selectZimFile()  {
		// Defines a list of columns to retrieve from the Cursor and load into an output row
		String[] mZimListColumns =
			{
				MediaStore.Images.Media.DATA
			};

		// Defines a list of View IDs that will receive the Cursor columns for each row
		int[] mZimListItems = { R.id.zim_file_list_entry_path};

		mCursorAdapter = new SimpleCursorAdapter(
				getApplicationContext(),               // The application's Context object
				R.layout.zimfilelistentry,                  // A layout in XML for one row in the ListView
				null,                               // The cursor, swapped later by cursorloader
				mZimListColumns,                      // A string array of column names in the cursor
				mZimListItems,                        // An integer array of view IDs in the row layout
				Adapter.NO_SELECTION);

		// Sets the adapter for the ListView
		setContentView(R.layout.zimfilelist);


		ListView zimFileList = (ListView) findViewById(R.id.zimfilelist);
		getLoaderManager().initLoader(LOADER_ID, null, this);

		zimFileList.setAdapter(mCursorAdapter);
		zimFileList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				// TODO Auto-generated method stub
				onListItemClick((ListView) arg0, arg0, arg2, arg3);
			}
		});
		//TODO close cursor when done
		//allNonMediaFiles.close();
	}


	private void onListItemClick(AdapterView<?> adapter, View view, int position, long arg) {
		// TODO Auto-generated method stub
		Log.d("zimgap", " zimFileList.onItemClick");

		ListView zimFileList = (ListView) findViewById(R.id.zimfilelist);
		Cursor mycursor = (Cursor) zimFileList.getItemAtPosition(position);
		//TODO not very clean		
		finishResult(mycursor.getString(1));
	}

	@Override
	public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
		//TODO leads to API min 11
		Uri uri = MediaStore.Files.getContentUri("external");

		String[] projection = {
				MediaStore.Images.Media._ID,
				MediaStore.Images.Media.DATA, //Path        
		};

		// exclude media files, they would be here also (perhaps
		// somewhat better performance), and filter for zim files
		// (normal and first split)        
		String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
				+ MediaStore.Files.FileColumns.MEDIA_TYPE_NONE + " AND "
				+ " ( LOWER(" +
				MediaStore.Images.Media.DATA + ") LIKE '%.zim'"
				+ " OR LOWER(" +
				MediaStore.Images.Media.DATA + ") LIKE '%.zimaa'"
				+" ) ";


		String[] selectionArgs = null; // there is no ? in selection so null here


		String sortOrder = MediaStore.Images.Media.DATA; // unordered
		Log.d("zimgap", " Performing query for zim files...");


		return new CursorLoader(this, uri, projection, selection, selectionArgs, sortOrder);

	}

	@Override
	public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
		Log.d("zimgap", " DONE query zim files");
		mCursorAdapter.swapCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursorLoader) {
		mCursorAdapter.swapCursor(null);
	}

}
