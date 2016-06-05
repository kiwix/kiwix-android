package org.kiwix.kiwixmobile.downloader;

public class Chunk {

  private String rangeHeader;
  private String fileName;
  private String url;
  private long contentLength;

  public Chunk(String rangeHeader, String fileName, String url, long contentLength) {
    this.rangeHeader = rangeHeader;
    this.fileName = fileName;
    this.url = url;
    this.contentLength = contentLength;
  }

  public String getRangeHeader() {
    return rangeHeader;
  }

  public String getFileName() {
    return fileName;
  }

  public long getContentLength() {
    return contentLength;
  }

  public String getUrl() {
    return url;
  }
}
