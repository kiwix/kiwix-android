package org.kiwix.kiwixmobile.history;

import org.kiwix.kiwixmobile.base.BaseContract;
import org.kiwix.kiwixmobile.data.local.entity.History;

import java.util.List;

interface HistoryContract {
  interface View extends BaseContract.View<Presenter> {
    void updateHistoryList(List<History> historyList);

    void notifyHistoryListFiltered(List<History> historyList);
  }

  interface Presenter extends BaseContract.Presenter<View> {
    void loadHistory(boolean showHistoryCurrentBook);

    void filterHistory(List<History> historyList, String newText);

    void deleteHistory(List<History> deleteList);
  }
}
