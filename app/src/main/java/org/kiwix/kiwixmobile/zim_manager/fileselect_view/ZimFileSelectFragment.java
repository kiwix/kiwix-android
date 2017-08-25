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

package org.kiwix.kiwixmobile.zim_manager.fileselect_view;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.ZimContentProvider;
import org.kiwix.kiwixmobile.database.BookDao;
import org.kiwix.kiwixmobile.database.KiwixDatabase;
import org.kiwix.kiwixmobile.library.LibraryAdapter;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.utils.BookUtils;
import org.kiwix.kiwixmobile.utils.LanguageUtils;
import org.kiwix.kiwixmobile.utils.TestingUtils;
import org.kiwix.kiwixmobile.utils.files.FileSearch;
import org.kiwix.kiwixmobile.utils.files.FileUtils;
import org.kiwix.kiwixmobile.zim_manager.ZimManageActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import static org.kiwix.kiwixmobile.utils.NetworkUtils.parseURL;
import static org.kiwix.kiwixmobile.utils.StyleUtils.dialogStyle;

public class ZimFileSelectFragment extends Fragment
    implements OnItemClickListener, AdapterView.OnItemLongClickListener, ZimFileSelectViewCallback{

  public static final String TAG_KIWIX = "kiwix";
  public static ZimManageActivity context;
  public RelativeLayout llLayout;
  private RescanDataAdapter mRescanAdapter;
  private ArrayList<LibraryNetworkEntity.Book> mFiles;
  private ListView mZimFileList;
  private RelativeLayout progressBar;
  private TextView mFileMessage;

  private BookDao bookDao;

  @Inject ZimFileSelectPresenter presenter;
  @Inject BookUtils bookUtils;

  private void setupDagger() {
    KiwixApplication.getInstance().getApplicationComponent().inject(this);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    context = (ZimManageActivity) super.getActivity();
    setupDagger();
    presenter.attachView(this);
    // Replace LinearLayout by the type of the root element of the layout you're trying to load
    llLayout = (RelativeLayout) inflater.inflate(R.layout.zim_list, container, false);
    new LanguageUtils(super.getActivity()).changeFont(super.getActivity().getLayoutInflater());

    mFileMessage = (TextView) llLayout.findViewById(R.id.file_management_no_files);
    mZimFileList = (ListView)  llLayout.findViewById(R.id.zimfilelist);

    mFiles = new ArrayList<>();
    progressBar = (RelativeLayout) super.getActivity().getLayoutInflater().inflate(R.layout.progress_bar, null);

    mRescanAdapter = new RescanDataAdapter(ZimFileSelectFragment.context, 0, mFiles);

    // Allow temporary use of ZimContentProvider to query books
    ZimContentProvider.canIterate = true;

    presenter.loadLocalZimFileFromDb(context);
     bookDao = new BookDao(KiwixDatabase.getInstance(context));

    return llLayout; // We must return the loaded Layout
  }

  // Set zim file and return
  public static void finishResult(String path) {
    ZimManageActivity zimManageActivity = context;
    if (path != null) {
      File file = new File(path);
      Uri uri = Uri.fromFile(file);
      Log.i(TAG_KIWIX, "Opening Zim File: " + uri);
      zimManageActivity.setResult(Activity.RESULT_OK, new Intent().setData(uri));
      zimManageActivity.finish();
    } else {
      zimManageActivity.setResult(Activity.RESULT_CANCELED);
      zimManageActivity.finish();
    }
  }

  @Override
  public void onResume() {
    presenter.loadLocalZimFileFromDb(context);
    super.onResume();
  }


  // Show files from database
  @Override
  public void showFiles(ArrayList<LibraryNetworkEntity.Book> books) {
    if (mZimFileList == null)
      return;

    mZimFileList.setOnItemClickListener(this);
    mZimFileList.setOnItemLongClickListener(this);
    Collections.sort(books, new FileComparator());
    mFiles.clear();
    mFiles.addAll(books);
    mZimFileList.setAdapter(mRescanAdapter);
    mRescanAdapter.notifyDataSetChanged();
    checkEmpty();
    checkPermissions();
  }

  public void refreshFragment(){
    if (mZimFileList == null)
      return;
    presenter.loadLocalZimFileFromDb(context);
  }

  // Add book after download
  public void addBook(String path) {
    LibraryNetworkEntity.Book book = FileSearch.fileToBook(path);
    if (book != null) {
      mFiles.add(book);
      mRescanAdapter.notifyDataSetChanged();
      bookDao.saveBooks(mFiles);
    }
  }

  private class FileComparator implements Comparator<LibraryNetworkEntity.Book> {
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

  public void getFiles() {
    if (mZimFileList.getFooterViewsCount() != 0)
      return;

    TestingUtils.bindResource(ZimFileSelectFragment.class);
    mZimFileList.addFooterView(progressBar);
    mZimFileList.setAdapter(mRescanAdapter);

    checkEmpty();

    new FileSearch(context, new FileSearch.ResultListener() {
      @Override
      public void onBookFound(LibraryNetworkEntity.Book book) {
        if (!mFiles.contains(book)) {
          context.runOnUiThread(() -> {
            Log.i("Scanner", "File Search: Found Book " + book.title);
            mFiles.add(book);
            mRescanAdapter.notifyDataSetChanged();
            checkEmpty();
          });
        }
      }

      @Override
      public void onScanCompleted() {
        // Remove non-existent books
        ArrayList<LibraryNetworkEntity.Book> books = new ArrayList<>(mFiles);
        for (LibraryNetworkEntity.Book book : books) {
          if (book.file == null || !book.file.canRead()) {
            mFiles.remove(book);
          }
        }

        boolean cached = mFiles.containsAll(bookDao.getBooks()) && bookDao.getBooks().containsAll(mFiles);

        // If content changed then update the list of downloadable books
        if (!cached && context.mSectionsPagerAdapter.libraryFragment.libraryAdapter != null && context.searchView != null) {
          context.mSectionsPagerAdapter.libraryFragment.libraryAdapter.getFilter().filter(context.searchView.getQuery());
        }

        // Save the current list of books
        context.runOnUiThread(() -> {
          mRescanAdapter.notifyDataSetChanged();
          bookDao.saveBooks(mFiles);
          mZimFileList.removeFooterView(progressBar);
          checkEmpty();
          TestingUtils.unbindResource(ZimFileSelectFragment.class);
        });
      }
    }).scan(PreferenceManager.getDefaultSharedPreferences(context)
        .getString(KiwixMobileActivity.PREF_STORAGE, Environment.getExternalStorageDirectory().getPath()));
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         String permissions[], int[] grantResults) {
    switch (requestCode) {
      case KiwixMobileActivity.REQUEST_STORAGE_PERMISSION: {
        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getFiles();
        } else if (grantResults.length != 0) {
          super.getActivity().finish();
        }
      }

    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_rescan_fs:
       getFiles();
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    // Stop file search from accessing content provider potentially opening wrong file
    ZimContentProvider.canIterate = false;

    String file;
    LibraryNetworkEntity.Book data = (LibraryNetworkEntity.Book) mZimFileList.getItemAtPosition(position);
    file = data.file.getPath();

    if (!data.file.canRead()) {
      Toast.makeText(context, getString(R.string.error_filenotfound), Toast.LENGTH_LONG).show();
      return;
    }

    finishResult(file);
  }

  @Override
  public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
    deleteSpecificZimDialog(position);
    return true;
  }

  public void deleteSpecificZimDialog(int position) {
    new AlertDialog.Builder(super.getActivity(), dialogStyle())
        .setMessage(getString(R.string.delete_specific_zim))
        .setPositiveButton(getResources().getString(R.string.delete), (dialog, which) -> {
          if (deleteSpecificZimFile(position)) {
            Toast.makeText(context, getResources().getString(R.string.delete_specific_zim_toast), Toast.LENGTH_SHORT).show();
          } else {
            Toast.makeText(context, getResources().getString(R.string.delete_zim_failed), Toast.LENGTH_SHORT).show();
          }
        })
        .setNegativeButton(android.R.string.no, (dialog, which) -> {
          // do nothing
        })
        .show();
  }

  public boolean deleteSpecificZimFile(int position) {
    File file = mFiles.get(position).file;
    FileUtils.deleteZimFile(file.getPath());
    if (file.exists()) {
      return false;
    }
    bookDao.deleteBook(mFiles.get(position).getId());
    mFiles.remove(position);
    mRescanAdapter.notifyDataSetChanged();
    checkEmpty();
    if (context.mSectionsPagerAdapter.libraryFragment.libraryAdapter != null) {
      context.mSectionsPagerAdapter.libraryFragment.libraryAdapter.getFilter().filter(context.searchView.getQuery());
    }
    return true;
  }

  public void checkEmpty(){
    if (mZimFileList.getCount() == 0){
      mFileMessage.setVisibility(View.VISIBLE);
    } else
      mFileMessage.setVisibility(View.GONE);
  }

  // The Adapter for the ListView for when the ListView is populated with the rescanned files
  private class RescanDataAdapter extends ArrayAdapter<LibraryNetworkEntity.Book> {

    public RescanDataAdapter(Context context, int textViewResourceId, List<LibraryNetworkEntity.Book> objects) {
      super(context, textViewResourceId, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

      ViewHolder holder;
      LibraryNetworkEntity.Book book = getItem(position);
        if (convertView == null) {
          convertView = View.inflate(context, R.layout.library_item, null);
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

        if (book == null) {
          return convertView;
        }

        holder.title.setText(book.getTitle());
        holder.description.setText(book.getDescription());
        holder.language.setText(bookUtils.getLanguage(book.getLanguage()));
        holder.creator.setText(book.getCreator());
        holder.publisher.setText(book.getPublisher());
        holder.date.setText(book.getDate());
        holder.size.setText(LibraryAdapter.createGbString(book.getSize()));
        holder.fileName.setText(parseURL(getActivity(), book.file.getPath()));
        holder.favicon.setImageBitmap(LibraryAdapter.createBitmapFromEncodedString(book.getFavicon(), context));


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
}
