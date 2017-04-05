package org.kiwix.kiwixmobile.BookmarksView;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.ZimContentProvider;
import org.kiwix.kiwixmobile.database.BookmarksDao;
import org.kiwix.kiwixmobile.database.KiwixDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by EladKeyshawn on 05/04/2017.
 */

public class BookmarksPresenter extends BasePresenter<BookmarksViewCallback> {


    private BookmarksDao bookmarksDao;
    private ArrayList<String> bookmarks;
    private ArrayList<String> bookmarkUrls;



    public void loadBookmarks(Context context) {
        bookmarksDao = new BookmarksDao(KiwixDatabase.getInstance(context));
        bookmarks = bookmarksDao.getBookmarkTitles(ZimContentProvider.getId(), ZimContentProvider.getName());
        bookmarkUrls = bookmarksDao.getBookmarks(ZimContentProvider.getId(), ZimContentProvider.getName());
        getMvpView().showBookmarks(bookmarks,bookmarkUrls);
    }


    public void deleteBookmark(String article) {
        bookmarksDao.deleteBookmark(article, ZimContentProvider.getId(), ZimContentProvider.getName());
    }



    @Override
    public void attachView(BookmarksViewCallback mvpView) {
        super.attachView(mvpView);
    }

    @Override
    public void detachView() {
        super.detachView();
    }





}
