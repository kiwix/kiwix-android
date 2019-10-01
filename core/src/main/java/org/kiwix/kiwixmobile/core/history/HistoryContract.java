package org.kiwix.kiwixmobile.core.history;

import java.util.List;
import org.kiwix.kiwixmobile.core.base.BaseContract;

interface HistoryContract {
  interface View extends BaseContract.View<Presenter> {
    void updateHistoryList(List<HistoryListItem> historyList);

    void notifyHistoryListFiltered(List<HistoryListItem> historyList);
  }

  interface Presenter extends BaseContract.Presenter<View> {
    void loadHistory(boolean showHistoryCurrentBook);

    void filterHistory(List<HistoryListItem> historyList, String newText);

    void deleteHistory(List<HistoryListItem> deleteList);
  }
}
