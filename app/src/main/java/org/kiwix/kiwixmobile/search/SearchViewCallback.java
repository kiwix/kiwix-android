package org.kiwix.kiwixmobile.search;

import org.kiwix.kiwixmobile.base.ViewCallback;

import java.util.List;

/**
 * Created by srv_twry on 14/2/18.
 */

public interface SearchViewCallback extends ViewCallback {
    void addRecentSearches(List<String> recentSearches);
}
