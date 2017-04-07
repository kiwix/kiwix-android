package org.kiwix.kiwixmobile.bookmarksView;

import android.content.Context;

import org.kiwix.kiwixmobile.ZimContentProvider;
import org.kiwix.kiwixmobile.database.BookmarksDao;
import org.kiwix.kiwixmobile.database.KiwixDatabase;

import java.util.ArrayList;

import javax.inject.Inject;

/**
 * Created by EladKeyshawn on 05/04/2017.
 */

public class BookmarksPresenter extends BasePresenter<BookmarksViewCallback> {


  private BookmarksDao bookmarksDao;
  private ArrayList<String> bookmarks;
  private ArrayList<String> bookmarkUrls;

  @Inject
  public BookmarksPresenter() {
  }

  public void loadBookmarks(Context context) {
    bookmarksDao = new BookmarksDao(KiwixDatabase.getInstance(context));
    bookmarks = bookmarksDao.getBookmarkTitles(ZimContentProvider.getId(), ZimContentProvider.getName());
    bookmarkUrls = bookmarksDao.getBookmarks(ZimContentProvider.getId(), ZimContentProvider.getName());
    getMvpView().showBookmarks(bookmarks, bookmarkUrls);
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
