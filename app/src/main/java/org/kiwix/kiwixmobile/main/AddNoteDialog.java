package org.kiwix.kiwixmobile.main;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentTransaction;

import org.kiwix.kiwixmobile.R;

public class AddNoteDialog extends DialogFragment {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setStyle(DialogFragment.STYLE_NORMAL, R.style.AddNoteDialogStyle);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    View view = inflater.inflate(R.layout.dialog_add_note, container, false);

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
            break;

          case R.id.save_note:
            Toast.makeText(getContext(), "Save Note", Toast.LENGTH_SHORT).show();
            //TODO: Add code for saving note text files
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

    EditText addNoteEditText = view.findViewById(R.id.add_note_edit_text);
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
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);

    switch (item.getItemId()) {
      case R.id.share_note:
        Toast.makeText(getContext(), "Share Note", Toast.LENGTH_SHORT).show();
        break;

      case R.id.save_note:
        Toast.makeText(getContext(), "Save Note", Toast.LENGTH_SHORT).show();
        break;
    }

    return true;
  }*/
}
