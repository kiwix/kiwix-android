package org.kiwix.kiwixmobile.library.entity;

import java.util.List;

public class MetaLinkNetworkEntity {
  private List<String> urls;

  public List<String> getUrls() {
    return urls;
  }

  public String getRelevantUrl() {
    return urls.get(0);
  }

  public void setUrls(List<String> urls) {
    this.urls = urls;
  }
}