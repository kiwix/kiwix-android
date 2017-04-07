package org.kiwix.kiwixmobile.new_bookmarks;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import org.kiwix.kiwixmobile.new_bookmarks.entities.BookmarkArticle;
import org.kiwix.kiwixmobile.new_bookmarks.entities.ReadinglistFolder;
import org.kiwix.kiwixmobile.new_bookmarks.lists.ReadingListArticleItem;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class ReadingListFragment extends Fragment implements FastAdapter.OnClickListener<ReadingListArticleItem> {


    private FastAdapter<ReadingListArticleItem> fastAdapter;
    private ItemAdapter<ReadingListArticleItem> itemAdapter;
    private final String FRAGMENT_ARGS_FOLDER_TITLE = "requested_folder_title";
    private ReadingListFolderDao readinglistFoldersDao;
    private ArrayList<BookmarkArticle> articles;


    public ReadingListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_reading_list, container, false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView readingListView = (RecyclerView) view.findViewById(R.id.readinglist_articles_list);
        fastAdapter = new FastAdapter<>();
        itemAdapter = new ItemAdapter<>();

        fastAdapter.withOnClickListener(this);
        fastAdapter.withSelectOnLongClick(false);
        fastAdapter.withSelectable(false);
        readingListView.setLayoutManager(new LinearLayoutManager(getActivity()));

        readingListView.setAdapter(itemAdapter.wrap(fastAdapter));

        readinglistFoldersDao = new ReadingListFolderDao(KiwixDatabase.getInstance(getActivity()));

        loadArticlesOfFolder();



    }


    void loadArticlesOfFolder() {
        String requestedFolderTitle = this.getArguments().getString(FRAGMENT_ARGS_FOLDER_TITLE);
        articles = readinglistFoldersDao.getArticlesOfFolder(new ReadinglistFolder(requestedFolderTitle));
//        for (BookmarkArticle article: articles) {
//            itemAdapter.add(new ReadingListArticleItem(article.getBookmarkTitle()));
//        }
    }



    @Override
    public boolean onClick(View v, IAdapter<ReadingListArticleItem> adapter, ReadingListArticleItem item, int position) {
        //TODO: open main activity with clicked article
        return false;
    }
}
