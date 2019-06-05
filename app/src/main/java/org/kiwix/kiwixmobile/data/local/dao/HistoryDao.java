package org.kiwix.kiwixmobile.data.local.dao;

import android.content.Context;
import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.data.ZimContentProvider;
import org.kiwix.kiwixmobile.data.local.KiwixDatabase;
import org.kiwix.kiwixmobile.data.local.entity.History;

import static org.kiwix.kiwixmobile.utils.LanguageUtils.getCurrentLocale;

public class HistoryDao {
  private final Context context;
  private final KiwixDatabase kiwixDatabase;

  @Inject HistoryDao(Context context, KiwixDatabase kiwixDatabase) {
    this.context = context;
    this.kiwixDatabase = kiwixDatabase;
  }

  public void saveHistory(History history) {
    SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", getCurrentLocale(context));
    String date = sdf.format(new Date(history.getTimeStamp()));
    kiwixDatabase.deleteWhere(History.class, History.HISTORY_URL.eq(history.getHistoryUrl())
        .and(History.DATE.eq(date)).and(History.ZIM_ID.eq(history.getZimId())));
    kiwixDatabase.persist(new History()
        .setZimId(history.getZimId())
        .setZimName(history.getZimName())
        .setZimFilePath(history.getZimFilePath())
        .setFavicon(history.getFavicon())
        .setHistoryUrl(history.getHistoryUrl())
        .setHistoryTitle(history.getHistoryTitle())
        .setDate(date)
        .setTimeStamp(history.getTimeStamp()));
  }

  public List<History> getHistoryList(boolean showHistoryCurrentBook) {
    ArrayList<History> histories = new ArrayList<>();
    Query query = Query.select();
    if (showHistoryCurrentBook) {
      query = query.where(History.ZIM_FILE_PATH.eq(ZimContentProvider.getZimFile()));
    }
    try (SquidCursor<History> historySquidCursor = kiwixDatabase
        .query(History.class, query.orderBy(History.TIME_STAMP.desc()))) {
      while (historySquidCursor.moveToNext()) {
        History history = new History();
        history.setDate(historySquidCursor.get(History.DATE))
            .setFavicon(historySquidCursor.get(History.FAVICON))
            .setHistoryTitle(historySquidCursor.get(History.HISTORY_TITLE))
            .setHistoryUrl(historySquidCursor.get(History.HISTORY_URL))
            .setTimeStamp(historySquidCursor.get(History.TIME_STAMP))
            .setZimFilePath(historySquidCursor.get(History.ZIM_FILE_PATH))
            .setZimName(historySquidCursor.get(History.ZIM_NAME))
            .setZimId(historySquidCursor.get(History.ZIM_ID));
        histories.add(history);
      }
    }
    return histories;
  }

  public void deleteHistory(List<History> historyList) {
    for (History history : historyList) {
      if (history != null) {
        kiwixDatabase.deleteWhere(History.class, History.TIME_STAMP.eq(history.getTimeStamp()));
      }
    }
  }
}
