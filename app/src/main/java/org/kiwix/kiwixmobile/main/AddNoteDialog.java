package org.kiwix.kiwixmobile.main;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentTransaction;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.data.ZimContentProvider;
import org.kiwix.kiwixmobile.utils.files.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

public class AddNoteDialog extends DialogFragment {//implements ConfirmationAlertDialogFragment.ConfirmationAlertDialogListener {

  private Toolbar toolbar;
  private TextView addNoteTextView;
  private EditText addNoteEditText;
  private String zimFileTitle;
  private String articleTitle;
  private boolean noteFileExists = false;
  private String noteFileText;
  private boolean noteEdited = false;
  private Dialog addNoteDialog;

  private final String TAG = "AddNoteDialog";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setStyle(DialogFragment.STYLE_NORMAL, R.style.AddNoteDialogStyle);

    zimFileTitle = ZimContentProvider.getZimFileTitle();
    articleTitle = ((MainActivity)getActivity()).getCurrentWebView().getTitle();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    View view = inflater.inflate(R.layout.dialog_add_note, container, false);
    //addNoteDialog = getDialog();

    toolbar = view.findViewById(R.id.add_note_toolbar);
    toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
    //toolbar.setNavigationOnClickListener();
    //toolbar.setOnMenuItemClickListener
    toolbar.setTitle("Note");

    toolbar.setNavigationOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        //TODO: Use AlertDialog for confirmation incase of changed note

