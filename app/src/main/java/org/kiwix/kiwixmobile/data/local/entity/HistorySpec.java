package org.kiwix.kiwixmobile.data.local.entity;

import com.yahoo.squidb.annotations.TableModelSpec;

@TableModelSpec(className = "History", tableName = "History")
public class HistorySpec {
  public String zimFile;
  public String favicon;
  public String historyUrl;
  public String historyTitle;
  public long timeStamp;
}
