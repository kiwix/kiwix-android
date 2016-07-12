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

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.kiwix.kiwixmobile.database.BookDao;
import org.kiwix.kiwixmobile.database.KiwixDatabase;
import org.kiwix.kiwixmobile.library.LibraryAdapter;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.utils.LanguageUtils;
import org.kiwix.kiwixmobile.utils.ShortcutUtils;
import org.kiwix.kiwixmobile.utils.files.FileSearch;
import org.kiwix.kiwixmobile.utils.files.FileUtils;
import org.kiwix.kiwixmobile.utils.files.FileWriter;
import org.w3c.dom.Text;

public class ZimFileSelectFragment extends Fragment
    implements OnItemClickListener, AdapterView.OnItemLongClickListener {

  public static final String TAG_KIWIX = "kiwix";

  private static final int LOADER_ID = 0x02;

  // Adapter of the Data populated by the MediaStore
  private SimpleCursorAdapter mCursorAdapter;

  // Adapter of the Data populated by recanning the Filesystem by ourselves
  private RescanDataAdapter mRescanAdapter;

  private ArrayList<DataModel> mFiles;

  private ListView mZimFileList;

  private RelativeLayout mProgressBar;

  private TextView mFileMessage;

  private TextView mProgressBarMessage;

  public RelativeLayout llLayout;

  public static Context context;

  private BookDao bookDao;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    FragmentActivity faActivity  = (FragmentActivity)    super.getActivity();
    context = super.getActivity();
    // Replace LinearLayout by the type of the root element of the layout you're trying to load
    llLayout = (RelativeLayout) inflater.inflate(R.layout.zim_list, container, false);
    // Of course you will want to faActivity and llLayout in the class and not this method to access them in the rest of
    // the class, just initialize them here

    new LanguageUtils(super.getActivity()).changeFont(super.getActivity().getLayoutInflater());

    mFiles = new ArrayList<DataModel>();

    mProgressBar = (RelativeLayout) llLayout.findViewById(R.id.progressbar_layout);
    mFileMessage = (TextView) llLayout.findViewById(R.id.file_management_no_files);
//    mProgressBarMessage = (TextView) llLayout.findViewById(R.id.progressbar_message);
    mZimFileList = (ListView)  llLayout.findViewById(R.id.zimfilelist);

    mZimFileList.setOnItemClickListener(this);
    mZimFileList.setOnItemLongClickListener(this);
    mProgressBar.setVisibility(View.VISIBLE);

    bookDao = new BookDao(new KiwixDatabase(context));

    checkPermissions();

    // Don't use this method, it's handled by inflater.inflate() above :
    // setContentView(R.layout.activity_layout);

    // The FragmentActivity doesn't contain the layout directly so we must use our instance of     LinearLayout :
    //llLayout.findViewById(R.id.someGuiElement);
    // Instead of :
    // findViewById(R.id.someGuiElement);
    return llLayout; // We must return the loaded Layout
  }



  public void checkPermissions(){
    if (ContextCompat.checkSelfPermission(super.getActivity(),
        Manifest.permission.READ_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT > 18) {
      Toast.makeText(super.getActivity(), getResources().getString(R.string.request_storage), Toast.LENGTH_LONG)
          .show();
        ActivityCompat.requestPermissions(super.getActivity(),
            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
            KiwixMobileActivity.REQUEST_STORAGE_PERMISSION);

    } else {
      getFiles();
    }
  }

  public void getFiles(){
      new RescanFileSystem().execute();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         String permissions[], int[] grantResults) {
    switch (requestCode) {
      case KiwixMobileActivity.REQUEST_STORAGE_PERMISSION: {
        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getFiles();
        } else {
          super.getActivity().finish();
        }
        return;
      }

    }
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

    if (view.getAlpha() == 1f) {
      DataModel data = (DataModel) mZimFileList.getItemAtPosition(position);
      file = data.getPath();
      finishResult(file);
    } else {
      openCorruptZimDialog(position);
    }
  }

  @Override
  public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
    String file = mZimFileList.getItemAtPosition(position).toString();
    deleteSpecificZimDialog(position);
    return true;
  }

  public void deleteSpecificZimDialog(int position) {
    new AlertDialog.Builder(super.getActivity())
        .setMessage(ShortcutUtils.stringsGetter(R.string.delete_specific_zim, context))
        .setPositiveButton(getResources().getString(R.string.delete), new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            deleteSpecificZimFile(position);
            Toast.makeText(context, getResources().getString(R.string.delete_specific_zim_toast), Toast.LENGTH_SHORT).show();
          }
        })
        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            // do nothing
          }
        })
        .show();
  }
  public void openCorruptZimDialog(int position) {
    new AlertDialog.Builder(super.getActivity())
        .setMessage(ShortcutUtils.stringsGetter(R.string.open_partial_zim, context))
        .setPositiveButton(getResources().getString(R.string.open), new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            String file;
            DataModel data = (DataModel) mZimFileList.getItemAtPosition(position);
            file = data.getPath();
            finishResult(file);
          }
        })
        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            // do nothing
          }
        })
        .show();
  }


  public void deleteSpecificZimFile(int position) {
    FileUtils.deleteZimFile(mFiles.get(position).getPath());
    mFiles.remove(position);
    mRescanAdapter.notifyDataSetChanged();
    checkEmpty();
  }


  private void finishResult(String path) {

    if (path != null) {
      File file = new File(path);
      Uri uri = Uri.fromFile(file);
      Log.i(TAG_KIWIX, "Opening " + uri);
      super.getActivity().setResult(super.getActivity().RESULT_OK, new Intent().setData(uri));
      super.getActivity().finish();
    } else {
      super.getActivity().setResult(super.getActivity().RESULT_CANCELED);
      super.getActivity().finish();
    }
  }


  // The Adapter for the ListView for when the ListView is populated with the rescanned files
  private class RescanDataAdapter extends ArrayAdapter<DataModel> {

    public RescanDataAdapter(Context context, int textViewResourceId, List<DataModel> objects) {
      super(context, textViewResourceId, objects);
      LibraryAdapter.initLanguageMap();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

      ViewHolder holder;

      // Check if we should inflate the layout for a new row, or if we can reuse a view.

      if (bookDao.getBook(mFiles.get(position).getTitle()) == null) {
        if (convertView == null) {
          convertView = View.inflate(getContext(), R.layout.library_item, null);
          holder = new ViewHolder();
          holder.title = (TextView) convertView.findViewById(R.id.title);
          holder.description = (TextView) convertView.findViewById(R.id.description);
          convertView.setTag(holder);
        } else {
          holder = (ViewHolder) convertView.getTag();
        }
        holder.title.setText(getItem(position).getTitle());
        holder.description.setText(getItem(position).getPath());
      } else {
        LibraryNetworkEntity.Book book = bookDao.getBook(mFiles.get(position).getTitle());
        if (convertView == null) {
          convertView = View.inflate(getContext(), R.layout.library_item, null);
          holder = new ViewHolder();
          holder.title = (TextView) convertView.findViewById(R.id.title);
          holder.description = (TextView) convertView.findViewById(R.id.description);
          holder.language = (TextView) convertView.findViewById(R.id.language);
          holder.creator = (TextView) convertView.findViewById(R.id.creator);
          holder.publisher = (TextView) convertView.findViewById(R.id.publisher);
          holder.date = (TextView) convertView.findViewById(R.id.date);
          holder.size = (TextView) convertView.findViewById(R.id.size);
          holder.fileName = (TextView) convertView.findViewById(R.id.fileName);
          holder.favicon = (ImageView) convertView.findViewById(R.id.favicon);
          convertView.setTag(holder);
        } else {
          holder = (ViewHolder) convertView.getTag();
        }

        holder.title.setText(book.getTitle());
        holder.description.setText(book.getDescription());
        holder.language.setText(LibraryAdapter.getLanguage(book.getLanguage()));
        holder.creator.setText(book.getCreator());
        holder.publisher.setText(book.getPublisher());
        holder.date.setText(book.getDate());
        holder.size.setText(LibraryAdapter.createGbString(book.getSize()));
        holder.fileName.setText(LibraryAdapter.parseURL(book.getUrl()));
        holder.favicon.setImageBitmap(LibraryAdapter.createBitmapFromEncodedString(book.getFavicon()));


        //// Check if no value is empty. Set the view to View.GONE, if it is. To View.VISIBLE, if not.
        if (book.getTitle() == null || book.getTitle().isEmpty()) {
          holder.title.setVisibility(View.GONE);
        } else {
          holder.title.setVisibility(View.VISIBLE);
        }

        if (book.getDescription() == null || book.getDescription().isEmpty()) {
          holder.description.setVisibility(View.GONE);
        } else {
          holder.description.setVisibility(View.VISIBLE);
        }

        if (book.getCreator() == null || book.getCreator().isEmpty()) {
          holder.creator.setVisibility(View.GONE);
        } else {
          holder.creator.setVisibility(View.VISIBLE);
        }

        if (book.getPublisher() == null || book.getPublisher().isEmpty()) {
          holder.publisher.setVisibility(View.GONE);
        } else {
          holder.publisher.setVisibility(View.VISIBLE);
        }

        if (book.getDate() == null || book.getDate().isEmpty()) {
          holder.date.setVisibility(View.GONE);
        } else {
          holder.date.setVisibility(View.VISIBLE);
        }

        if (book.getSize() == null || book.getSize().isEmpty()) {
          holder.size.setVisibility(View.GONE);
        } else {
          holder.size.setVisibility(View.VISIBLE);
        }

        if (!book.downloaded) {
          convertView.setAlpha(0.3f);
        } else {
          convertView.setAlpha(1f);
        }

      }
      return convertView;
    }

    // We are using the ViewHolder pattern in order to optimize the ListView by reusing
    // Views and saving them to this mLibrary class, and not inlating the layout every time
    // we need to create a row.
    private class ViewHolder {
      TextView title;

      TextView description;

      TextView language;

      TextView creator;

      TextView publisher;

      TextView date;

      TextView size;

      TextView fileName;

      ImageView favicon;
    }
  }

  public void checkEmpty(){
    if (mZimFileList.getCount() == 0){
      mFileMessage.setVisibility(View.VISIBLE);
    }
  }

  // This AsyncTask will scan the file system for files with the Extension ".zim" or ".zimaa"
  private class RescanFileSystem extends AsyncTask<Void, Void, Void> {

    @Override
    protected void onPreExecute() {

      mProgressBar.setVisibility(View.VISIBLE);

      super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... params) {

      mFiles = new FileSearch().findFiles();
      return null;
    }
    @Override
    protected void onPostExecute(Void result) {
      mRescanAdapter = new RescanDataAdapter(ZimFileSelectFragment.context, 0, mFiles);

      mZimFileList.setAdapter(mRescanAdapter);

      mProgressBar.setVisibility(View.GONE);

      checkEmpty();

      new FileWriter(ZimFileSelectFragment.context).saveArray(mFiles);

      super.onPostExecute(result);
    }
  }
}
