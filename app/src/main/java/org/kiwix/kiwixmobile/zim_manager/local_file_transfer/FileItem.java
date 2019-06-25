package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

public class FileItem {
  public static short TO_BE_SENT = -1;
  public static short SENDING = 0;
  public static short SENT = +1;

  private String fileName = "";
  private short fileStatus = TO_BE_SENT;

  public FileItem(String fileName, short fileStatus) {
    this.fileName = fileName;
    this.fileStatus = fileStatus;
  }

  /*public void setFileName(String fileName) {
    this.fileName = fileName;
  }*/

  public void setFileStatus(short fileStatus) {
    this.fileStatus = fileStatus;
  }

  public String getFileName() {
    return fileName;
  }

  public short getFileStatus() {
    return fileStatus;
  }
}
