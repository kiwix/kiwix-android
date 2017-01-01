package org.kiwix.kiwixmobile.downloader;

public class Chunk {

  private String rangeHeader;
  private String fileName;
  private String url;
  private long contentLength;
  private int notificationID;
  private long startByte;
  private long endByte;
  public boolean isDownloaded = false;

  public Chunk(String rangeHeader, String fileName, String url, long contentLength, int notificationID , long startByte, long endByte) {
    this.rangeHeader = rangeHeader;
    this.fileName = fileName;
    this.url = url;
    this.contentLength = contentLength;
    this.startByte = startByte;
    this.endByte = endByte;
    this.notificationID = notificationID;
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

  public int getNotificationID(){ return notificationID; }

  public long getStartByte(){ return startByte; }

  public long getEndByte(){ return endByte; }

  public long getSize() { return 1 + endByte - startByte; }
}
