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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
  public static ZimManageActivity context;
  public RelativeLayout llLayout;
  // Adapter of the Data populated by recanning the Filesystem by ourselves
  private RescanDataAdapter mRescanAdapter;
  private ArrayList<LibraryNetworkEntity.Book> mFiles;
  private ListView mZimFileList;
  private RelativeLayout progressBar;
  private TextView mFileMessage;

  private BookDao bookDao;


  public static void finishResult(String path) {
    ZimManageActivity zimManageActivity = (ZimManageActivity) context;
    if (path != null) {
      File file = new File(path);
      Uri uri = Uri.fromFile(file);
      Log.i(TAG_KIWIX, "Opening " + uri);
      zimManageActivity.setResult(zimManageActivity.RESULT_OK, new Intent().setData(uri));
      zimManageActivity.finish();
    } else {
      zimManageActivity.setResult(zimManageActivity.RESULT_CANCELED);
      zimManageActivity.finish();
    }
  }

  public void refreshFragment(){
    // Of course you will want to faActivity and llLayout in the class and not this method to access them in the rest of
    // the class, just initialize them here
    if (mZimFileList == null)
      return;

    mZimFileList.addFooterView(progressBar);
    mZimFileList.setOnItemClickListener(this);
    mZimFileList.setOnItemLongClickListener(this);

    bookDao = new BookDao(KiwixDatabase.getInstance(context));

    mFiles = bookDao.getBooks();
    Collections.sort(mFiles, new fileComparator());

    mRescanAdapter = new RescanDataAdapter(ZimFileSelectFragment.context, 0, mFiles);

    mZimFileList.setAdapter(mRescanAdapter);
    mRescanAdapter.notifyDataSetChanged();
    checkPermissions();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    FragmentActivity faActivity  = (FragmentActivity)    super.getActivity();
    context = (ZimManageActivity) super.getActivity();
    // Replace LinearLayout by the type of the root element of the layout you're trying to load
    llLayout = (RelativeLayout) inflater.inflate(R.layout.zim_list, container, false);
    new LanguageUtils(super.getActivity()).changeFont(super.getActivity().getLayoutInflater());

    mFileMessage = (TextView) llLayout.findViewById(R.id.file_management_no_files);
    mZimFileList = (ListView)  llLayout.findViewById(R.id.zimfilelist);

    mFiles = new ArrayList<LibraryNetworkEntity.Book>();
    progressBar = (RelativeLayout) super.getActivity().getLayoutInflater().inflate(R.layout.progress_bar, null);

    // Allow temporary use of ZimContentProvider to query books
    ZimContentProvider.canIterate = true;

    refreshFragment();

    return llLayout; // We must return the loaded Layout
  }

  private class fileComparator implements Comparator<LibraryNetworkEntity.Book> {
    @Override
    public int compare(LibraryNetworkEntity.Book b1, LibraryNetworkEntity.Book b2) {
      return b1.getTitle().compareTo(b2.getTitle());
    }
  }

  public void checkPermissions(){
    if (ContextCompat.checkSelfPermission(super.getActivity(),
        Manifest.permission.READ_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT > 18) {
      Toast.makeText(super.getActivity(), getResources().getString(R.string.request_storage), Toast.LENGTH_LONG)
          .show();
        requestPermissions( new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
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
    // Stop file search from accessing content provider potentially opening wrong file
    ZimContentProvider.canIterate = false;

    String file;
    LibraryNetworkEntity.Book data = (LibraryNetworkEntity.Book) mZimFileList.getItemAtPosition(position);
    file = data.file.getPath();
    finishResult(file);
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

  public void deleteSpecificZimFile(int position) {
    FileUtils.deleteZimFile(mFiles.get(position).file.getPath());
    mFiles.remove(position);
    mRescanAdapter.notifyDataSetChanged();
    checkEmpty();
    if (LibraryFragment.libraryAdapter != null) {
      LibraryFragment.libraryAdapter.getFilter().filter(context.searchView.getQuery());
    }

  }

  public void checkEmpty(){
    if (mZimFileList.getCount() == 0){
      mFileMessage.setVisibility(View.VISIBLE);
    }
  }

  // The Adapter for the ListView for when the ListView is populated with the rescanned files
  private class RescanDataAdapter extends ArrayAdapter<LibraryNetworkEntity.Book> {

    public RescanDataAdapter(Context context, int textViewResourceId, List<LibraryNetworkEntity.Book> objects) {
      super(context, textViewResourceId, objects);
      LibraryAdapter.initLanguageMap();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

      ViewHolder holder;
      LibraryNetworkEntity.Book book = getItem(position);
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
        holder.fileName.setText(LibraryAdapter.parseURL(book.file.getPath()));
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

  // This AsyncTask will scan the file system for files with the Extension ".zim" or ".zimaa"
  private class RescanFileSystem extends AsyncTask<Void, Void, Void> {

    @Override
    protected void onPreExecute() {

      super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... params) {
      // Search file system for files
      mFiles = new FileSearch().findFiles(context);

      // Remove duplicate files
      HashSet<LibraryNetworkEntity.Book> singularBooks = new HashSet<>();
      singularBooks.addAll(mFiles);
      mFiles.clear();
      mFiles.addAll(singularBooks);

      // Sort files
      Collections.sort(mFiles, new fileComparator());

      // Save files for quick access later
      bookDao.saveBooks(mFiles);
      return null;
    }
    @Override
    protected void onPostExecute(Void result) {
      mRescanAdapter = new RescanDataAdapter(ZimFileSelectFragment.context, 0, mFiles);

      mZimFileList.setAdapter(mRescanAdapter);

      mZimFileList.removeFooterView(progressBar);
      //mZimFileList.addFooterView(emptyView);

      checkEmpty();


      super.onPostExecute(result);
    }
  }
}
