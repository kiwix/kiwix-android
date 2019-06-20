package org.kiwix.kiwixmobile.main;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import org.kiwix.kiwixmobile.BuildConfig;
import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.data.ZimContentProvider;
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static org.kiwix.kiwixmobile.utils.Constants.NOTES_DIRECTORY;

/**
 * Created by @author Aditya-Sood (21/05/19) as a part of GSoC 2019
 *
 * AddNoteDialog extends DialogFragment and is used to display the note corresponding to a particular
 * article (of a particular zim file/wiki/book) as a full-screen dialog fragment.
 *
 * Notes are saved as text files at location: "{External Storage}/Kiwix/Notes/ZimFileTitle/ArticleTitle.txt"
 * */

public class AddNoteDialog extends DialogFragment implements ConfirmationAlertDialogFragment.UserClickListener {

  public static String TAG = "AddNoteDialog";

  @Inject
  SharedPreferenceUtil sharedPreferenceUtil;

  @BindView(R.id.add_note_toolbar)
  Toolbar toolbar;  // Displays options for the note dialog
  @BindView(R.id.add_note_text_view)
  TextView addNoteTextView; // Displays article title
  @BindView(R.id.add_note_edit_text)
  EditText addNoteEditText; // Displays zim file title (wiki name)

  private Unbinder unbinder;

  private String zimFileTitle;
  private String articleTitle;
  private boolean noteFileExists = false;
  private boolean noteEdited = false; // Keeps track of state of the note (whether edited since last save)

