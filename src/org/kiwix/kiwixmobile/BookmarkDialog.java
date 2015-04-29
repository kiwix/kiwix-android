package org.kiwix.kiwixmobile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

@SuppressLint("ValidFragment")
public class BookmarkDialog extends DialogFragment {

    private BookmarkDialogListener listen;

    private String[] contents;

    private boolean isBookmarked;

    public BookmarkDialog(String[] contents, boolean isBookmarked) {
        this.contents = contents;
        this.isBookmarked = isBookmarked;
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder build = new AlertDialog.Builder(getActivity());
        //build.setTitle(R.string.menu_bookmarks);
        String buttonText;
        if (isBookmarked) {
            buttonText = getResources().getString(R.string.remove_bookmark);
        } else {
            buttonText = getResources().getString(R.string.add_bookmark);
        }

        if (contents.length != 0) {
            build.setItems(contents, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int choice) {
                    listen.onListItemSelect(contents[choice]);
                }
            });
        }

        build.setNeutralButton(buttonText, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int choice) {
                listen.onBookmarkButtonPressed();
            }
        });

        return build.create();
    }

    @Override
    public void onAttach(Activity a) {
        super.onAttach(a);
        try {
            listen = (BookmarkDialogListener) a;
        } catch (ClassCastException e) {
            throw new ClassCastException(a.toString()
                    + " must implement BookmarkDialogListener");
        }
    }

    public interface BookmarkDialogListener {

        public void onListItemSelect(String choice);

        public void onBookmarkButtonPressed();
    }
}
