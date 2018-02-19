package org.kiwix.kiwixmobile.modules.search.contract;

import org.kiwix.kiwixmobile.common.base.contract.ViewCallback;

import java.util.List;

/**
 * Created by srv_twry on 14/2/18.
 */

public interface SearchViewCallback extends ViewCallback {
    void addRecentSearches(List<String> recentSearches);
}
