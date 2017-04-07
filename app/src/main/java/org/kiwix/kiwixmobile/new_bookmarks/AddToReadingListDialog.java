package org.kiwix.kiwixmobile.new_bookmarks;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.database.KiwixDatabase;
import org.kiwix.kiwixmobile.database.ReadingListFolderDao;
import org.kiwix.kiwixmobile.database.entity.ReadingListFolders;
import org.kiwix.kiwixmobile.new_bookmarks.entities.ReadinglistFolder;
import org.kiwix.kiwixmobile.new_bookmarks.lists.ReadingListItem;

import java.util.ArrayList;


public class AddToReadingListDialog extends ExtendedBottomSheetDialogFragment {


    private FastAdapter<ReadingListItem> fastAdapter;
    private ItemAdapter<ReadingListItem> itemAdapter;
    private View listsContainer;
    private CreateButtonClickListener createClickListener = new CreateButtonClickListener();
    @Nullable
    private DialogInterface.OnDismissListener dismissListener;
//    private ReadingListItemCallback listItemCallback = new ReadingListItemCallback();
    private String pageTitle;
    private ReadingListFolderDao readinglistDao;

    public static AddToReadingListDialog newInstance(@NonNull String title) {
        return newInstance(title, null);
    }

    public static AddToReadingListDialog newInstance(@NonNull String title,
                                                     @Nullable DialogInterface.OnDismissListener listener) {
        AddToReadingListDialog dialog = new AddToReadingListDialog();
        Bundle args = new Bundle();
        args.putString("title", title);
        dialog.setArguments(args);
        dialog.setOnDismissListener(listener);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageTitle = getArguments().getString("title");
//        adapter = new ReadingListAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        readinglistDao = new ReadingListFolderDao(KiwixDatabase.getInstance(getActivity()));
        View rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_add_to_readinglist, container);

        listsContainer = rootView.findViewById(R.id.lists_container);

        RecyclerView readingListView = (RecyclerView) rootView.findViewById(R.id.list_of_lists);fastAdapter = new FastAdapter<>();
        itemAdapter = new ItemAdapter<>();

        fastAdapter.withSelectOnLongClick(false);
        fastAdapter.withSelectable(false);
        readingListView.setLayoutManager(new LinearLayoutManager(getActivity()));

        readingListView.setAdapter(itemAdapter.wrap(fastAdapter));

        View createButton = rootView.findViewById(R.id.create_button);
        createButton.setOnClickListener(createClickListener);

        View closeButton = rootView.findViewById(R.id.close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        updateLists();
        return rootView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (dismissListener != null) {
            dismissListener.onDismiss(null);
        }
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        dismissListener = listener;
    }



    private void updateLists() {
        ArrayList<ReadinglistFolder> folders = readinglistDao.getFolders();
        for (ReadinglistFolder folder: folders) {
            itemAdapter.add(new ReadingListItem(folder.getFolderTitle()));
        }
    }

    private class CreateButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            showCreateListDialog();
        }
    }

    private void showCreateListDialog() {
        new MaterialDialog.Builder(getActivity())
            .title("Create a new reading list")
            .content("tada")
            .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
            .input("my reading list", null, new MaterialDialog.InputCallback() {
                @Override
                public void onInput(MaterialDialog dialog, CharSequence input) {
                    // TODO: save folder
                }
            }).show();

    }



}
