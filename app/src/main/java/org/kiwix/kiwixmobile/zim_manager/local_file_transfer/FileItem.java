package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

/**
 * Helper class, part of the local file sharing module.
 *
 * Defines a file-item to represent the files being transferred.
 * */
public class FileItem {
  public static final short TO_BE_SENT = -1;  // File yet to be sent
  public static final short SENDING = 0;      // Being sent
  public static final short SENT = +1;        // Successfully sent
  public static final short ERROR = +2;       // Error encountered while transferring the file

  private String fileName;
  private short fileStatus;

  public FileItem(String fileName, short fileStatus) {
    this.fileName = fileName;
    this.fileStatus = fileStatus;
  }

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
