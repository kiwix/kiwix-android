package org.kiwix.kiwixmobile.modules.bookmarks_view;

import android.content.Context;

import org.kiwix.kiwixmobile.common.data.content_provider.ZimContentProvider;
import org.kiwix.kiwixmobile.common.base.BasePresenter;
import org.kiwix.kiwixmobile.common.data.database.BookmarksDao;
import org.kiwix.kiwixmobile.common.data.database.KiwixDatabase;
import org.kiwix.kiwixmobile.modules.bookmarks_view.contract.BookmarksViewCallback;

import java.util.ArrayList;

import javax.inject.Inject;

/**
 * Created by EladKeyshawn on 05/04/2017.
 */
public class BookmarksPresenter extends BasePresenter<BookmarksViewCallback> {

  private BookmarksDao bookmarksDao;

  @Inject
  public BookmarksPresenter() {
  }

  public void loadBookmarks(Context context) {
    bookmarksDao = new BookmarksDao(KiwixDatabase.getInstance(context));
    ArrayList<String> bookmarks = bookmarksDao.getBookmarkTitles(ZimContentProvider.getId(), ZimContentProvider.getName());
    ArrayList<String> bookmarkUrls = bookmarksDao.getBookmarks(ZimContentProvider.getId(), ZimContentProvider.getName());
    getMvpView().showBookmarks(bookmarks, bookmarkUrls);
  }


  public void deleteBookmark(String article) {
    bookmarksDao.deleteBookmark(article, ZimContentProvider.getId(), ZimContentProvider.getName());
  }

  @Override
  public void attachView(BookmarksViewCallback mvpView) {
    super.attachView(mvpView);
  }

}
