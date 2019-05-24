package org.kiwix.kiwixmobile.main;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentTransaction;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.data.ZimContentProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AddNoteDialog extends DialogFragment {

  private TextView addNoteTextView;
  private EditText addNoteEditText;
  private String zimFileTitle;
  private String articleTitle;

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

    addNoteTextView = view.findViewById(R.id.add_note_text_view);
    addNoteTextView.setText(articleTitle);
    addNoteEditText = view.findViewById(R.id.add_note_edit_text);

    Toolbar toolbar = view.findViewById(R.id.add_note_toolbar);
    toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
    //toolbar.setNavigationOnClickListener();
    //toolbar.setOnMenuItemClickListener
    toolbar.setTitle("Note");
    toolbar.setNavigationOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Dialog dialog = getDialog();
        closeKeyboard();
        dialog.dismiss();
      }
    });
    toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {
          case R.id.share_note:
            Toast.makeText(getContext(), "Share Note", Toast.LENGTH_SHORT).show();
            //TODO: Add app-chooser to intent for sharing note text file
            /*String message = "Parent Directory - "+ ZimContentProvider.getZimFileTitle()
                              + "\nSub Directory - " + ((MainActivity)getActivity()).getCurrentWebView().getTitle();
            addNoteEditText.setText(message);*/

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

    return view;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    addNoteEditText.requestFocus();
    showKeyboard();
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
}
