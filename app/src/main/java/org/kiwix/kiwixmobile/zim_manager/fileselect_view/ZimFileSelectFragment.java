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

/**
 * Fragment for list of downloaded ZIM files
 */

package org.kiwix.kiwixmobile.zim_manager.fileselect_view;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.kiwix.kiwixmobile.BuildConfig;
import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.base.BaseFragment;
import org.kiwix.kiwixmobile.data.ZimContentProvider;
import org.kiwix.kiwixmobile.data.local.dao.BookDao;
import org.kiwix.kiwixmobile.library.LibraryAdapter;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.utils.BookUtils;
import org.kiwix.kiwixmobile.utils.LanguageUtils;
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil;
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

import static org.kiwix.kiwixmobile.utils.Constants.REQUEST_STORAGE_PERMISSION;
import static org.kiwix.kiwixmobile.utils.NetworkUtils.parseURL;
import static org.kiwix.kiwixmobile.utils.StyleUtils.dialogStyle;

public class ZimFileSelectFragment extends BaseFragment
    implements OnItemClickListener, AdapterView.OnItemLongClickListener, ZimFileSelectViewCallback {

  public RelativeLayout llLayout;
  public SwipeRefreshLayout swipeRefreshLayout;
  @Inject
  ZimFileSelectPresenter presenter;
  @Inject
  BookUtils bookUtils;
  @Inject
  SharedPreferenceUtil sharedPreferenceUtil;
  @Inject
  BookDao bookDao;
  private ZimManageActivity zimManageActivity;
  private RescanDataAdapter mRescanAdapter;
  private ArrayList<LibraryNetworkEntity.Book> mFiles;
  private ListView mZimFileList;
  private TextView mFileMessage;
  private boolean mHasRefresh;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    KiwixApplication.getApplicationComponent().inject(this);
    zimManageActivity = (ZimManageActivity) super.getActivity();
    presenter.attachView(this);
    // Replace LinearLayout by the type of the root element of the layout you're trying to load
    llLayout = (RelativeLayout) inflater.inflate(R.layout.zim_list, container, false);
    new LanguageUtils(zimManageActivity).changeFont(zimManageActivity.getLayoutInflater(), sharedPreferenceUtil);

    mFileMessage = llLayout.findViewById(R.id.file_management_no_files);
    mZimFileList = llLayout.findViewById(R.id.zimfilelist);

    mFiles = new ArrayList<>();

    // SwipeRefreshLayout for the list view
    swipeRefreshLayout = llLayout.findViewById(R.id.zim_swiperefresh);
    swipeRefreshLayout.setOnRefreshListener(this::refreshFragment);

    // A boolean to distinguish between a user refresh and a normal loading
    mHasRefresh = false;

    mRescanAdapter = new RescanDataAdapter(zimManageActivity, 0, mFiles);

    // Allow temporary use of ZimContentProvider to query books
    ZimContentProvider.canIterate = true;

    // Setting up Contextual Action Mode in response to selection of ZIM files in the ListView
    mZimFileList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    mZimFileList.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

      // Holds positions corresponding to every selected list item in the ListView
      private ArrayList<Integer> selectedViewPosition = new ArrayList<Integer>();

      @Override
      public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

        if(checked) { // If the item was selected
          selectedViewPosition.add(position);
          mode.setTitle("" + selectedViewPosition.size()); // Update title of the CAB
        }
        else {  // If the item was deselected
          selectedViewPosition.remove(Integer.valueOf(position));
          mode.setTitle("" + selectedViewPosition.size()); // Update title of the CAB
        }

      }

      @Override
      public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // Inflate and setup the Contextual Action Bar (CAB)
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.menu_zim_files_contextual, menu);

        return true;
      }

      @Override
      public boolean onPrepareActionMode(ActionMode mode, Menu menu) {  // Leave the default implementation as is
        return false;
      }

      @Override
      public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

        switch(item.getItemId()) {  // Determine the action item clicked on in the CAB, and respond accordingly

          // Initiate file deletion functionality for each selected list item (file)
          case R.id.zim_file_delete_item :

            for(int i = 0; i < selectedViewPosition.size(); i++)
              deleteSpecificZimDialog(selectedViewPosition.get(i)); // Individually confirm & initiate deletion for each selected file

            mode.finish(); // Action performed, so close CAB
            return true;


          // Initiate file sharing functionality for each selected list item (file)
          case R.id.zim_file_share_item :

            // Create an implicit intent for sharing multiple selected files
            Intent selectedFileShareIntent = new Intent();
            selectedFileShareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
            selectedFileShareIntent.setType("application/octet-stream");  // ZIM files are binary data without an Android-predefined subtype

            ArrayList<Uri> selectedFileContentURIs = new ArrayList<Uri>(); // Store Content URIs for all selected files being shared

            for(int i = 0; i < selectedViewPosition.size(); i++) {

              LibraryNetworkEntity.Book data = (LibraryNetworkEntity.Book) mZimFileList.getItemAtPosition(selectedViewPosition.get(i));
              String shareFilePath = data.file.getPath(); //Returns path to file in device storage

              /**
               * Using 'file:///' URIs directly is unsafe as it grants the intent receiving application
               * the same file system permissions as the intent creating app.
               *
               * FileProvider instead offers 'content://' URIs for sharing files, which offer temporary
               * access permissions to the file being shared (to the intent receiving application), which
               * is fundamentally safer.
                */

              File shareFile = new File(shareFilePath);
              Uri shareContentUri = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID + ".fileprovider", shareFile);

              if(shareContentUri != null)
                selectedFileContentURIs.add(shareContentUri);  // Populate with the selected file content URIs
            }

            selectedFileShareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, selectedFileContentURIs); // Intent Extra for storing the array list of selected file content URIs

            if(selectedFileContentURIs != null) {  // Grant temporary access permission to the intent receiver for the content URIs
              selectedFileShareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            /**
             * Since a different app may be used for sharing everytime (E-mail, Cloud Upload, Wifi Sharing, etc.),
             * so force an app chooser dialog every time some selected files are to be shared.
              */
            Intent shareChooserIntent = Intent.createChooser(selectedFileShareIntent, getResources().getString(R.string.selected_file_cab_app_chooser_title));

            if(shareChooserIntent.resolveActivity(getActivity().getPackageManager()) != null)
              startActivity(shareChooserIntent);  // Open the app chooser dialog

            mode.finish(); // Action performed, so close CAB
            return true;


          default :
            return false;

        }
      }

      @Override
      public void onDestroyActionMode(ActionMode mode) {
        // Upon closure of the CAB, empty the array list of selected list item positions
        selectedViewPosition.clear();
      }

    });

    return llLayout; // Return the loaded Layout
  }

  @Override
  public void onResume() {
    presenter.loadLocalZimFileFromDb();
    super.onResume();
  }


  // Show files from database
  @Override
  public void showFiles(ArrayList<LibraryNetworkEntity.Book> books) {
    if (mZimFileList == null)
      return;

    // Long click response is the Contextual Action Bar (Selected file deletion & sharing)
    mZimFileList.setOnItemClickListener(this);
    Collections.sort(books, new FileComparator());
    mFiles.clear();
    mFiles.addAll(books);
    mZimFileList.setAdapter(mRescanAdapter);
    mRescanAdapter.notifyDataSetChanged();
    checkEmpty();
    checkPermissions();
  }

  public void refreshFragment() {
    if (mZimFileList == null) {
      swipeRefreshLayout.setRefreshing(false);
      return;
    }

    mHasRefresh = true;
    presenter.loadLocalZimFileFromDb();
  }

  // Add book after download
  public void addBook(String path) {
    LibraryNetworkEntity.Book book = FileSearch.fileToBook(path);
    if (book != null) {
      mFiles.add(book);
      mRescanAdapter.notifyDataSetChanged();
      presenter.saveBooks(mFiles);
      checkEmpty();
    }
  }

  public void checkPermissions() {
    if (ContextCompat.checkSelfPermission(zimManageActivity,
        Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT > 18) {
      Toast.makeText(super.getActivity(), getResources().getString(R.string.request_storage), Toast.LENGTH_LONG)
          .show();
      requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
          REQUEST_STORAGE_PERMISSION);
    } else {
      getFiles();
    }
  }

  public void getFiles() {
    if (swipeRefreshLayout.isRefreshing() && !mHasRefresh)
      return;

    TestingUtils.bindResource(ZimFileSelectFragment.class);
    swipeRefreshLayout.setRefreshing(true);
    mZimFileList.setAdapter(mRescanAdapter);

    // Set mHasRefresh to false to prevent loops
    mHasRefresh = false;

    checkEmpty();

    new FileSearch(zimManageActivity, new FileSearch.ResultListener() {
      @Override
      public void onBookFound(LibraryNetworkEntity.Book book) {
        if (!mFiles.contains(book)) {
          zimManageActivity.runOnUiThread(() -> {
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
        if (!cached && zimManageActivity.mSectionsPagerAdapter.libraryFragment.libraryAdapter != null && zimManageActivity.searchView != null) {
          zimManageActivity.mSectionsPagerAdapter.libraryFragment.libraryAdapter.getFilter().filter(zimManageActivity.searchView.getQuery());
        }

        // Save the current list of books
        zimManageActivity.runOnUiThread(() -> {
          mRescanAdapter.notifyDataSetChanged();
          presenter.saveBooks(mFiles);
          checkEmpty();
          TestingUtils.unbindResource(ZimFileSelectFragment.class);

          // Stop swipe refresh animation
          swipeRefreshLayout.setRefreshing(false);
        });
      }
    }).scan(sharedPreferenceUtil.getPrefStorage());
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         @NonNull String permissions[], @NonNull int[] grantResults) {
    switch (requestCode) {
      case REQUEST_STORAGE_PERMISSION: {
        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          getFiles();
        } else if (grantResults.length != 0) {
          zimManageActivity.finish();
        }
      }

    }
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    // Stop file search from accessing content provider potentially opening wrong file
    ZimContentProvider.canIterate = false;

    String file;
    LibraryNetworkEntity.Book data = (LibraryNetworkEntity.Book) mZimFileList.getItemAtPosition(position);
    file = data.file.getPath();

    if (!data.file.canRead()) {
      Toast.makeText(zimManageActivity, getString(R.string.error_filenotfound), Toast.LENGTH_LONG).show();
      return;
    }

    zimManageActivity.finishResult(file);
  }

  @Override
  public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
    return false;
  }

  public void deleteSpecificZimDialog(int position) {
    new AlertDialog.Builder(zimManageActivity, dialogStyle())
        .setMessage(mFiles.get(position).getTitle() + ": " + getString(R.string.delete_specific_zim))
        .setPositiveButton(getResources().getString(R.string.delete), (dialog, which) -> {
          // Toast messages updated for coherence with the new manner of file deletion (from CAB)
          if (deleteSpecificZimFile(position)) {
            Toast.makeText(zimManageActivity, getResources().getString(R.string.delete_specific_zim_toast), Toast.LENGTH_SHORT).show();
          } else {
            Toast.makeText(zimManageActivity, getResources().getString(R.string.delete_zim_failed), Toast.LENGTH_SHORT).show();
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
    presenter.deleteBook(mFiles.get(position));
    mFiles.remove(position);
    mRescanAdapter.notifyDataSetChanged();
    checkEmpty();
    if (zimManageActivity.mSectionsPagerAdapter.libraryFragment.libraryAdapter != null) {
      zimManageActivity.mSectionsPagerAdapter.libraryFragment.libraryAdapter.getFilter().filter(zimManageActivity.searchView.getQuery());
    }
    return true;
  }

  public void checkEmpty() {
    if (mZimFileList.getCount() == 0) {
      mFileMessage.setVisibility(View.VISIBLE);
    } else
      mFileMessage.setVisibility(View.GONE);
  }

  private class FileComparator implements Comparator<LibraryNetworkEntity.Book> {
    @Override
    public int compare(LibraryNetworkEntity.Book b1, LibraryNetworkEntity.Book b2) {
      return b1.getTitle().compareTo(b2.getTitle());
    }
  }

  // The Adapter for the ListView for when the ListView is populated with the rescanned files
  private class RescanDataAdapter extends ArrayAdapter<LibraryNetworkEntity.Book> {

    RescanDataAdapter(Context context, int textViewResourceId, List<LibraryNetworkEntity.Book> objects) {
      super(context, textViewResourceId, objects);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

      ViewHolder holder;
      LibraryNetworkEntity.Book book = getItem(position);
      if (convertView == null) {
        convertView = View.inflate(zimManageActivity, R.layout.library_item, null);
        holder = new ViewHolder();
        holder.title = convertView.findViewById(R.id.title);
        holder.description = convertView.findViewById(R.id.description);
        holder.language = convertView.findViewById(R.id.language);
        holder.creator = convertView.findViewById(R.id.creator);
        holder.publisher = convertView.findViewById(R.id.publisher);
        holder.date = convertView.findViewById(R.id.date);
        holder.size = convertView.findViewById(R.id.size);
        holder.fileName = convertView.findViewById(R.id.fileName);
        holder.favicon = convertView.findViewById(R.id.favicon);
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
      holder.favicon.setImageBitmap(LibraryAdapter.createBitmapFromEncodedString(book.getFavicon(), zimManageActivity));


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