        closeKeyboard();
        if(noteEdited) {
          DialogFragment newFragment = new ConfirmationAlertDialogFragment(getDialog());
          newFragment.show(getActivity().getSupportFragmentManager(), "ConfirmationAlertDialog");

        } else {
          //Closing unedited dialog straightaway
          Dialog dialog = getDialog();
          dialog.dismiss();
        }
      }
    });

    toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {
          case R.id.share_note:
            Toast.makeText(getContext(), "Share Note", Toast.LENGTH_SHORT).show();
            //TODO: Add app-chooser to intent for sharing note text file
            break;

          case R.id.save_note:
            //TODO: Add code for saving note text files
            saveNote(addNoteEditText.getText().toString());
            break;
        }
        return true;
      }
    });

    toolbar.inflateMenu(R.menu.menu_add_note_dialog);
    disableMenuItems();

    addNoteTextView = view.findViewById(R.id.add_note_text_view);
    addNoteTextView.setText(articleTitle);

    addNoteEditText = view.findViewById(R.id.add_note_edit_text);
    //TODO: Use displayNote() to show the previously saved note if it exists
    displayNote();

    addNoteEditText.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        noteEdited = true;
        Toast.makeText(getContext(), "Text changed", Toast.LENGTH_SHORT).show();
        enableSaveNoteMenuItem();
        enableShareNoteMenuItem();
      }

      @Override
      public void afterTextChanged(Editable s) {}
    });

    return view;
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

  /*@Override
  public void onPrepareOptionsMenu(Menu menu) {

    MenuItem item = menu.findItem(R.id.save_note);

    if(noteEdited) {
      item.setEnabled(true);
      item.getIcon().setAlpha(255);
    } else {
      Toast.makeText(getContext(), "UNEDITED", Toast.LENGTH_SHORT).show();
      item.setEnabled(false);
      item.getIcon().setAlpha(130);
    }

  }*/

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    if(!noteFileExists) {
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

    if(isExternalStorageWritable()) {

      if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        Log.d(TAG, "WRITE_EXTERNAL_STORAGE permission not granted");
        Toast.makeText(getContext(), "Note save unsuccessful", Toast.LENGTH_LONG);
        return;
      }

      File notesFolder = new File(Environment.getExternalStorageDirectory() + "/Kiwix/Notes/" + zimFileTitle);
      boolean folderExists = true;

      if(!notesFolder.exists()) {
        folderExists = notesFolder.mkdirs();
      }

      if(folderExists) {
        File noteFile = new File(notesFolder.getAbsolutePath(), articleTitle + ".txt");

        //TODO: Save file code
        try {
          FileOutputStream fileOutputStream = new FileOutputStream(noteFile);
          fileOutputStream.write(noteText.getBytes());
          fileOutputStream.close();
          Toast.makeText(getContext(), "Note saved", Toast.LENGTH_SHORT).show();
          noteEdited = false;
        } catch (IOException e) {
          e.printStackTrace();
        }

      } else {
        //Toast.makeText(getContext(), "Error: Check logs", Toast.LENGTH_SHORT).show();
        Toast.makeText(getContext(), "Note save unsuccessful", Toast.LENGTH_LONG);
        Log.d(TAG, "Required folder doesn't exist");
      }
    }
    else {
      Toast.makeText(getContext(), "Error saving note:\nStorage not writable", Toast.LENGTH_LONG).show();
    }

  }

  private void displayNote() {

    File noteFile = new File(Environment.getExternalStorageDirectory() + "/Kiwix/Notes/" + zimFileTitle + "/" + articleTitle + ".txt");

    if(noteFile.exists()) {
      noteFileExists = true;
      //save note in noteFileText
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

        /*FileInputStream fileInputStream = new FileInputStream(noteFile);
        long noteFileSize = noteFile.length();
        int byteArraySize;
        if(noteFileSize > Integer.MAX_VALUE) {
          int offset = 0;
          while(noteFileSize > Integer.MAX_VALUE) {
            byte[] byteArray = new byte[Integer.MAX_VALUE];


            noteFileText += new String(byteArray, "UTF-8");
            //noteFileSize
          }
        }*/

      } catch (IOException e) {
        e.printStackTrace();
        Log.d(TAG, "Error closing BufferedReader");
      }

      addNoteEditText.setText(contents.toString());
      enableShareNoteMenuItem();

      //return contents.toString();

      /*catch (FileNotFoundException e) {
        e.printStackTrace();
        Log.d(TAG, "Note does not exist, not being displayed");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
        Log.d(TAG, "Unsupported encoding exception");
      }*/

    }

    //return null;
  }

  public boolean isExternalStorageWritable() {
    String state = Environment.getExternalStorageState();
    if(Environment.MEDIA_MOUNTED.equals(state)) {
      return true;
    }
    return false;
  }

  public boolean isExternalStorageReadable() {
    String state = Environment.getExternalStorageState();
    if(Environment.MEDIA_MOUNTED.equals(state)
        || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
      return true;
    }
    return false;
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

  /*@Override
  public void onDialogPositiveClick(DialogFragment dialog) {
    Dialog addNoteDialog = getDialog();
    addNoteDialog.dismiss();
  }

  @Override
  public void onDialogNegativeClick(DialogFragment dialog) {
    //No action
  }*/
}

class ConfirmationAlertDialogFragment extends DialogFragment {
  /**
   * Helper class to show the alert dialog in case the user tries to exit the
   * AddNoteDialog with unsaved file changes
   **/

  private Dialog addNoteDialog;

  public ConfirmationAlertDialogFragment(Dialog dialog) {
    super();
    addNoteDialog = dialog;
  }

  /*public interface ConfirmationAlertDialogListener {
    public void onDialogPositiveClick(DialogFragment dialog);
    public void onDialogNegativeClick(DialogFragment dialog);
  }

  ConfirmationAlertDialogListener listener;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    *//*try {
      listener = (ConfirmationAlertDialogListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(getActivity().toString() + " must implement ConfirmationAlertDialogListener");
    }*//*
  }*/

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {

    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());//, R.style.ConfirmationAlertDialogStyle);
    builder.setMessage("Discard unsaved changes?")
        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            //TODO: Return that user wants to dismiss dialog
            //listener.onDialogPositiveClick(ConfirmationAlertDialogFragment.this);
            //addNoteDialog = getDialog();
            addNoteDialog.dismiss();
          }
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            //No action to be taken in case of 'Cancel'
            //listener.onDialogNegativeClick(ConfirmationAlertDialogFragment.this);
          }
        });

    return builder.create();
  }

}
