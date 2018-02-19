package org.kiwix.kiwixmobile.modules.search;

import android.content.Context;

import org.kiwix.kiwixmobile.common.base.BasePresenter;
import org.kiwix.kiwixmobile.common.data.database.KiwixDatabase;
import org.kiwix.kiwixmobile.common.data.database.RecentSearchDao;
import org.kiwix.kiwixmobile.modules.search.contract.SearchViewCallback;

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
