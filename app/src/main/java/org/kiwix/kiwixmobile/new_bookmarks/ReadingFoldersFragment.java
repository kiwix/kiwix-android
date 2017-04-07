package org.kiwix.kiwixmobile.new_bookmarks;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.database.KiwixDatabase;
import org.kiwix.kiwixmobile.database.ReadingListFolderDao;
import org.kiwix.kiwixmobile.new_bookmarks.entities.ReadinglistFolder;
import org.kiwix.kiwixmobile.new_bookmarks.lists.ReadingListItem;

import java.util.ArrayList;


public class ReadingFoldersFragment extends Fragment implements FastAdapter.OnClickListener<ReadingListItem> {

    private FastAdapter<ReadingListItem> fastAdapter;
    private ItemAdapter<ReadingListItem> itemAdapter;
    private ReadingListFolderDao readinglistFoldersDao;
    private ArrayList<ReadinglistFolder> folders;
    private final String FRAGMENT_ARGS_FOLDER_TITLE = "requested_folder_title";

    public ReadingFoldersFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reading_folders, container, false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        RecyclerView foldersList = (RecyclerView) view.findViewById(R.id.readinglist_folders_list);
        fastAdapter = new FastAdapter<>();
        itemAdapter = new ItemAdapter<>();

        fastAdapter.withOnClickListener(this);
        fastAdapter.withSelectOnLongClick(false);
        fastAdapter.withSelectable(false);
        foldersList.setLayoutManager(new LinearLayoutManager(getActivity()));

        foldersList.setAdapter(itemAdapter.wrap(fastAdapter));

        readinglistFoldersDao = new ReadingListFolderDao(KiwixDatabase.getInstance(getActivity()));

        refreshFolders();
    }


    void refreshFolders() {
        folders = readinglistFoldersDao.getFolders();
        for (ReadinglistFolder folder: folders) {
            itemAdapter.add(new ReadingListItem(folder.getFolderTitle()));
        }
    }

    @Override
    public boolean onClick(View v, IAdapter<ReadingListItem> adapter, ReadingListItem item, int position) {
        ReadingListFragment readingListFragment = new ReadingListFragment();
        Bundle args = new Bundle();
        args.putString(FRAGMENT_ARGS_FOLDER_TITLE,item.getTitle());
        readingListFragment.setArguments(args);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.readinglist_fragment_container, readingListFragment);
        transaction.addToBackStack(null);

        transaction.commit();
        return true;
    }
}
