package org.kiwix.kiwixmobile.data.local.dao;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;

import org.kiwix.kiwixmobile.data.ZimContentProvider;
import org.kiwix.kiwixmobile.data.local.KiwixDatabase;
import org.kiwix.kiwixmobile.data.local.entity.History;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class HistoryDao {
  private KiwixDatabase kiwixDatabase;

  @Inject
  HistoryDao(KiwixDatabase kiwixDatabase) {
    this.kiwixDatabase = kiwixDatabase;
  }

  public void saveHistory(String file, String favicon, String url, String title, long timeStamp) {
    kiwixDatabase.persist(new History().setZimFile(file).setFavicon(favicon)
        .setHistoryUrl(url).setHistoryTitle(title).setTimeStamp(timeStamp));
  }

  public List<History> getHistoryList(boolean showHistoryCurrentBook) {
    ArrayList<History> histories = new ArrayList<>();
    Query query = Query.select();
    if (showHistoryCurrentBook) {
      query = query.where(History.ZIM_FILE.eq(ZimContentProvider.getZimFile()));
    }
    try (SquidCursor<History> historySquidCursor = kiwixDatabase
        .query(History.class, query.orderBy(History.TIME_STAMP.desc()))) {
      while (historySquidCursor.moveToNext()) {
        History history = new History();
        history.setFavicon(historySquidCursor.get(History.FAVICON));
        history.setHistoryTitle(historySquidCursor.get(History.HISTORY_TITLE));
        history.setHistoryUrl(historySquidCursor.get(History.HISTORY_URL));
        history.setTimeStamp(historySquidCursor.get(History.TIME_STAMP));
        history.setZimFile(historySquidCursor.get(History.ZIM_FILE));
        histories.add(history);
      }
    }
    return histories;
  }

  public void deleteHistory(List<History> historyList) {
    for (History history : historyList) {
      kiwixDatabase.deleteWhere(History.class, History.TIME_STAMP.eq(history.getTimeStamp()));
    }
  }
}
