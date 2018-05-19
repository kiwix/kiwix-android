/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kiwix.kiwixmobile.modules.bookmarksview;

import org.kiwix.kiwixmobile.common.data.contentprovider.ZimContentProvider;
import org.kiwix.kiwixmobile.common.base.BasePresenter;
import org.kiwix.kiwixmobile.modules.bookmarksview.contract.BookmarksViewCallback;
import org.kiwix.kiwixmobile.common.data.database.BookmarksDao;

import java.util.ArrayList;

import javax.inject.Inject;

/**
 * Created by EladKeyshawn on 05/04/2017.
 */
public class BookmarksPresenter extends BasePresenter<BookmarksViewCallback> {

  @Inject
  BookmarksDao bookmarksDao;

  @Inject
  public BookmarksPresenter() {
  }

  public void loadBookmarks() {
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
