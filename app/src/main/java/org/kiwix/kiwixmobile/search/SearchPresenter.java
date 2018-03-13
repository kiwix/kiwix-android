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
package org.kiwix.kiwixmobile.search;

import android.content.Context;

import org.kiwix.kiwixmobile.base.BasePresenter;
import org.kiwix.kiwixmobile.database.KiwixDatabase;
import org.kiwix.kiwixmobile.database.RecentSearchDao;

import javax.inject.Inject;

/**
 * Created by srv_twry on 14/2/18.
 */

public class SearchPresenter extends BasePresenter<SearchViewCallback> {

    @Inject
    public SearchPresenter() {}

    @Override
    public void attachView(SearchViewCallback searchViewCallback) {
        super.attachView(searchViewCallback);
    }

    void getRecentSearches(Context context) {
        RecentSearchDao recentSearchDao = new RecentSearchDao(KiwixDatabase.getInstance(context));
        getMvpView().addRecentSearches(recentSearchDao.getRecentSearches());
    }

    void saveSearch(String title, Context context) {
        RecentSearchDao recentSearchDao = new RecentSearchDao(KiwixDatabase.getInstance(context));
        recentSearchDao.saveSearch(title);
    }

    void deleteSearchString(String search, Context context) {
        RecentSearchDao recentSearchDao = new RecentSearchDao(KiwixDatabase.getInstance(context));
        recentSearchDao.deleteSearchString(search);
    }
}