  public AddNoteDialog() {
    super();
    KiwixApplication.getApplicationComponent().inject(this);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setStyle(DialogFragment.STYLE_NORMAL, sharedPreferenceUtil.nightMode() ? R.style.AddNoteDialogStyle_Night : R.style.AddNoteDialogStyle);

    zimFileTitle = ZimContentProvider.getZimFileTitle();
    articleTitle = ((MainActivity)getActivity()).getCurrentWebView().getTitle();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    View view = inflater.inflate(R.layout.dialog_add_note, container, false);
    unbinder = ButterKnife.bind(this, view);

    toolbar.setTitle(R.string.note);
    toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
    toolbar.setNavigationOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        closeKeyboard();
        exitAddNoteDialog();
      }
    });

    toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {
          case R.id.share_note: // Opens app-chooser for sharing the note text file
            shareNote();
            break;

          case R.id.save_note:  // Saves the note as a text file
            saveNote(addNoteEditText.getText().toString());
            break;
        }
        return true;
      }
    });

    toolbar.inflateMenu(R.menu.menu_add_note_dialog);
    // 'Share' disabled for empty notes, 'Save' disabled for unedited notes
    disableMenuItems();

    addNoteTextView.setText(articleTitle);

    // Show the previously saved note if it exists
    displayNote();

    addNoteEditText.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        noteEdited = true;
        enableSaveNoteMenuItem();
        enableShareNoteMenuItem();
      }

      @Override
      public void afterTextChanged(Editable s) {}
    });

    return view;
  }

  // Override onBackPressed() to respond to user pressing 'Back' button on navigation bar
  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    return new Dialog(getActivity(), getTheme()) {
      @Override
      public void onBackPressed() {
        exitAddNoteDialog();
      }
    };
  }

  private void exitAddNoteDialog() {
    if(noteEdited) {
      Fragment previousInstance = getActivity().getSupportFragmentManager().findFragmentByTag(ConfirmationAlertDialogFragment.TAG);

      if(previousInstance == null) {
        // Custom AlertDialog for taking user confirmation before closing note dialog in case of unsaved changes
        DialogFragment newFragment = new ConfirmationAlertDialogFragment(TAG, R.string.confirmation_alert_dialog_message);
        newFragment.show(getActivity().getSupportFragmentManager(), ConfirmationAlertDialogFragment.TAG);
      }

    } else {
      // Closing unedited note dialog straightaway
      dismissAddNoteDialog();
    }
  }

  private void disableMenuItems() {
    if(toolbar.getMenu() != null) {
      MenuItem saveItem = toolbar.getMenu().findItem(R.id.save_note);
      MenuItem shareItem = toolbar.getMenu().findItem(R.id.share_note);
      saveItem.setEnabled(false);
      shareItem.setEnabled(false);
      saveItem.getIcon().setAlpha(130);
      shareItem.getIcon().setAlpha(130);

    } else {
      Log.d(TAG, "Toolbar without inflated menu");
    }
  }

  private void enableSaveNoteMenuItem() {
    if(toolbar.getMenu() != null) {
      MenuItem saveItem = toolbar.getMenu().findItem(R.id.save_note);
      saveItem.setEnabled(true);
      saveItem.getIcon().setAlpha(255);

    } else {
      Log.d(TAG, "Toolbar without inflated menu");
    }
  }

  private void enableShareNoteMenuItem() {
    if(toolbar.getMenu() != null) {
      MenuItem shareItem = toolbar.getMenu().findItem(R.id.share_note);
      shareItem.setEnabled(true);
      shareItem.getIcon().setAlpha(255);

    } else {
      Log.d(TAG, "Toolbar without inflated menu");
    }
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    if(!noteFileExists) {
      // Prepare for input in case of empty/new note
      addNoteEditText.requestFocus();
      showKeyboard();
    }
  }

  public void showKeyboard(){
    InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
  }

  public void closeKeyboard(){
    InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
  }

  private void saveNote(String noteText) {

    /* String content of the EditText, given by noteText, is saved into the text file given by:
     *    "{External Storage}/Kiwix/Notes/ZimFileTitle/ArticleTitle.txt"
     * */

    if(isExternalStorageWritable()) {

      if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        Log.d(TAG, "WRITE_EXTERNAL_STORAGE permission not granted");
        showToast(R.string.note_save_unsuccessful, Toast.LENGTH_LONG);
        return;
      }

      File notesFolder = new File(NOTES_DIRECTORY + zimFileTitle);
      boolean folderExists = true;

      if(!notesFolder.exists()) {
        // Try creating folder if it doesn't exist
        folderExists = notesFolder.mkdirs();
      }

      if(folderExists) {
        File noteFile = new File(notesFolder.getAbsolutePath(), articleTitle + ".txt");

        // Save note text-file code:
        try {
          FileOutputStream fileOutputStream = new FileOutputStream(noteFile);
          fileOutputStream.write(noteText.getBytes());
          fileOutputStream.close();
          showToast(R.string.note_save_successful, Toast.LENGTH_SHORT);
          noteEdited = false; // As no unsaved changes remain

        } catch (IOException e) {
          e.printStackTrace();
          showToast(R.string.note_save_unsuccessful, Toast.LENGTH_LONG);
        }

      } else {
        showToast(R.string.note_save_unsuccessful, Toast.LENGTH_LONG);
        Log.d(TAG, "Required folder doesn't exist");
      }
    }
    else {
      showToast(R.string.note_save_error_storage_not_writable, Toast.LENGTH_LONG);
    }

  }

  private void displayNote() {

    /* String content of the note text file given at:
     *    "{External Storage}/Kiwix/Notes/ZimFileTitle/ArticleTitle.txt"
     * is displayed in the EditText field (note content area)
     * */

    File noteFile = new File(NOTES_DIRECTORY + zimFileTitle + "/" + articleTitle + ".txt");

    if(noteFile.exists()) {
      noteFileExists = true;

      StringBuilder contents = new StringBuilder();
      try {

        BufferedReader input = new BufferedReader(new java.io.FileReader(noteFile));
        try {
          String line = null;

          while((line = input.readLine()) != null) {
            contents.append(line);
            contents.append(System.getProperty("line.separator"));
          }
        } catch (IOException e) {
          e.printStackTrace();
          Log.d(TAG, "Error reading line with BufferedReader");
        } finally {
          input.close();
        }

      } catch (IOException e) {
        e.printStackTrace();
        Log.d(TAG, "Error closing BufferedReader");
      }

      addNoteEditText.setText(contents.toString()); // Display the note content

      enableShareNoteMenuItem(); // As note content exists which can be shared
    }

    // No action in case the note file for the currently open article doesn't exist
  }

  private void shareNote() {

    /* The note text file corresponding to the currently open article, given at:
     *    "{External Storage}/Kiwix/Notes/ZimFileTitle/ArticleTitle.txt"
     * is shared via an app-chooser intent
     * */

    if(noteEdited) {
     saveNote(addNoteEditText.getText().toString()); // Save edited note before sharing the text file
    }

    File noteFile = new File(NOTES_DIRECTORY + zimFileTitle + "/" + articleTitle + ".txt");

    Uri noteFileUri = null;
    if(noteFile.exists()) {

      if (Build.VERSION.SDK_INT >= 24) {
        // From Nougat 7 (API 24) access to files is shared temporarily with other apps
        // Need to use FileProvider for the same
        noteFileUri = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID+".fileprovider", noteFile);

      } else {
        noteFileUri = Uri.fromFile(noteFile);
      }

    } else {
      showToast(R.string.note_share_error_file_missing, Toast.LENGTH_SHORT);
    }

    if(noteFileUri != null) {
      Intent noteFileShareIntent = new Intent(Intent.ACTION_SEND);
      noteFileShareIntent.setType("application/octet-stream");
      noteFileShareIntent.putExtra(Intent.EXTRA_STREAM, noteFileUri);
      noteFileShareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      Intent shareChooser = Intent.createChooser(noteFileShareIntent, getString(R.string.note_share_app_chooser_title));

      if(noteFileShareIntent.resolveActivity(getActivity().getPackageManager()) != null) {
        startActivity(shareChooser);
      }
    }
  }

  public static boolean isExternalStorageWritable() {
    return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
  }

  private void showToast(int stringResource, int duration) {
    Toast.makeText(getActivity(), stringResource, duration).show();
  }

  // Methods from ConfirmationAlertDialogFragment.UserClickListener interface
  @Override
  public void onPositiveClick() {
    dismissAddNoteDialog();
  }

  @Override
  public void onNegativeClick() {
    // Do nothing
  }

  private void dismissAddNoteDialog() {
    Dialog dialog = getDialog();
    dialog.dismiss();
  }

  @Override
  public void onStart() {
    super.onStart();

    Dialog dialog = getDialog();
    if(dialog != null) {
      int width = ViewGroup.LayoutParams.MATCH_PARENT;
      int height = ViewGroup.LayoutParams.MATCH_PARENT;
      dialog.getWindow().setLayout(width, height);
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (unbinder != null) {
      unbinder.unbind();
    }
  }

}
