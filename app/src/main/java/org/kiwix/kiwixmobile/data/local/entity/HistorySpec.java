package org.kiwix.kiwixmobile.data.local.entity;

import com.yahoo.squidb.annotations.TableModelSpec;

@TableModelSpec(className = "History", tableName = "History")
class HistorySpec {
  public String zimId;
  public String zimName;
  public String zimFilePath;
  public String favicon;
  public String historyUrl;
  public String historyTitle;
  public String date;
  public long timeStamp;
}
